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