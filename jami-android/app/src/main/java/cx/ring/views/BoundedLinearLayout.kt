/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
import android.util.AttributeSet
import android.widget.LinearLayout
import cx.ring.R

class BoundedLinearLayout : LinearLayout {
    private val mBoundedWidth: Int
    private val mBoundedHeight: Int

    constructor(context: Context) : super(context) {
        mBoundedWidth = 0
        mBoundedHeight = 0
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BoundedScrollView)
        mBoundedWidth = typedArray.getDimensionPixelSize(R.styleable.BoundedScrollView_bounded_width, 0)
        mBoundedHeight = typedArray.getDimensionPixelSize(R.styleable.BoundedScrollView_bounded_height, 0)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Adjust width as necessary
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        if (mBoundedWidth > 0 && mBoundedWidth < measuredWidth) {
            val measureMode = MeasureSpec.getMode(widthMeasureSpec)
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mBoundedWidth, measureMode)
        }
        // Adjust height as necessary
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (mBoundedHeight > 0 && mBoundedHeight < measuredHeight) {
            val measureMode = MeasureSpec.getMode(heightMeasureSpec)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mBoundedHeight, measureMode)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}