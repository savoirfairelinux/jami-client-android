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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Adds pinch-to-zoom (and one-finger pan once zoomed-in, plus double-tap reset) to a [TextureView].
 *
 * The host code must pass its existing aspect-ratio Matrix via [setBaseTransform]; this helper combines
 * the user zoom/pan with that base matrix and calls [TextureView.setTransform].
 *
 * The host should also forward touch events via [onTouchEvent]. The simplest way is:
 *   view.setOnTouchListener { _, ev -> helper.onTouchEvent(ev) }
 */
class VideoDisplayZoomHelper(
    context: Context,
    private val view: TextureView,
    private val minScale: Float = 1f,
    private val maxScale: Float = 5f,
) {
    private val baseMatrix = Matrix()
    private val combinedMatrix = Matrix()
    private var scale = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            applyZoomAroundFocus(newScale, detector.focusX, detector.focusY)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            stopProgressiveZoom()
            if (scale <= minScale) return false
            translateX -= dx
            translateY -= dy
            clampTranslation()
            apply()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            stopProgressiveZoom()
            scale = 1f
            translateX = 0f
            translateY = 0f
            apply()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            startProgressiveZoom(e.x, e.y)
        }
    })

    private var progressiveAnimator: ValueAnimator? = null

    /**
     * Update scale while keeping the point [focusX, focusY] (screen coords) fixed.
     * The base matrix applies postScale around the view center (cx, cy), so the screen
     * position of the image point under the focus is:
     *   pos = oldScale * (P - C) + C + T_old
     * Solving for T_new so pos stays at focus:
     *   T_new = (1 - r) * (F - C) + r * T_old   with r = newScale/oldScale
     */
    private fun applyZoomAroundFocus(newScale: Float, focusX: Float, focusY: Float) {
        val cx = view.width / 2f
        val cy = view.height / 2f
        val ratio = if (scale == 0f) 1f else newScale / scale
        translateX = (1f - ratio) * (focusX - cx) + ratio * translateX
        translateY = (1f - ratio) * (focusY - cy) + ratio * translateY
        scale = newScale
        clampTranslation()
        apply()
    }

    private fun startProgressiveZoom(focusX: Float, focusY: Float) {
        progressiveAnimator?.cancel()
        if (scale >= maxScale - 0.001f) return
        val start = scale
        val end = maxScale
        val total = (maxScale - 1f).coerceAtLeast(0.01f)
        val duration = (3000L * (end - start) / total).toLong().coerceAtLeast(150L)
        progressiveAnimator = ValueAnimator.ofFloat(start, end).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                val newScale = (it.animatedValue as Float).coerceIn(minScale, maxScale)
                applyZoomAroundFocus(newScale, focusX, focusY)
            }
            start()
        }
    }

    private fun stopProgressiveZoom() {
        progressiveAnimator?.cancel()
        progressiveAnimator = null
    }

    /** Update the base (aspect-ratio / rotation) matrix to combine with user zoom. */
    fun setBaseTransform(matrix: Matrix) {
        baseMatrix.set(matrix)
        apply()
    }

    /** Reset zoom/pan to identity (e.g. on call end or camera switch). */
    fun reset() {
        scale = 1f
        translateX = 0f
        translateY = 0f
        apply()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        view.setOnTouchListener { v: View, ev: MotionEvent ->
            scaleDetector.onTouchEvent(ev)
            gestureDetector.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_UP -> {
                    stopProgressiveZoom()
                    v.performClick()
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_DOWN -> stopProgressiveZoom()
            }
            true
        }
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_DOWN -> stopProgressiveZoom()
        }
        return true
    }

    private fun clampTranslation() {
        if (scale <= 1f) {
            translateX = 0f
            translateY = 0f
            return
        }
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        val maxTx = (w * (scale - 1f)) / 2f
        val maxTy = (h * (scale - 1f)) / 2f
        translateX = translateX.coerceIn(-maxTx, maxTx)
        translateY = translateY.coerceIn(-maxTy, maxTy)
    }

    private fun apply() {
        val cx = view.width / 2f
        val cy = view.height / 2f
        combinedMatrix.set(baseMatrix)
        combinedMatrix.postScale(scale, scale, cx, cy)
        combinedMatrix.postTranslate(translateX, translateY)
        view.setTransform(combinedMatrix)
    }
}
