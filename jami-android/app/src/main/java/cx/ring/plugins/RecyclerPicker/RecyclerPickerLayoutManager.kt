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