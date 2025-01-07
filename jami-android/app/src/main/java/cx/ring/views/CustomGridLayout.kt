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
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout
import androidx.core.view.isVisible

class CustomGridLayout(context: Context?, attrs: AttributeSet?, defStyle: Int) :
    GridLayout(context, attrs, defStyle) {

    private val views = ArrayList<View>()

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null)

    private fun arrangeElements() {
        if (views.isEmpty()) {
            for (i in 0 until childCount) {
                views.add(getChildAt(i))
            }
        }
        removeAllViews()
        for (i in views.indices) {
            if (views[i].isVisible) addView(views[i])
        }
    }

     override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        arrangeElements()
        super.onLayout(changed, left, top, right, bottom)
    }
}