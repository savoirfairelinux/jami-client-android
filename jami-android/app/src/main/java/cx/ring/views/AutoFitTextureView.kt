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
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import kotlin.jvm.JvmOverloads
import android.view.TextureView
import androidx.core.view.ViewCompat
import cx.ring.R
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitTextureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : TextureView(context, attrs, defStyle) {
    private var mRatioWidth = 720
    private var mRatioHeight = 1280
    private var isFullscreen = false
    private val mSize: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, context.resources.displayMetrics).roundToInt()
    private val mBounds = listOf(Rect())

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.AutoFitTextureView)
            isFullscreen = a.getBoolean(R.styleable.AutoFitTextureView_isFullscreen, false)
            a.recycle()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mBounds[0][left, top, right] = bottom
        ViewCompat.setSystemGestureExclusionRects(this, mBounds)
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        if (mRatioWidth != width || mRatioHeight != height) {
            mRatioWidth = width
            mRatioHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val targetWidth = if (isFullscreen) requestedWidth else min(requestedWidth, mSize)
        val targetHeight = if (isFullscreen) requestedHeight else min(requestedHeight, mSize)

        if (mRatioWidth == 0 || mRatioHeight == 0) {
            setMeasuredDimension(targetWidth, targetHeight)
            return
        }

        val aspectRatio = mRatioWidth.toFloat() / mRatioHeight
        val adjustedWidth: Int
        val adjustedHeight: Int

        if (isFullscreen) {
            // Fullscreen mode: adjust to fill parent while keeping aspect ratio
            if (targetHeight >= targetWidth) {
                adjustedHeight = targetHeight
                adjustedWidth = (adjustedHeight * aspectRatio).toInt().coerceAtMost(targetWidth)
            } else {
                adjustedWidth = targetWidth
                adjustedHeight = (adjustedWidth / aspectRatio).toInt().coerceAtMost(targetHeight)
            }
        } else {
            // Preview mode: fit inside a 150dp box with correct aspect ratio
            if (targetWidth < targetHeight * aspectRatio) {
                adjustedWidth = targetWidth
                adjustedHeight = (adjustedWidth / aspectRatio).toInt()
            } else {
                adjustedHeight = targetHeight
                adjustedWidth = (adjustedHeight * aspectRatio).toInt()
            }
        }

        setMeasuredDimension(adjustedWidth, adjustedHeight)
    }
}
