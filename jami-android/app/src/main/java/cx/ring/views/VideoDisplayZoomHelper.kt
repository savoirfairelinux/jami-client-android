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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View

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
    private var scale = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            val focusX = detector.focusX
            val focusY = detector.focusY
            val ratio = newScale / scale
            translateX = focusX - (focusX - translateX) * ratio
            translateY = focusY - (focusY - translateY) * ratio
            scale = newScale
            clampTranslation()
            apply()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scale <= minScale) return false
            translateX -= dx
            translateY -= dy
            clampTranslation()
            apply()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            scale = 1f
            translateX = 0f
            translateY = 0f
            apply()
            return true
        }
    })

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
            true
        }
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
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
        val combined = Matrix(baseMatrix)
        combined.postScale(scale, scale, cx, cy)
        combined.postTranslate(translateX, translateY)
        view.setTransform(combined)
    }
}
