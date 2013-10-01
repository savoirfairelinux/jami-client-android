package com.savoirfairelinux.sflphone.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.savoirfairelinux.sflphone.R;

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
                // TODO Auto-generated method stub
                edit_text.setText("");
            }
        });
    }

    void showHideClearButton() {
        edit_text.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
                if (s.length() > 0)
                    btn_clear.setVisibility(RelativeLayout.VISIBLE);
                else
                    btn_clear.setVisibility(RelativeLayout.INVISIBLE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

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
}
