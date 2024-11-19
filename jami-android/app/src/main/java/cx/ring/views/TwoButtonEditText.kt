/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
import android.graphics.PorterDuff
import android.text.TextUtils
import android.text.method.KeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import java.util.MissingFormatArgumentException

class TwoButtonEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var mContext: Context? = null
    var editTextLayout: TextInputLayout? = null
        private set
    var editText: TextInputEditText? = null
        private set
    private var mButtonRight: AppCompatImageButton? = null
    private var mButtonLeft: AppCompatImageButton? = null

    init {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        mContext = context
        orientation = HORIZONTAL
        background = context.getDrawable(R.drawable.background_jami_edittext)
        LayoutInflater.from(context).inflate(R.layout.item_two_button_edittext, this, true)
        editTextLayout = findViewById(R.id.edit_text_layout)
        editText = findViewById(R.id.edit_text)
        mButtonRight = findViewById(R.id.btn_right)
        mButtonLeft = findViewById(R.id.btn_left)
        setPadding(0, 0, context.resources.getDimension(R.dimen.padding_small).toInt(), 0)
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.TwoButtonEditText, defStyleAttr, 0)
        for (i in 0 until a.indexCount) {
            val index = a.getIndex(i)
            if (index == R.styleable.TwoButtonEditText_android_text) {
                val resourceId = a.getResourceId(index, 0)
                if (0 != resourceId) {
                    try {
                        val string = resources.getString(resourceId)
                        text = string
                    } catch (e: MissingFormatArgumentException) {
                        // ignore
                    }
                }
            }
            if (index == R.styleable.TwoButtonEditText_android_hint) {
                val resourceId = a.getResourceId(index, 0)
                if (0 != resourceId) {
                    try {
                        val string = resources.getString(resourceId)
                        setHint(string)
                    } catch (e: MissingFormatArgumentException) {
                        // ignore
                    }
                }
            }
            if (index == R.styleable.TwoButtonEditText_android_tint) {
                setDrawableTint(a.getResourceId(index, 0))
            }
            if (index == R.styleable.TwoButtonEditText_drawable_right) {
                setRightDrawable(a.getResourceId(index, 0))
            }
            if (index == R.styleable.TwoButtonEditText_drawable_left) {
                setLeftDrawable(a.getResourceId(index, 0))
            }
            if (index == R.styleable.TwoButtonEditText_android_enabled) {
                isEnabled = a.getBoolean(index, true)
            }
            if (index == R.styleable.TwoButtonEditText_android_singleLine) {
                setSingleLine(a.getBoolean(index, false))
            }
        }
        a.recycle()
    }

    fun setHint(hint: CharSequence?) {
        editTextLayout!!.hint = hint
    }

    fun setText(@StringRes stringId: Int) {
        text = resources.getString(stringId)
    }

    fun setHint(@StringRes stringId: Int) {
        setHint(resources.getString(stringId))
    }

    var text: CharSequence?
        get() = editText!!.text.toString()
        set(text) {
            editText!!.setText(text)
        }

    fun setRightDrawable(@DrawableRes resId: Int) {
        mButtonRight!!.setImageResource(resId)
        mButtonRight!!.visibility = VISIBLE
    }

    fun setLeftDrawable(@DrawableRes resId: Int) {
        mButtonLeft!!.setImageResource(resId)
        mButtonLeft!!.visibility = VISIBLE
    }

    fun setDrawableTint(@ColorRes color: Int) {
        mButtonRight!!.setColorFilter(
            ContextCompat.getColor(mContext!!, color),
            PorterDuff.Mode.SRC_IN
        )
        mButtonLeft!!.setColorFilter(
            ContextCompat.getColor(mContext!!, color),
            PorterDuff.Mode.SRC_IN
        )
    }

    fun setSingleLine(singleLine: Boolean) {
        editText!!.isSingleLine = singleLine
        editText!!.ellipsize = TextUtils.TruncateAt.END
    }

    fun setRightDrawableOnClickListener(onClickListener: OnClickListener?) {
        mButtonRight!!.setOnClickListener(onClickListener)
    }

    fun setLeftDrawableOnClickListener(onClickListener: OnClickListener?) {
        mButtonLeft!!.setOnClickListener(onClickListener)
    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            editText!!.keyListener = editText!!.tag as KeyListener
        } else {
            editText!!.tag = editText!!.keyListener
            editText!!.keyListener = null
        }
    }
}
