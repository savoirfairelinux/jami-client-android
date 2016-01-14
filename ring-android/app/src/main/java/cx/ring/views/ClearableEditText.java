/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.views;

import cx.ring.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView.OnEditorActionListener;



public class ClearableEditText extends RelativeLayout {
    LayoutInflater inflater = null;
    EditText edit_text;
    Button btn_clear;
    private TextWatcher watch = null;

    public ClearableEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    public ClearableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public ClearableEditText(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        initViews();
    }

    void initViews() {
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.clearable_edit_text, this, true);
        edit_text = (EditText) findViewById(R.id.clearable_edit);
        edit_text.setSingleLine();
        edit_text.setImeOptions(EditorInfo.IME_ACTION_DONE);
        btn_clear = (Button) findViewById(R.id.clearable_button_clear);
        btn_clear.setVisibility(RelativeLayout.INVISIBLE);

        // Dummy listener to fix an sdk issue: https://code.google.com/p/android/issues/detail?id=21775 
        edit_text.setOnDragListener(new OnDragListener() {

            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DROP)
                    return true;
                else
                    return false;
            }
        });
        clearText();
        showHideClearButton();
    }

    void clearText() {
        btn_clear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_text.setText("");
            }
        });
    }

    void showHideClearButton() {
        edit_text.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0)
                    btn_clear.setVisibility(RelativeLayout.VISIBLE);
                else
                    btn_clear.setVisibility(RelativeLayout.INVISIBLE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public Editable getText() {
        Editable text = edit_text.getText();
        return text;
    }

    public void setInputType(int typeClassNumber) {
        edit_text.setFocusableInTouchMode(true);
        edit_text.requestFocus();
        edit_text.setInputType(typeClassNumber);

    }

    public EditText getEdit_text() {
        return edit_text;
    }

    public void setError(String string) {
        edit_text.setError(string);
        edit_text.requestFocus();
    }

    public void setTextWatcher(TextWatcher l) {
        watch = l;
        edit_text.addTextChangedListener(watch);
    }

    public void unsetTextWatcher() {
        edit_text.removeTextChangedListener(watch);
    }

    public void setOnEditorActionListener(OnEditorActionListener onEditorActionListener) {
        edit_text.setOnEditorActionListener(onEditorActionListener);
        
    }
}
