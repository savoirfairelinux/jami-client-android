/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.MissingFormatArgumentException;

import cx.ring.R;

public class TwoButtonEditText extends LinearLayout {

    private Context mContext;

    private TextInputLayout mEditTextLayout;
    private TextInputEditText mEditText;
    private AppCompatImageButton mButtonRight;
    private AppCompatImageButton mButtonLeft;

    public TwoButtonEditText(Context context) {
        this(context, null, 0, 0);
    }

    public TwoButtonEditText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public TwoButtonEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoButtonEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        setOrientation(HORIZONTAL);
        setBackground(context.getDrawable(R.drawable.background_jami_edittext));

        LayoutInflater.from(context).inflate(R.layout.item_two_button_edittext, this, true);

        mEditTextLayout = findViewById(R.id.edit_text_layout);
        mEditText = findViewById(R.id.edit_text);
        mButtonRight = findViewById(R.id.btn_right);
        mButtonLeft = findViewById(R.id.btn_left);

        setPadding(16, 0, 16, 0);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwoButtonEditText, defStyleAttr, 0);

        for (int i = 0; i < a.getIndexCount(); i++) {
            int index = a.getIndex(i);

            if (index == R.styleable.TwoButtonEditText_android_text) {
                int resourceId = a.getResourceId(index, 0);
                if (0 != resourceId) {
                    try {
                        String string = getResources().getString(resourceId);
                        setText(string);
                    } catch (MissingFormatArgumentException e) {
                        // ignore
                    }
                }
            }
            if (index == R.styleable.TwoButtonEditText_android_hint) {
                int resourceId = a.getResourceId(index, 0);
                if (0 != resourceId) {
                    try {
                        String string = getResources().getString(resourceId);
                        setHint(string);
                    } catch (MissingFormatArgumentException e) {
                        // ignore
                    }
                }
            }
            if (index == R.styleable.TwoButtonEditText_android_tint) {
                setDrawableTint(a.getResourceId(index, 0));
            }
            if (index == R.styleable.TwoButtonEditText_drawable_right) {
                setRightDrawable(a.getResourceId(index, 0));
            }
            if (index == R.styleable.TwoButtonEditText_drawable_left) {
                setLeftDrawable(a.getResourceId(index, 0));
            }
            if (index == R.styleable.TwoButtonEditText_android_enabled) {
                setEnabled(a.getBoolean(index, true));
            }
            if (index == R.styleable.TwoButtonEditText_android_singleLine) {
                setSingleLine(a.getBoolean(index, false));
            }
        }

        a.recycle();
    }

    public void setText(CharSequence text) {
        mEditText.setText(text);
    }

    public void setHint(CharSequence hint) {
        mEditTextLayout.setHint(hint);
    }

    public void setText(@StringRes int stringId) {
        setText(getResources().getString(stringId));
    }

    public void setHint(@StringRes int stringId) {
        setHint(getResources().getString(stringId));
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void setRightDrawable(@DrawableRes int resId) {
        mButtonRight.setImageResource(resId);
        mButtonRight.setVisibility(VISIBLE);
    }

    public void setLeftDrawable(@DrawableRes int resId) {
        mButtonLeft.setImageResource(resId);
        mButtonLeft.setVisibility(VISIBLE);
    }

    public void setDrawableTint(@ColorRes int color) {
        mButtonRight.setColorFilter(ContextCompat.getColor(mContext, color), android.graphics.PorterDuff.Mode.SRC_IN);
        mButtonLeft.setColorFilter(ContextCompat.getColor(mContext, color), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    public void setSingleLine(boolean singleLine) {
        mEditText.setSingleLine(singleLine);
        mEditText.setEllipsize(TextUtils.TruncateAt.END);
    }

    public void setRightDrawableOnClickListener(OnClickListener onClickListener) {
        mButtonRight.setOnClickListener(onClickListener);
    }

    public void setLeftDrawableOnClickListener(OnClickListener onClickListener) {
        mButtonLeft.setOnClickListener(onClickListener);
    }

    public TextInputEditText getEditText() {
        return mEditText;
    }

    public TextInputLayout getEditTextLayout() {
        return mEditTextLayout;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mEditText.setKeyListener((KeyListener) mEditText.getTag());
        } else {
            mEditText.setTag(mEditText.getKeyListener());
            mEditText.setKeyListener(null);
        }
    }

}
