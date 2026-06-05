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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
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

    private val barWidth = dp(3f)
    private val barGap = dp(2f)
    private val cornerRadius = dp(1.5f)
    private val minBarHeight = dp(2f)

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF1565C0.toInt())
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = (resolveThemeColor(com.google.android.material.R.attr.colorOnSurface, 0xFF000000.toInt()) and 0x00FFFFFF) or 0x40000000
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
        invalidate()
    }

    /** Append a single amplitude sample (used live while recording). */
    fun addAmplitude(value: Float) {
        amplitudes.add(value.coerceIn(0f, 1f))
        invalidate()
    }

    fun clear() {
        amplitudes.clear()
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
            val slot = barWidth + barGap
            val maxBars = max(1, (w / slot).toInt())
            val bars = resample(amplitudes, min(maxBars, amplitudes.size))
            val count = bars.size
            val step = w / count
            val progressX = w * progress
            for (i in 0 until count) {
                val cx = i * step + step / 2f
                val half = max(minBarHeight, bars[i] * maxHalf)
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
            val visible = min(amplitudes.size, (w / slot).toInt() + 1)
            val start = amplitudes.size - visible
            var x = w - barWidth
            for (i in amplitudes.size - 1 downTo start) {
                val half = max(minBarHeight, amplitudes[i] * maxHalf)
                canvas.drawRoundRect(
                    x, centerY - half,
                    x + barWidth, centerY + half,
                    cornerRadius, cornerRadius, activePaint
                )
                x -= slot
                if (x + barWidth < 0) break
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!fitMode || onSeek == null) return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val fraction = (event.x / width).coerceIn(0f, 1f)
                progress = fraction
                onSeek?.invoke(fraction)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    /** Aggregates [src] into [target] buckets (averaging) so dense waveforms render as clean bars. */
    private fun resample(src: List<Float>, target: Int): List<Float> {
        if (target <= 0 || target >= src.size) return src
        val out = ArrayList<Float>(target)
        for (i in 0 until target) {
            val start = i * src.size / target
            val end = max(start + 1, (i + 1) * src.size / target)
            var sum = 0f
            for (j in start until end) sum += src[j]
            out.add(sum / (end - start))
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
}
