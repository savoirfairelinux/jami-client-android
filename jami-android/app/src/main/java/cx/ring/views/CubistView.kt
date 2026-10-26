/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import cx.ring.R
import kotlin.math.max

/**
 * Renders the Cubist ("Just between us") painted canvas on the welcome screen,
 * aligning the horizontal painted table edge right above the welcome title so
 * the two whispering cubist faces in the upper register are never covered by
 * text. On inner wizard screens (where [landOn] is not called), renders the
 * quiet faceted cubist ground (`bg_welcome_cubist_calm`).
 */
class CubistView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val bgColor = ContextCompat.getColor(context, R.color.welcome_background)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var isHomeScreen = false
    private var cachedBitmap: Bitmap? = null
    private var cachedResId: Int = 0

    private var landingTarget: View? = null
    private var landingY: Float = 0f
    private val locSelf = IntArray(2)
    private val locTarget = IntArray(2)
    private val srcRect = Rect()
    private val dstRect = RectF()

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        updateLandingY()
    }

    fun landOn(target: View) {
        isHomeScreen = true
        landingTarget = target
        if (isAttachedToWindow) {
            target.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            updateLandingY()
        }
        invalidate()
    }

    private fun updateLandingY() {
        val target = landingTarget ?: return
        if (!target.isShown || width == 0 || height == 0) return
        getLocationOnScreen(locSelf)
        target.getLocationOnScreen(locTarget)
        val newY = (locTarget[1] - locSelf[1]).toFloat()
        if (newY != landingY) {
            landingY = newY
            invalidate()
        }
    }

    private fun getPainting(): Bitmap? {
        val resId = if (isHomeScreen) {
            R.drawable.bg_welcome_cubist_home
        } else {
            R.drawable.bg_welcome_cubist_calm
        }
        if (cachedBitmap != null && cachedResId == resId) {
            return cachedBitmap
        }
        cachedResId = resId
        cachedBitmap = BitmapFactory.decodeResource(resources, resId)
        return cachedBitmap
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        landingTarget?.viewTreeObserver?.addOnGlobalLayoutListener(layoutListener)
        updateLandingY()
    }

    override fun onDetachedFromWindow() {
        landingTarget?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawColor(bgColor)
        val bmp = getPainting() ?: return

        if (isHomeScreen) {
            // Scale painting to match screen width and keep the painted table edge
            // (at 43.2% of the 1080x2400 painting) cleanly above the welcome title.
            val scale = w / bmp.width.toFloat()
            val scaledH = bmp.height * scale
            val bmpTableBottomY = TABLE_EDGE_BOTTOM_FRAC * scaledH
            val maxTableY = if (landingY > 0f) {
                landingY - 8f * density
            } else {
                bmpTableBottomY
            }
            val dy = minOf(0f, maxTableY - bmpTableBottomY)

            srcRect.set(0, 0, bmp.width, bmp.height)
            dstRect.set(0f, dy, w, dy + scaledH)
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)

            // If the shifted painting ends above the bottom of a very tall screen,
            // extend the calm painted table plane seamlessly to the bottom.
            val bottomEdge = dy + scaledH
            if (bottomEdge < h) {
                srcRect.set(0, (bmp.height * 0.72f).toInt(), bmp.width, bmp.height)
                dstRect.set(0f, bottomEdge - 2f, w, h)
                canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
            }
        } else {
            // Center-crop the calm faceted cubist ground on secondary wizard screens.
            val scale = max(w / bmp.width.toFloat(), h / bmp.height.toFloat())
            val sw = bmp.width * scale
            val sh = bmp.height * scale
            val dx = (w - sw) * 0.5f
            val dy = (h - sh) * 0.5f
            srcRect.set(0, 0, bmp.width, bmp.height)
            dstRect.set(dx, dy, dx + sw, dy + sh)
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
        }
    }

    companion object {
        // In the home paintings (1080x2400), the horizontal painted
        // table edge spans y = 0.430..0.438 of the canvas height.
        private const val TABLE_EDGE_BOTTOM_FRAC = 0.438f
    }
}
