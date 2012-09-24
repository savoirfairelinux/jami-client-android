package com.savoirfairelinux.sflphone.client;

import android.content.Context;
import android.text.Editable;
import android.widget.TableLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.savoirfairelinux.sflphone.R;

public class Numpad extends TableLayout implements OnClickListener
{
    private static final String TAG = "Numpad";
    private static final SparseArray<String> DIGITS_NAME = new SparseArray<String>();
    private EditText mEditText;

    static {
        DIGITS_NAME.put(R.id.numButton0, "0");
        DIGITS_NAME.put(R.id.numButton1, "1");
        DIGITS_NAME.put(R.id.numButton2, "2");
        DIGITS_NAME.put(R.id.numButton3, "3");
        DIGITS_NAME.put(R.id.numButton4, "4");
        DIGITS_NAME.put(R.id.numButton5, "5");
        DIGITS_NAME.put(R.id.numButton6, "6");
        DIGITS_NAME.put(R.id.numButton7, "7");
        DIGITS_NAME.put(R.id.numButton8, "8");
        DIGITS_NAME.put(R.id.numButton9, "9");
        DIGITS_NAME.put(R.id.numButtonStar, "*");
        DIGITS_NAME.put(R.id.numButtonSharp, "#");
    }

    public Numpad(Context context)
    {
        super(context);
    }

    public Numpad(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.numpad, this, true);
    }

    protected void onFinishInflate()
    {
        super.onFinishInflate();

        for(int i = 0; i < DIGITS_NAME.size(); i++)
        {
            Button b = (Button) findViewById(DIGITS_NAME.keyAt(i));
            b.setText(DIGITS_NAME.valueAt(i));
            b.setOnClickListener(this);
        }
    }

    public void setEditText(EditText editText)
    {
        mEditText = editText;
    } 

    @Override
    public void onClick(View v)
    {
        Log.i(TAG, "Clicked " + ((Button)v).getText().toString() );
        Editable edit = mEditText.getText(); 
        edit.append(((Button)v).getText());
    }
}
