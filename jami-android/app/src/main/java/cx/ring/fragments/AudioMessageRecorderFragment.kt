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

import android.app.Dialog
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
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
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
class AudioMessageRecorderFragment(
    private val maxDurationMs: Long = DEFAULT_MAX_DURATION_MS,
    private val maxFileSize: Long = Long.MAX_VALUE,
    private val initialSegment: File? = null,
    private val initialAmplitudes: FloatArray? = null,
    private val continueRecording: Boolean = false,
    private val onAudioReady: ((File) -> Unit)? = null,
) : BottomSheetDialogFragment() {

    private enum class State { RECORDING, REVIEW, PLAYING }

    private var binding: FragAudioRecorderBinding? = null

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
                binding?.status?.setText(R.string.audio_recording_limit_reached)
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

        binding.waveform.onSeek = { fraction -> seekTo(fraction) }
        binding.primaryButton.setOnClickListener { onPrimaryClicked() }
        binding.deleteButton.setOnClickListener { discardAndDismiss() }
        binding.restartButton.setOnClickListener { restart() }
        binding.extendButton.setOnClickListener { extend() }
        binding.sendButton.setOnClickListener { confirmAndSend() }

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
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
                        binding?.status?.setText(R.string.audio_recording_limit_reached)
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
        binding?.waveform?.fitMode = false
        binding?.status?.setText(R.string.audio_recording_in_progress)
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
        binding?.status?.setText(R.string.audio_recording_ready)
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
            onAudioReady?.invoke(file)
            dismissAllowingStateLoss()
        } else {
            Toast.makeText(activity, R.string.audio_recorder_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun discardAndDismiss() {
        dismissAllowingStateLoss()
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
        binding?.status?.setText(R.string.audio_recording_playing)
        handler.post(progressRunnable)
        updateUi()
    }

    private fun pausePlayback() {
        player?.pause()
        handler.removeCallbacks(progressRunnable)
        state = State.REVIEW
        binding?.status?.setText(R.string.audio_recording_ready)
        updateUi()
    }

    private fun seekTo(fraction: Float) {
        val mp = ensurePlayer() ?: return
        val target = (fraction * mp.duration).toInt()
        mp.seekTo(target)
        if (state != State.PLAYING) updateTimer()
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
                binding.primaryButton.icon = context.getDrawable(R.drawable.pause_24px)
                binding.primaryButton.contentDescription = getString(R.string.audio_recording_stop)
                binding.extendButton.visibility = View.GONE
                binding.sendButton.isEnabled = true
            }
            State.REVIEW -> {
                binding.primaryButton.icon = context.getDrawable(R.drawable.play_arrow_24px)
                binding.primaryButton.contentDescription = getString(R.string.audio_recording_play)
                binding.extendButton.visibility = if (limitReached) View.GONE else View.VISIBLE
                binding.sendButton.isEnabled = true
            }
            State.PLAYING -> {
                binding.primaryButton.icon = context.getDrawable(R.drawable.pause_24px)
                binding.primaryButton.contentDescription = getString(R.string.audio_recording_pause)
                binding.extendButton.visibility = if (limitReached) View.GONE else View.VISIBLE
                binding.sendButton.isEnabled = true
            }
        }
    }

    private fun updateTimer() {
        val binding = binding ?: return
        when (state) {
            State.RECORDING -> binding.timer.text = formatTime(amplitudes.size.toLong() * POLL_INTERVAL_MS)
            State.PLAYING, State.REVIEW -> {
                val mp = player
                val total = mp?.duration?.toLong() ?: (amplitudes.size.toLong() * POLL_INTERVAL_MS)
                val pos = mp?.currentPosition?.toLong() ?: 0L
                binding.timer.text = formatTime(pos)//"${formatTime(pos)} / ${formatTime(total)}"
            }
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
        finalizeSegment()
        for (s in segments) s.delete()
        segments.clear()
        if (!sent) combinedFile?.delete()
        binding = null
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
    }
}
