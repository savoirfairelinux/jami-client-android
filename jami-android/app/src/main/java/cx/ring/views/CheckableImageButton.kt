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
