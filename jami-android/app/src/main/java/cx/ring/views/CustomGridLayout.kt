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
import android.util.AttributeSet
import android.widget.GridLayout
import androidx.core.view.isVisible

/**
 * GridLayout that reflows its visible children so hidden (GONE) children
 * don't leave holes in the grid.
 *
 * Children are never detached: cell specs of visible children are reassigned
 * in declaration order before each measure pass. Mutating the child list from
 * a layout pass (previous implementation) corrupts GridLayout's placement
 * state and produces misplaced, oversized cells.
 */
class CustomGridLayout(context: Context?, attrs: AttributeSet?, defStyle: Int) :
    GridLayout(context, attrs, defStyle) {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null)

    private fun arrangeElements() {
        val columns = columnCount
        var index = 0
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (!view.isVisible) continue
            val rowSpec = spec(index / columns, 1f)
            val columnSpec = spec(index % columns, 1f)
            val lp = view.layoutParams as LayoutParams
            if (lp.rowSpec != rowSpec || lp.columnSpec != columnSpec) {
                lp.rowSpec = rowSpec
                lp.columnSpec = columnSpec
                view.layoutParams = lp
            }
            index++
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        arrangeElements()
        super.onMeasure(widthSpec, heightSpec)
    }
}
