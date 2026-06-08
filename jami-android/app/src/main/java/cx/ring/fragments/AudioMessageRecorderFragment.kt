/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.fragments

import android.animation.LayoutTransition
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragAudioRecorderBinding
import cx.ring.utils.AndroidFileUtils
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Modal bottom sheet allowing the user to record an audio message and, before sending it:
 *  - extend the recording (append more audio),
 *  - restart it from scratch,
 *  - play it back and seek through it.
 * A live waveform is drawn while recording and used as a seek bar during playback.
 *
 * The host must have already been granted the [android.Manifest.permission.RECORD_AUDIO]
 * permission. When the user confirms, [onAudioReady] is invoked with the resulting file.
 */
class AudioMessageRecorderFragment : BottomSheetDialogFragment() {

    private enum class State { RECORDING, REVIEW, PLAYING }

    private var binding: FragAudioRecorderBinding? = null

    private var defaultIconTint: ColorStateList? = null
    private var defaultExtendBackgroundTint: ColorStateList? = null
    private var defaultExtendIconTint: ColorStateList? = null

    private var maxDurationMs: Long = DEFAULT_MAX_DURATION_MS
    private var maxFileSize: Long = Long.MAX_VALUE
    private var initialSegment: File? = null
    private var initialAmplitudes: FloatArray? = null
    private var continueRecording: Boolean = false

    private val segments = ArrayList<File>()
    private val amplitudes = ArrayList<Float>()

    private var recorder: MediaRecorder? = null
    private var currentSegment: File? = null

    private var player: MediaPlayer? = null
    private var combinedFile: File? = null
    private var sent = false

    private var state = State.RECORDING
    private var limitReached = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            maxDurationMs = args.getLong(ARG_MAX_DURATION, DEFAULT_MAX_DURATION_MS)
            maxFileSize = args.getLong(ARG_MAX_FILE_SIZE, Long.MAX_VALUE)
            initialSegment = args.getString(ARG_INITIAL_SEGMENT)?.let { File(it) }
            initialAmplitudes = args.getFloatArray(ARG_INITIAL_AMPLITUDES)
            continueRecording = args.getBoolean(ARG_CONTINUE, false)
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            val rec = recorder ?: return
            val amp = try { rec.maxAmplitude } catch (e: Exception) { 0 }
            val normalized = min(1f, sqrt(amp / 32767f))
            amplitudes.add(normalized)
            binding?.waveform?.addAmplitude(normalized)
            updateTimer()
            if (amplitudes.size.toLong() * POLL_INTERVAL_MS >= maxDurationMs) {
                limitReached = true
                finalizeSegment()
                enterReview()
                // binding?.status?.setText(R.string.audio_recording_limit_reached)
            } else {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = player ?: return
            val duration = mp.duration
            if (duration > 0) {
                binding?.waveform?.progress = mp.currentPosition.toFloat() / duration
            }
            updateTimer()
            handler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAudioRecorderBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        defaultIconTint = binding.primaryButton.iconTint
        defaultExtendBackgroundTint = binding.extendButton.backgroundTintList
        defaultExtendIconTint = binding.extendButton.iconTint

        binding.waveform.onSeek = { fraction -> seekTo(fraction) }
        binding.primaryButton.setOnClickListener { onPrimaryClicked() }
        binding.deleteButton.setOnClickListener { discardAndDismiss() }
        binding.restartButton.setOnClickListener { restart() }
        binding.extendButton.setOnClickListener { onExtendClicked() }
        binding.sendButton.setOnClickListener { confirmAndSend() }
        setupLayoutTransitions(binding)

        // Restore an in-progress recording across recreation (e.g. theme/orientation change).
        if (savedInstanceState != null) {
            savedInstanceState.getStringArray(STATE_SEGMENTS)?.forEach { path ->
                val f = File(path)
                if (f.exists() && f.length() > 0L) segments.add(f)
            }
            savedInstanceState.getFloatArray(STATE_AMPLITUDES)?.let { amplitudes.addAll(it.toList()) }
            limitReached = savedInstanceState.getBoolean(STATE_LIMIT, false)
            invalidateCombined()
            if (segments.isNotEmpty()) {
                enterReview()
                return
            }
        }

        val initial = initialSegment
        if (initial != null && initial.exists() && initial.length() > 0L) {
            // Opened with an already-recorded clip (e.g. from the inline recorder):
            // adopt it as the first segment.
            segments.add(initial)
            initialAmplitudes?.let { amplitudes.addAll(it.toList()) }
            invalidateCombined()
            if (continueRecording) {
                // The user slid to the review target while holding: keep recording seamlessly.
                binding.waveform.setAmplitudes(amplitudes)
                startSegment()
            } else {
                enterReview()
            }
        } else {
            startSegment()
        }
    }

    /**
     * Animates internal layout changes (e.g. the "continue recording" button appearing) without
     * letting the change propagate to the bottom sheet, which would make it jump on screen.
     */
    private fun setupLayoutTransitions(binding: FragAudioRecorderBinding) {
        binding.recorderBox.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setAnimateParentHierarchy(false)
        }
        (binding.root as ViewGroup).layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setAnimateParentHierarchy(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Finalize any active recording so its file is complete and survives recreation.
        if (state == State.RECORDING) finalizeSegment()
        outState.putStringArray(STATE_SEGMENTS, segments.map { it.absolutePath }.toTypedArray())
        outState.putFloatArray(STATE_AMPLITUDES, amplitudes.toFloatArray())
        outState.putBoolean(STATE_LIMIT, limitReached)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Intercept cancel (outside tap / back) so a non-trivial recording isn't lost by accident.
        val dialog = object : BottomSheetDialog(requireContext(), theme) {
            override fun cancel() {
                if (shouldConfirmDiscard()) confirmDiscard() else super.cancel()
            }
        }
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        dialog.setOnShowListener { applyBackgroundBlur(dialog) }
        return dialog
    }

    /**
     * On Android 12+ (when the device/window manager supports cross-window blur), turns the sheet
     * into a frosted-glass panel: the content behind the dialog is blurred and the sheet background
     * is made translucent so the blur shows through. Falls back to the default opaque sheet
     * otherwise.
     */
    private fun applyBackgroundBlur(dialog: BottomSheetDialog) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val window = dialog.window ?: return
        val wm = context?.getSystemService(WindowManager::class.java) ?: return
        if (!wm.isCrossWindowBlurEnabled) return

        val density = resources.displayMetrics.density
        // Blur the screen behind the dialog (the dimmed scrim area) and the content seen through
        // the translucent sheet itself.
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply {
            blurBehindRadius = (24f * density).toInt()
        }
        window.setBackgroundBlurRadius((48f * density).toInt())
        // Lighten the default scrim so the blur stays visible instead of being washed out by dim.
        window.setDimAmount(0.2f)

        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        val surface = MaterialColors.getColor(sheet, com.google.android.material.R.attr.colorSurface)
        val translucentSurface = ColorUtils.setAlphaComponent(surface, (0.80f * 255).toInt())
        val corner = 28f * density
        sheet.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(corner, corner, corner, corner, 0f, 0f, 0f, 0f)
            setColor(translucentSurface)
        }
        sheet.backgroundTintList = null
    }

    // region Recording

    private fun startSegment() {
        if (recorder != null) return
        val ctx = context ?: return
        val file = File(ctx.cacheDir, "audioseg_${System.nanoTime()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(SAMPLE_RATE)
                if (maxFileSize != Long.MAX_VALUE) setMaxFileSize(maxFileSize)
                setOutputFile(file.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        limitReached = true
                        finalizeSegment()
                        enterReview()
                        // binding?.status?.setText(R.string.audio_recording_limit_reached)
                    }
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startSegment: recorder start failed", e)
            rec.release()
            file.delete()
            Toast.makeText(activity, R.string.audio_recorder_error, Toast.LENGTH_SHORT).show()
            if (segments.isEmpty()) dismissAllowingStateLoss()
            return
        }
        recorder = rec
        currentSegment = file
        state = State.RECORDING
        binding?.waveform?.apply {
            fitMode = false
            // Hand the view our fixed poll cadence so the scroll is smooth from the first frame,
            // even when resuming (the busy main thread otherwise skews the inferred interval).
            setSampleInterval(POLL_INTERVAL_MS.toFloat())
        }
        // binding?.status?.setText(R.string.audio_recording_in_progress)
        updateUi()
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }

    /** Stops the active recorder and keeps the resulting segment if it is valid. */
    private fun finalizeSegment(): Boolean {
        val rec = recorder ?: return true
        recorder = null
        handler.removeCallbacks(pollRunnable)
        val ok = try {
            rec.stop()
            true
        } catch (e: Exception) {
            Log.w(TAG, "finalizeSegment: stop failed", e)
            false
        }
        rec.release()
        val seg = currentSegment
        currentSegment = null
        if (ok && seg != null && seg.exists() && seg.length() > 0L) {
            segments.add(seg)
            invalidateCombined()
            return true
        }
        seg?.delete()
        return segments.isNotEmpty()
    }

    private fun enterReview() {
        state = State.REVIEW
        binding?.waveform?.apply {
            fitMode = true
            setAmplitudes(amplitudes)
            progress = 0f
        }
        // binding?.status?.setText(R.string.audio_recording_ready)
        updateTimer()
        updateUi()
    }

    // endregion

    // region Controls

    private fun onPrimaryClicked() {
        when (state) {
            State.RECORDING -> {
                finalizeSegment()
                enterReview()
            }
            State.REVIEW -> startPlayback()
            State.PLAYING -> pausePlayback()
        }
    }

    /** Stops an active recording, otherwise extends (appends) a new segment. */
    private fun onExtendClicked() {
        if (state == State.RECORDING) {
            finalizeSegment()
            enterReview()
        } else {
            extend()
        }
    }

    private fun extend() {
        if (limitReached) return
        if (state == State.PLAYING) pausePlayback()
        releasePlayer()
        startSegment()
    }

    private fun restart() {
        releasePlayer()
        finalizeSegment()
        for (s in segments) s.delete()
        segments.clear()
        invalidateCombined()
        amplitudes.clear()
        limitReached = false
        binding?.waveform?.clear()
        startSegment()
    }

    private fun confirmAndSend() {
        if (state == State.RECORDING) finalizeSegment()
        releasePlayer()
        val file = buildCombined()
        if (file != null && file.exists() && file.length() > 0L) {
            sent = true
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_FILE_PATH to file.absolutePath))
            dismissAllowingStateLoss()
        } else {
            Toast.makeText(activity, R.string.audio_recorder_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun discardAndDismiss() {
        dismissAllowingStateLoss()
    }

    /** Total recorded length in ms, from the prepared player when available, else the live count. */
    private fun totalRecordedMs(): Long =
        player?.duration?.takeIf { it > 0 }?.toLong() ?: (amplitudes.size.toLong() * POLL_INTERVAL_MS)

    /** Whether tapping outside / pressing back should ask before throwing the recording away. */
    private fun shouldConfirmDiscard(): Boolean =
        !sent && totalRecordedMs() >= DISCARD_CONFIRM_THRESHOLD_MS

    private fun confirmDiscard() {
        // Pause capture/playback while the user decides, so nothing keeps running behind the dialog.
        when (state) {
            State.RECORDING -> {
                finalizeSegment()
                enterReview()
            }
            State.PLAYING -> pausePlayback()
            State.REVIEW -> {}
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.audio_recording_discard_title)
            .setMessage(R.string.audio_recording_discard_message)
            .setNegativeButton(R.string.audio_recording_discard_keep, null)
            .setPositiveButton(R.string.audio_recording_discard_confirm) { _, _ -> discardAndDismiss() }
            .show()
    }

    // endregion

    // region Playback

    private fun ensurePlayer(): MediaPlayer? {
        player?.let { return it }
        val file = buildCombined() ?: return null
        val mp = MediaPlayer()
        return try {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                handler.removeCallbacks(progressRunnable)
                binding?.waveform?.progress = 1f
                state = State.REVIEW
                updateTimer()
                updateUi()
            }
            player = mp
            mp
        } catch (e: Exception) {
            Log.e(TAG, "ensurePlayer: prepare failed", e)
            mp.release()
            Toast.makeText(activity, R.string.audio_recorder_error, Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun startPlayback() {
        val mp = ensurePlayer() ?: return
        if (mp.currentPosition >= mp.duration) mp.seekTo(0)
        mp.start()
        state = State.PLAYING
        // binding?.status?.setText(R.string.audio_recording_playing)
        handler.post(progressRunnable)
        updateUi()
    }

    private fun pausePlayback() {
        player?.pause()
        handler.removeCallbacks(progressRunnable)
        state = State.REVIEW
        // binding?.status?.setText(R.string.audio_recording_ready)
        updateUi()
    }

    private fun seekTo(fraction: Float) {
        val mp = ensurePlayer() ?: return
        val target = (fraction * mp.duration).toInt()
        mp.seekTo(target)
        // While paused, show the dragged position live instead of the full recorded duration.
        if (state != State.PLAYING) binding?.timer?.text = formatTime(target.toLong())
    }

    private fun releasePlayer() {
        handler.removeCallbacks(progressRunnable)
        player?.let {
            try { it.release() } catch (_: Exception) {}
        }
        player = null
    }

    // endregion

    // region Combining segments

    private fun invalidateCombined() {
        combinedFile?.let { if (!sent) it.delete() }
        combinedFile = null
    }

    private fun buildCombined(): File? {
        combinedFile?.let { if (it.exists()) return it }
        if (segments.isEmpty()) return null
        val ctx = context ?: return null
        val name = "audio_${dateFormat.format(Date())}.m4a"
        val out = File(AndroidFileUtils.getTempShareDir(ctx), name)
        return try {
            concatenate(segments, out)
            combinedFile = out
            out
        } catch (e: Exception) {
            Log.e(TAG, "buildCombined: concatenation failed", e)
            out.delete()
            // Fall back to the first segment so the user can still send something.
            segments.firstOrNull()?.takeIf { it.exists() && it.length() > 0L }
        }
    }

    /** Concatenates several AAC/MP4 audio files (same format) into a single MP4 file. */
    private fun concatenate(inputs: List<File>, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var outTrack = -1
        var ptsOffsetUs = 0L
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            for (input in inputs) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(input.absolutePath)
                    var trackIndex = -1
                    var format: MediaFormat? = null
                    for (i in 0 until extractor.trackCount) {
                        val f = extractor.getTrackFormat(i)
                        if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                            trackIndex = i
                            format = f
                            break
                        }
                    }
                    if (trackIndex < 0 || format == null) continue
                    extractor.selectTrack(trackIndex)

                    if (outTrack < 0) {
                        outTrack = muxer.addTrack(format)
                        muxer.start()
                    }

                    val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                        format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else DEFAULT_BUFFER_SIZE
                    val buffer = ByteBuffer.allocate(maxInput.coerceAtLeast(DEFAULT_BUFFER_SIZE))

                    var lastSampleTime = 0L
                    while (true) {
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        val sampleTime = extractor.sampleTime
                        bufferInfo.offset = 0
                        bufferInfo.size = size
                        bufferInfo.presentationTimeUs = ptsOffsetUs + sampleTime
                        bufferInfo.flags =
                            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer.writeSampleData(outTrack, buffer, bufferInfo)
                        lastSampleTime = sampleTime
                        extractor.advance()
                    }
                    // Advance the offset by the last sample time plus one frame of headroom.
                    ptsOffsetUs += lastSampleTime + AAC_FRAME_DURATION_US
                } finally {
                    extractor.release()
                }
            }
        } finally {
            if (outTrack >= 0) {
                try { muxer.stop() } catch (e: Exception) { Log.w(TAG, "concatenate: muxer stop failed", e) }
            }
            muxer.release()
        }
    }

    // endregion

    // region UI

    private fun updateUi() {
        val binding = binding ?: return
        val context = binding.root.context
        when (state) {
            State.RECORDING -> {
                // Hide the play/pause control while recording; stopping happens via the
                // red stop button (the former mic/extend button).
                binding.primaryButton.visibility = View.GONE

                binding.extendButton.visibility = View.VISIBLE
                binding.extendButton.icon = context.getDrawable(R.drawable.stop_24px)
                binding.extendButton.contentDescription = getString(R.string.audio_recording_stop)
                binding.extendButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_A100))
                binding.extendButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_500))
                binding.extendButton.isEnabled = true

                binding.sendButton.isEnabled = true
            }
            State.REVIEW -> {
                binding.primaryButton.visibility = View.VISIBLE
                binding.primaryButton.icon = context.getDrawable(R.drawable.play_arrow_24px)
                binding.primaryButton.contentDescription = getString(R.string.audio_recording_play)
                binding.primaryButton.iconTint = defaultIconTint

                binding.extendButton.visibility = if (limitReached) View.GONE else View.VISIBLE
                binding.extendButton.icon = context.getDrawable(R.drawable.mic_add_24px)
                binding.extendButton.contentDescription = getString(R.string.audio_recording_extend)
                binding.extendButton.backgroundTintList = defaultExtendBackgroundTint
                binding.extendButton.iconTint = defaultExtendIconTint
                binding.extendButton.isEnabled = true

                binding.sendButton.isEnabled = true
            }
            State.PLAYING -> {
                binding.primaryButton.visibility = View.VISIBLE
                binding.primaryButton.icon = context.getDrawable(R.drawable.pause_24px)
                binding.primaryButton.contentDescription = getString(R.string.audio_recording_pause)
                binding.primaryButton.iconTint = defaultIconTint

                binding.extendButton.visibility = if (limitReached) View.GONE else View.VISIBLE
                binding.extendButton.icon = context.getDrawable(R.drawable.mic_add_24px)
                binding.extendButton.contentDescription = getString(R.string.audio_recording_extend)
                binding.extendButton.backgroundTintList = defaultExtendBackgroundTint
                binding.extendButton.iconTint = defaultExtendIconTint
                binding.extendButton.isEnabled = true

                binding.sendButton.isEnabled = true
            }
        }
    }


    private fun updateTimer() {
        val binding = binding ?: return
        when (state) {
            State.RECORDING -> binding.timer.text = formatTime(amplitudes.size.toLong() * POLL_INTERVAL_MS)
            State.PLAYING -> binding.timer.text = formatTime(player?.currentPosition?.toLong() ?: 0L)
            // Idle/paused: show the full recorded duration rather than the play position (which is 0
            // right after pausing a recording).
            State.REVIEW -> binding.timer.text = formatTime(totalRecordedMs())
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    // endregion

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        // Stop and release the recorder, keeping the segment file (cleanup happens in onDestroy).
        finalizeSegment()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Preserve the recorded files across a configuration change so the recreated fragment can
        // restore them; only delete when the dialog is genuinely going away.
        if (activity?.isChangingConfigurations == true) return
        for (s in segments) s.delete()
        segments.clear()
        if (!sent) combinedFile?.delete()
    }

    companion object {
        private val TAG = AudioMessageRecorderFragment::class.simpleName
        const val DEFAULT_MAX_DURATION_MS = 300_000L // 5 minutes
        private const val POLL_INTERVAL_MS = 80L
        private const val PROGRESS_INTERVAL_MS = 50L
        private const val SAMPLE_RATE = 44100
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024
        private const val AAC_FRAME_DURATION_US = 1024L * 1_000_000L / SAMPLE_RATE
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

        /** Below this recorded length, discarding by tapping outside happens without confirmation. */
        private const val DISCARD_CONFIRM_THRESHOLD_MS = 3_000L

        /** Fragment result API: the host listens for the produced audio file path. */
        const val REQUEST_KEY = "audioRecorderResult"
        const val RESULT_FILE_PATH = "audioFilePath"

        private const val ARG_MAX_DURATION = "maxDurationMs"
        private const val ARG_MAX_FILE_SIZE = "maxFileSize"
        private const val ARG_INITIAL_SEGMENT = "initialSegment"
        private const val ARG_INITIAL_AMPLITUDES = "initialAmplitudes"
        private const val ARG_CONTINUE = "continueRecording"

        private const val STATE_SEGMENTS = "stateSegments"
        private const val STATE_AMPLITUDES = "stateAmplitudes"
        private const val STATE_LIMIT = "stateLimitReached"

        fun newInstance(
            maxDurationMs: Long = DEFAULT_MAX_DURATION_MS,
            maxFileSize: Long = Long.MAX_VALUE,
            initialSegment: File? = null,
            initialAmplitudes: FloatArray? = null,
            continueRecording: Boolean = false,
        ): AudioMessageRecorderFragment = AudioMessageRecorderFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_MAX_DURATION, maxDurationMs)
                putLong(ARG_MAX_FILE_SIZE, maxFileSize)
                putString(ARG_INITIAL_SEGMENT, initialSegment?.absolutePath)
                putFloatArray(ARG_INITIAL_AMPLITUDES, initialAmplitudes)
                putBoolean(ARG_CONTINUE, continueRecording)
            }
        }
    }
}
