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
import android.util.AttributeSet
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")
class ConversationImageView : ImageView {
    private val mMaxHeight: Int

    constructor(context: Context) : super(context) {
        mMaxHeight = 0
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mMaxHeight = maxHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        // Avoid image to take zero space when not loaded yet
        if (measuredHeight == 0) {
            val measureMode = MeasureSpec.getMode(heightMeasureSpec)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}