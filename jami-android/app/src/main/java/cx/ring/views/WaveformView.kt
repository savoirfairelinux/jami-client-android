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
package cx.ring.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders an audio waveform from a list of normalized amplitudes (each in 0..1).
 *
 * Two display modes are supported:
 *  - Recording (fitMode = false): bars are drawn with a fixed width, anchored to the
 *    right edge, the newest amplitude on the right. Older bars scroll off the left.
 *  - Review (fitMode = true): the full set of amplitudes is scaled to fit the view
 *    width. A [progress] fraction colors the already-played portion and the view
 *    becomes interactive for seeking (see [onSeek]).
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val amplitudes = ArrayList<Float>()

    /** When true, the full waveform is scaled to fit the width (review/playback). */
    var fitMode: Boolean = false
        set(value) {
            field = value
            if (value) {
                stopAnimation()
                // The amplitude set may have changed while recording (when the cache is bypassed),
                // so drop it to force a rebuild on the next review draw.
                fitBars = null
            }
            invalidate()
        }

    /** Playback progress in 0..1 used to color the played part of the waveform. */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** Invoked while the user drags on the waveform (review mode only) with a 0..1 fraction. */
    var onSeek: ((Float) -> Unit)? = null

    /** Pop/fade shape of a freshly added bar (overshoots its target, then settles). */
    private val popInterpolator = OvershootInterpolator(2.2f)

    /** Real-time animation driver so the slide and scale stay smooth regardless of frame rate. */
    private var animating = false
    private var lastFrameTimeNs = 0L
    private var lastSampleTimeNs = 0L
    private var sampleIntervalMs = APPEAR_DURATION_MS
    private var sampleIntervalSeeded = false
    private val frameCallback = Choreographer.FrameCallback { onAnimationFrame(it) }

    /** Running loudness peak (with slow decay) used to auto-scale the live recording waveform. */
    private var recordingPeak = 0f
    private var recordingScale = 1f
    private var targetScale = 1f

    /** Last bar index the finger was over while seeking, used to emit one haptic tick per bar. */
    private var lastSeekBar = -1

    /**
     * Cached downsampled bars for [fitMode] review rendering, together with the view width and
     * the vertical auto-scale they were computed for. The amplitude set doesn't change while a
     * clip is being reviewed, so the (potentially large) aggregation is done once here instead of
     * on every [onDraw]; playback progress updates and seeking then only re-color the same bars,
     * keeping each frame O(visible bars) regardless of how many amplitudes were recorded.
     */
    private var fitBars: FloatArray? = null
    private var fitBarsWidth = 0
    private var fitScale = 1f

    private val barWidth = dp(3f)
    private val barGap = dp(2f)
    private val cornerRadius = dp(1.5f)
    private val minBarHeight = dp(2f)
    /** Distance over which recording bars fade out as they reach the left edge (a long trail). */
    private val fadeTailPx = dp(FADE_TAIL_DP)
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF1565C0.toInt())
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = (resolveThemeColor(com.google.android.material.R.attr.colorOnSurface, 0xFF000000.toInt()) and 0x00FFFFFF) or 0x40000000
    }

    /** Color used for the live waveform while recording (defaults to red). */
    var recordingColor: Int = ContextCompat.getColor(context, cx.ring.R.color.red_500)
        set(value) {
            field = value
            recordingPaint.color = value
            invalidate()
        }

    /** Paint used to draw the live (non-fit) recording bars, kept separate so it stays red
     * regardless of the review/playback [setColors]. */
    private val recordingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = recordingColor
    }

    /**
     * Overrides the bar colors so the waveform can match its surrounding bubble.
     * [active] colors the already-played portion, [inactive] the rest.
     */
    fun setColors(active: Int, inactive: Int) {
        activePaint.color = active
        inactivePaint.color = inactive
        invalidate()
    }

    /** Replace the whole amplitude set (used when entering review mode). */
    fun setAmplitudes(values: List<Float>) {
        amplitudes.clear()
        amplitudes.addAll(values)
        fitBars = null
        invalidate()
    }

    /**
     * Replace the whole amplitude set from a [FloatArray] (used when entering review mode with
     * pre-extracted data). Avoids boxing every sample into an intermediate `List<Float>`.
     */
    fun setAmplitudes(values: FloatArray) {
        amplitudes.clear()
        amplitudes.ensureCapacity(values.size)
        for (v in values) amplitudes.add(v)
        fitBars = null
        invalidate()
    }

    /**
     * Tells the view the expected time between [addAmplitude] calls (ms). The recorder already
     * knows its own cadence, so supplying it makes the horizontal slide correct from the very
     * first frame instead of inferring it over the first ~second. That inference stutters when
     * the main thread is busy (most visibly when resuming a recording, where layout work delays
     * the first samples) and then snaps smooth once it converges; passing the real value removes
     * that. The estimate still adapts to genuine jitter afterwards.
     */
    fun setSampleInterval(ms: Float) {
        if (ms > 0f) {
            sampleIntervalMs = ms
            sampleIntervalSeeded = true
        }
    }

    /** Append a single amplitude sample (used live while recording). */
    fun addAmplitude(value: Float) {
        val v = value.coerceIn(0f, 1f)
        amplitudes.add(v)
        if (fitMode) {
            fitBars = null
            invalidate()
            return
        }
        // Track a slowly-decaying loudness peak and derive a target scale so the waveform fills
        // the available height around the loudest part, while leaving genuine silence flat.
        recordingPeak = max(v, recordingPeak * PEAK_DECAY)
        targetScale = if (recordingPeak < SILENCE_FLOOR) 1f
            else min(MAX_GAIN, NORM_TARGET / recordingPeak)
        // Measure the real interval between samples so the slide consumes exactly one slot
        // between them, keeping the scroll continuous no matter the sample or frame rate.
        val now = System.nanoTime()
        if (lastSampleTimeNs != 0L) {
            val intervalMs = (now - lastSampleTimeNs) / 1_000_000f
            // Snap to the first real measurement instead of easing down from the placeholder
            // default: otherwise the slide is normalized by a too-large interval for the first
            // ~10 samples and the scroll stutters for about a second before the estimate converges.
            sampleIntervalMs = if (sampleIntervalSeeded)
                sampleIntervalMs * (1f - INTERVAL_EASE) + intervalMs * INTERVAL_EASE
            else intervalMs
            sampleIntervalSeeded = true
        }
        lastSampleTimeNs = now
        startAnimation()
    }

    fun clear() {
        stopAnimation()
        lastSampleTimeNs = 0L
        sampleIntervalMs = APPEAR_DURATION_MS
        sampleIntervalSeeded = false
        recordingPeak = 0f
        recordingScale = 1f
        targetScale = 1f
        amplitudes.clear()
        fitBars = null
        progress = 0f
        invalidate()
    }

    fun amplitudeCount(): Int = amplitudes.size

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f
        val maxHalf = (h / 2f) - dp(1f)

        if (fitMode) {
            // Aggregate the amplitudes into at most one bar per drawable slot so the waveform
            // shows clean, evenly spaced bars instead of one (often sub-pixel) bar per sample.
            // The aggregation (and the auto-scale derived from it) is cached and only rebuilt when
            // the data or width changes, so playback progress updates and seeking don't re-process
            // the whole (potentially large) amplitude set on every frame.
            ensureFitBars(width)
            val bars = fitBars ?: return
            val count = bars.size
            if (count == 0) return
            val step = w / count
            val progressX = w * progress
            val scale = fitScale
            for (i in 0 until count) {
                val cx = i * step + step / 2f
                val half = max(minBarHeight, min(maxHalf, bars[i] * scale * maxHalf))
                val paint = if (cx <= progressX) activePaint else inactivePaint
                canvas.drawRoundRect(
                    cx - barWidth / 2f, centerY - half,
                    cx + barWidth / 2f, centerY + half,
                    cornerRadius, cornerRadius, paint
                )
            }
        } else {
            // Recording: fixed-width bars anchored to the right, newest on the right.
            val slot = barWidth + barGap
            // Time since the newest sample arrived (same clock as lastSampleTimeNs). Driving the
            // animation straight from elapsed time keeps it framerate-independent and lets several
            // bars be mid-pop at once: each older bar is one sample interval "older" than the one
            // to its right, so when APPEAR_DURATION_MS spans multiple intervals the pops overlap.
            val sinceSampleMs = if (lastSampleTimeNs == 0L) sampleIntervalMs
                else (System.nanoTime() - lastSampleTimeNs) / 1_000_000f
            // Slide the whole row left by the unfinished fraction of a slot so the bars scroll
            // smoothly. Normalizing by the measured sample interval consumes exactly one slot
            // between samples, so the scroll stays continuous regardless of sample or frame rate.
            // Until that interval is measured, park the newest bar at the right edge (it still
            // pops in) rather than sliding by a fraction derived from the placeholder interval,
            // which would otherwise jump the moment the real interval is seeded.
            val slide = if (!sampleIntervalSeeded || sampleIntervalMs <= 0f) 1f
                else (sinceSampleMs / sampleIntervalMs).coerceIn(0f, 1f)
            val shift = (1f - slide) * slot
            val visible = min(amplitudes.size, (w / slot).toInt() + 2)
            val start = amplitudes.size - visible
            val newestIndex = amplitudes.size - 1
            var x = w - barWidth + shift
            val baseAlpha = recordingPaint.alpha
            for (i in newestIndex downTo start) {
                val target = (minBarHeight).coerceAtLeast(
                    min(maxHalf, amplitudes[i] * recordingScale * maxHalf)
                )
                // Estimate this bar's age from its distance to the newest bar so the pop follows
                // it as it scrolls left, giving a smooth staggered cascade instead of a single pop.
                val ageMs = sinceSampleMs + (newestIndex - i) * sampleIntervalMs
                val pop = (ageMs / APPEAR_DURATION_MS).coerceIn(0f, 1f)
                // The bar pops up past its target then settles, fading in as it pops.
                val half: Float
                val popFade: Float
                if (pop < 1f) {
                    val popT = popInterpolator.getInterpolation(pop)
                    half = (minBarHeight + (target - minBarHeight) * popT)
                        .coerceIn(minBarHeight, maxHalf)
                    popFade = min(1f, pop * 1.4f)
                } else {
                    half = target
                    popFade = 1f
                }
                // Long fade-out tail on the left: a bar dims as its left edge approaches the view's
                // left edge over a fixed distance (fadeTailPx, in dp), independent of scroll speed,
                // so old bars trail off smoothly instead of clipping abruptly at the edge.
                val edgeFade = (x / fadeTailPx).coerceIn(0f, 1f)
                recordingPaint.alpha = (baseAlpha * popFade * edgeFade).toInt()
                canvas.drawRoundRect(
                    x, centerY - half,
                    x + barWidth, centerY + half,
                    cornerRadius, cornerRadius, recordingPaint
                )
                x -= slot
                if (x + barWidth < 0) break
            }
            recordingPaint.alpha = baseAlpha
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // The fit-mode bar count depends on width; drop the cache so it rebuilds for the new size.
        if (w != oldw) fitBars = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Resume the recording animation loop if we were recording when detached.
        if (!fitMode && amplitudes.isNotEmpty()) startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    private fun startAnimation() {
        if (animating || fitMode) return
        animating = true
        lastFrameTimeNs = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopAnimation() {
        if (!animating) return
        animating = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    /**
     * Per-frame update driven by [Choreographer]. Uses real elapsed time so the vertical scale
     * easing is framerate-independent: changing the frame rate only changes smoothness, not speed.
     * The slide and per-bar pop are derived from elapsed time directly in [onDraw].
     */
    private fun onAnimationFrame(frameTimeNanos: Long) {
        if (!animating) return
        val dt = if (lastFrameTimeNs == 0L) 0f
            else (frameTimeNanos - lastFrameTimeNs) / 1_000_000_000f
        lastFrameTimeNs = frameTimeNanos

        // Framerate-independent exponential easing toward the target vertical scale.
        if (dt > 0f) {
            recordingScale += (targetScale - recordingScale) * (1f - exp(-dt / SCALE_TIME_CONSTANT))
        }

        invalidate()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!fitMode || onSeek == null) return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val fraction = (event.x / width).coerceIn(0f, 1f)
                progress = fraction
                onSeek?.invoke(fraction)
                lastSeekBar = seekBarIndex(fraction)
                performHapticFeedback(hapticStart)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val fraction = (event.x / width).coerceIn(0f, 1f)
                progress = fraction
                onSeek?.invoke(fraction)
                // Emit one crisp haptic tick each time the finger crosses to a new bar.
                val bar = seekBarIndex(fraction)
                if (bar != lastSeekBar) {
                    lastSeekBar = bar
                    performHapticFeedback(hapticTick)
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val fraction = (event.x / width).coerceIn(0f, 1f)
                progress = fraction
                onSeek?.invoke(fraction)
                performHapticFeedback(hapticEnd)
                lastSeekBar = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Index of the displayed bar under [fraction], used to space out seek haptics. */
    private fun seekBarIndex(fraction: Float): Int {
        val slot = barWidth + barGap
        val bars = max(1, (width / slot).toInt())
        return (fraction * bars).toInt().coerceIn(0, bars - 1)
    }

    private val hapticStart: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_START
        else HapticFeedbackConstants.VIRTUAL_KEY

    private val hapticEnd: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END
        else HapticFeedbackConstants.CONTEXT_CLICK

    private val hapticTick: Int = when {
        // SEGMENT_FREQUENT_TICK is tuned for subtle, repeated ticks during a drag (much softer
        // than CLOCK_TICK/SEGMENT_TICK); TEXT_HANDLE_MOVE is the gentlest option on older devices.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    /**
     * Builds (or rebuilds) the cached downsampled bars and vertical auto-scale for [fitMode], but
     * only when the data was invalidated ([fitBars] is null) or the width changed. This lets the
     * per-frame draws (playback progress, seeking) reuse the aggregation instead of recomputing it.
     */
    private fun ensureFitBars(viewWidth: Int) {
        if (fitBars != null && fitBarsWidth == viewWidth) return
        if (amplitudes.isEmpty() || viewWidth <= 0) {
            fitBars = FloatArray(0)
            fitBarsWidth = viewWidth
            fitScale = 1f
            return
        }
        val slot = barWidth + barGap
        val maxBars = max(1, (viewWidth / slot).toInt())
        val bars = resampleToArray(amplitudes, min(maxBars, amplitudes.size))
        // Auto-scale to the loudest bar so quiet clips fill the height, while genuine silence
        // (peak below the floor) stays flat. Mirrors the live recording behaviour.
        var peak = 0f
        for (b in bars) if (b > peak) peak = b
        fitScale = if (peak < SILENCE_FLOOR) 1f else min(MAX_GAIN, NORM_TARGET / peak)
        fitBars = bars
        fitBarsWidth = viewWidth
    }

    /** Aggregates [src] into [target] buckets (averaging) so dense waveforms render as clean bars. */
    private fun resampleToArray(src: List<Float>, target: Int): FloatArray {
        val n = src.size
        if (target <= 0) return FloatArray(0)
        if (target >= n) {
            val out = FloatArray(n)
            for (i in 0 until n) out[i] = src[i]
            return out
        }
        val out = FloatArray(target)
        for (i in 0 until target) {
            val start = i * n / target
            val end = max(start + 1, (i + 1) * n / target)
            var sum = 0f
            for (j in start until end) sum += src[j]
            out[i] = sum / (end - start)
        }
        return out
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.resourceId != 0) ContextCompat.getColor(context, typedValue.resourceId)
            else typedValue.data
        } else fallback
    }

    @Suppress("unused")
    private fun Float.toPx() = this.roundToInt()

    private companion object {
        const val APPEAR_DURATION_MS = 500f    // pop/fade duration of a newly added bar (ms)
        const val FADE_TAIL_DP = 72f           // left-edge fade-out trail length for recording bars
        const val PEAK_DECAY = 0.992f          // how slowly the loudness peak fades
        const val SILENCE_FLOOR = 0.05f        // below this peak, keep bars flat (silence stays silence)
        const val NORM_TARGET = 0.9f           // map the loudest part to ~90% of the height
        const val MAX_GAIN = 5f                // cap amplification so quiet noise isn't blown up
        const val SCALE_TIME_CONSTANT = 0.09f  // seconds; time-based easing toward the target scale
        const val INTERVAL_EASE = 0.3f         // smoothing for the measured sample interval
    }
}
