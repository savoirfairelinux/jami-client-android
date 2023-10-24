package cx.ring.views

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton

class CheckableImageButton(context: Context, attrs: AttributeSet?) :
    AppCompatImageButton(context, attrs), Checkable {
    private var mChecked = false
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    init {
        val a = context.obtainStyledAttributes(attrs, CHECKED_STATE_SET)
        val checked = a.getBoolean(0, false)
        isChecked = checked
        a.recycle()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshDrawableState()
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener!!.onCheckedChanged(this, checked)
            }
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     *
     * @param listener
     * the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        onCheckedChangeListener = listener
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a button has changed.
         *
         * @param button
         * The button view whose state has changed.
         * @param isChecked
         * The new checked state of button.
         */
        fun onCheckedChanged(button: Checkable?, isChecked: Boolean)
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(
            R.attr.state_checked
        )
    }
}
