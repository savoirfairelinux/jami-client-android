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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.R.color.red_300
import cx.ring.databinding.ViewAudioRecordOverlayBinding
import cx.ring.utils.AndroidFileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Drives the press-and-hold "voice message" gesture on the microphone button, in the spirit
 * of popular messengers:
 *
 *  - A simple tap opens the full audio recorder dialog ([Callbacks.onSimpleTap]).
 *  - Pressing and holding starts an inline recording with a live waveform overlay.
 *  - Releasing in place sends the clip immediately ([Callbacks.onSend]).
 *  - Sliding left into the trash target and releasing discards the clip.
 *  - Sliding up or right into the review target and releasing opens the full recorder dialog
 *    pre-loaded with the clip and keeps recording so the user can carry on ([Callbacks.onReview]).
 *
 * All the recording, overlay and gesture handling lives here to keep `ConversationFragment` lean.
 */
class InlineAudioRecorder(
    private val context: Context,
    private val overlay: ViewAudioRecordOverlayBinding,
    private val maxDurationMs: Long,
    private val maxFileSize: Long,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun hasAudioPermission(): Boolean

        /** A short tap: open the full recorder dialog. */
        fun onSimpleTap()

        /** Release in place: send the recorded clip directly. */
        fun onSend(file: File)

        /**
         * Release on the review target: open the full recorder with this clip loaded.
         * When [continueRecording] is true the dialog should keep recording from this clip
         * (the user slid to the review target manually rather than hitting a limit).
         */
        fun onReview(file: File, amplitudes: FloatArray, continueRecording: Boolean)
    }

    private enum class Zone { SEND, CANCEL, REVIEW }

    private val handler = Handler(Looper.getMainLooper())

    private var micButton: View? = null
    private var downX = 0f
    private var downY = 0f
    private var pendingNoPermission = false

    private var recording = false
    private var recorder: MediaRecorder? = null
    private var recordFile: File? = null
    private val amplitudes = ArrayList<Float>()
    private var zone = Zone.SEND

    /** True when recording stopped on its own (duration/size limit), not from a user gesture. */
    private var autoStopped = false

    private val cancelThresholdPx = dp(32f)
    private val reviewUpThresholdPx = dp(64f)
    private val reviewRightThresholdPx = dp(28f)

    private val startRunnable = Runnable { startRecording() }

    private val pollRunnable = object : Runnable {
        override fun run() {
            val rec = recorder ?: return
            val amp = try {
                rec.maxAmplitude
            } catch (e: Exception) {
                0
            }
            val normalized = min(1f, sqrt(amp / 32767f))
            amplitudes.add(normalized)
            overlay.waveform.addAmplitude(normalized)
            overlay.timer.text = formatTime(amplitudes.size.toLong() * POLL_INTERVAL_MS)
            if (amplitudes.size.toLong() * POLL_INTERVAL_MS >= maxDurationMs) {
                // Auto-stop on limit and let the user review the result.
                autoStopped = true
                zone = Zone.REVIEW
                finishGesture()
            } else {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach(micButton: View) {
        this.micButton = micButton
        micButton.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> onDown(v, event)
                MotionEvent.ACTION_MOVE -> onMove(event)
                MotionEvent.ACTION_UP -> {
                    onUp(v); true
                }

                MotionEvent.ACTION_CANCEL -> {
                    onCancel(); true
                }

                else -> false
            }
        }
    }

    /** Cancels any in-flight recording. Safe to call from the host's lifecycle callbacks. */
    fun release() {
        handler.removeCallbacks(startRunnable)
        if (recording) {
            zone = Zone.CANCEL
            finishGesture()
        }
    }

    private fun onDown(v: View, event: MotionEvent): Boolean {
        downX = event.rawX
        downY = event.rawY
        zone = Zone.SEND
        if (!callbacks.hasAudioPermission()) {
            // Without permission we can't record; the release will request it via onSimpleTap.
            pendingNoPermission = true
            return true
        }
        pendingNoPermission = false
        v.parent?.requestDisallowInterceptTouchEvent(true)
        handler.postDelayed(startRunnable, LONG_PRESS_MS)
        return true
    }

    private fun onMove(event: MotionEvent): Boolean {
        if (!recording) return true
        val dx = event.rawX - downX
        val dy = event.rawY - downY
        val newZone = when {
            dx <= -cancelThresholdPx -> Zone.CANCEL
            dy <= -reviewUpThresholdPx || dx >= reviewRightThresholdPx -> Zone.REVIEW
            else -> Zone.SEND
        }
        if (newZone != zone) {
            zone = newZone
            micButton?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            applyZoneFeedback()
        }
        return true
    }

    private fun onUp(v: View) {
        handler.removeCallbacks(startRunnable)
        v.parent?.requestDisallowInterceptTouchEvent(false)
        if (pendingNoPermission) {
            pendingNoPermission = false
            callbacks.onSimpleTap()
            return
        }
        if (recording) {
            finishGesture()
        } else {
            // Released before the long-press fired: treat as a simple tap.
            callbacks.onSimpleTap()
        }
    }

    private fun onCancel() {
        handler.removeCallbacks(startRunnable)
        micButton?.parent?.requestDisallowInterceptTouchEvent(false)
        pendingNoPermission = false
        if (recording) {
            zone = Zone.CANCEL
            finishGesture()
        }
    }

    private fun startRecording() {
        val file = File(
            AndroidFileUtils.getTempShareDir(context),
            "audio_${dateFormat.format(Date())}.m4a"
        )
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
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
                        autoStopped = true
                        zone = Zone.REVIEW
                        finishGesture()
                    }
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startRecording: recorder start failed", e)
            rec.release()
            file.delete()
            Toast.makeText(context, R.string.audio_recorder_error, Toast.LENGTH_SHORT).show()
            return
        }
        recorder = rec
        recordFile = file
        recording = true
        autoStopped = false
        amplitudes.clear()
        showOverlay()
        micButton?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }

    /** Stops the recorder and dispatches the result according to the current [zone]. */
    private fun finishGesture() {
        if (!recording) return
        recording = false
        handler.removeCallbacks(pollRunnable)
        val rec = recorder
        recorder = null
        val durationMs = amplitudes.size.toLong() * POLL_INTERVAL_MS
        var ok = true
        if (rec != null) {
            try {
                rec.stop()
            } catch (e: Exception) {
                Log.w(TAG, "finishGesture: stop failed", e)
                ok = false
            }
            rec.release()
        }
        hideOverlay()
        val file = recordFile
        recordFile = null
        val currentZone = zone
        zone = Zone.SEND

        if (file == null) return
        val valid = ok && file.exists() && file.length() > 0L
        val wasAutoStopped = autoStopped
        autoStopped = false
        when (currentZone) {
            Zone.CANCEL -> file.delete()
            Zone.REVIEW -> if (valid) {
                callbacks.onReview(
                    file,
                    amplitudes.toFloatArray(),
                    continueRecording = !wasAutoStopped
                )
            } else file.delete()

            Zone.SEND -> {
                if (valid && durationMs >= MIN_RECORD_MS) {
                    callbacks.onSend(file)
                } else {
                    file.delete()
                    if (valid) Toast.makeText(
                        context,
                        R.string.audio_recording_too_short,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // region Overlay UI

    private fun showOverlay() {
        overlay.waveform.fitMode = false
        overlay.waveform.clear()
        overlay.waveform.setSampleInterval(POLL_INTERVAL_MS.toFloat())
        overlay.timer.text = formatTime(0)
        zone = Zone.SEND
        applyZoneFeedback()
        startDotBlink()
        overlay.audioOverlayRoot.apply {
            alpha = 0f
            translationY = dp(12f)
            visibility = View.VISIBLE
            animate().alpha(1f).translationY(0f).setDuration(150).start()
        }
    }

    private fun hideOverlay() {
        stopDotBlink()
        overlay.audioOverlayRoot.animate()
            .alpha(0f)
            .translationY(dp(12f))
            .setDuration(120)
            .withEndAction { overlay.audioOverlayRoot.visibility = View.GONE }
            .start()
    }

    private fun applyZoneFeedback() {
        val highlight = resolveColor(com.google.android.material.R.attr.colorPrimaryContainer)
        val neutral = resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHighest)
        val cancelActive = ContextCompat.getColor(context, R.color.red_500)

        val cancelOn = zone == Zone.CANCEL
        val reviewOn = zone == Zone.REVIEW

        overlay.cancelTarget.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (cancelOn) cancelActive else neutral)
        overlay.cancelTarget.scaleX = if (cancelOn) 1.2f else 1f
        overlay.cancelTarget.scaleY = if (cancelOn) 1.2f else 1f
        overlay.cancelTarget.setIconTintResource(if (cancelOn) android.R.color.white else red_300)

        overlay.expandCircle.scaleX = if (reviewOn) 1.2f else 1f
        overlay.expandCircle.scaleY = if (reviewOn) 1.2f else 1f
        overlay.expandCircle.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (reviewOn) highlight else neutral)
        overlay.expandCircle.iconTint = android.content.res.ColorStateList.valueOf(
            if (reviewOn) neutral else highlight
        )

        overlay.hintText.setText(
            when (zone) {
                Zone.CANCEL -> R.string.audio_recording_release_cancel
                Zone.REVIEW -> R.string.audio_recording_release_review
                Zone.SEND -> R.string.audio_recording_gesture_hint
            }
        )
        overlay.hintText.setTextColor(
            if (cancelOn) cancelActive
            else resolveColor(android.R.attr.textColorSecondary)
        )
    }

    private fun startDotBlink() {
        overlay.recDot.animate()
            .alpha(0.2f)
            .setDuration(550)
            .withEndAction(object : Runnable {
                override fun run() {
                    if (!recording) return
                    val target = if (overlay.recDot.alpha < 0.6f) 1f else 0.2f
                    overlay.recDot.animate().alpha(target).setDuration(550).withEndAction(this)
                        .start()
                }
            })
            .start()
    }

    private fun stopDotBlink() {
        overlay.recDot.animate().cancel()
        overlay.recDot.alpha = 1f
    }

    // endregion

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )

    @ColorInt
    private fun resolveColor(@AttrRes attr: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else 0xFF888888.toInt()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    companion object {
        private val TAG = InlineAudioRecorder::class.simpleName
        private const val LONG_PRESS_MS = 250L
        private const val POLL_INTERVAL_MS = 80L
        private const val MIN_RECORD_MS = 800L
        private const val SAMPLE_RATE = 44100
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
