package org.sflphone.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import org.sflphone.R;

/**
 * Created by lisional on 06/04/14.
 */
public class PasswordEditText extends RelativeLayout {
    LayoutInflater inflater = null;
    EditText edit_text;
    Button btn_clear;

    public PasswordEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public PasswordEditText(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        initViews();
    }

    void initViews() {
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.password_edittext, this, true);
        edit_text = (EditText) findViewById(R.id.password_edittext);
        edit_text.setSingleLine();
        edit_text.setImeOptions(EditorInfo.IME_ACTION_DONE);
        btn_clear = (Button) findViewById(R.id.password_visibility);
        btn_clear.setVisibility(RelativeLayout.INVISIBLE);
        revealText();
        edit_text.setTransformationMethod(PasswordTransformationMethod.getInstance());
        showHideClearButton();
    }

    void revealText() {
        btn_clear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        edit_text.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        edit_text.setSelection(edit_text.getText().length());
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        edit_text.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        edit_text.setSelection(edit_text.getText().length());
                        return true; // if you want to handle the touch event
                }
                return false;
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
}