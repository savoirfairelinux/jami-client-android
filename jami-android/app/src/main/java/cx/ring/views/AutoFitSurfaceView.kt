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
import android.view.SurfaceView
import androidx.core.view.ViewCompat
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : SurfaceView(context, attrs, defStyle) {
    private var mRatioWidth = 720
    private var mRatioHeight = 1280
    private val mSize: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        150f,
        context.resources.displayMetrics
    ).roundToInt()
    private val mBounds = listOf(Rect())

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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = min(MeasureSpec.getSize(widthMeasureSpec), mSize)
        val height = min(MeasureSpec.getSize(heightMeasureSpec), mSize)
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }
}
