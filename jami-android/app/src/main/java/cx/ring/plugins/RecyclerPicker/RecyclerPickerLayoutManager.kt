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
package cx.ring.plugins.RecyclerPicker

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView.Recycler

class RecyclerPickerLayoutManager(
    context: Context?,
    orientation: Int,
    reverseLayout: Boolean,
    private val listener: ItemSelectedListener
) : LinearLayoutManager(context, orientation, reverseLayout) {
    private var recyclerView: RecyclerView? = null
    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        recyclerView = view

        // Smart snapping
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        scaleDownView()
    }

    override fun scrollHorizontallyBy(
        dx: Int, recycler: Recycler,
        state: RecyclerView.State
    ): Int {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        return if (orientation == VERTICAL) {
            0
        } else {
            scaleDownView()
            scrolled
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        // When scroll stops we notify on the selected item
        if (state == RecyclerView.SCROLL_STATE_IDLE) {

            // Find the closest child to the recyclerView center --> this is the selected item.
            val recyclerViewCenterX = recyclerViewCenterX
            var minDistance = recyclerView!!.width
            var position = -1
            for (i in 0 until recyclerView!!.childCount) {
                val child = recyclerView!!.getChildAt(i)
                val childCenterX = getDecoratedLeft(child) + (getDecoratedRight(child) - getDecoratedLeft(child)) / 2
                val newDistance = Math.abs(childCenterX - recyclerViewCenterX)
                if (newDistance < minDistance) {
                    minDistance = newDistance
                    position = recyclerView!!.getChildLayoutPosition(child)
                }
            }
            listener.onItemSelected(position)
        }
    }

    private val recyclerViewCenterX: Int
        private get() = recyclerView!!.width / 2 + recyclerView!!.left

    private fun scaleDownView() {
        val mid = width / 2.0f
        for (i in 0 until childCount) {

            // Calculating the distance of the child from the center
            val child = getChildAt(i)
            val childMid = (getDecoratedLeft(child!!) + getDecoratedRight(child)) / 2.0f
            val distanceFromCenter = Math.abs(mid - childMid)

            // The scaling formula
            var k = Math.sqrt((distanceFromCenter / width).toDouble()).toFloat()
            k *= 1.5f
            val scale = 1 - k * 0.66f

            // Set scale to view
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    interface ItemSelectedListener {
        fun onItemSelected(position: Int)
        fun onItemClicked(position: Int)
    }
}