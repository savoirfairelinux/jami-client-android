package com.savoirfairelinux.sflphone.client;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class ButtonSectionFragment extends Fragment implements OnClickListener
{
    static final String TAG = "ButtonSectionFragment";
    EditText mTextEntry;


    public ButtonSectionFragment()
    {
        setRetainInstance(true);
    }

    public static final String ARG_SECTION_NUMBER = "section_number";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        View view;
        Log.i(TAG, "onCreateView" );
        view = inflater.inflate(R.layout.test_layout, parent, false);

        Numpad numpad = (Numpad) view.findViewById(R.id.numPad);

        Button makeCall = (Button) view.findViewById(R.id.buttonCall); 
        makeCall.setOnClickListener(this);

        mTextEntry = (EditText) view.findViewById(R.id.numDisplay);

        numpad.setEditText(mTextEntry);

        return view;
    }

    @Override
    public void onClick(View v)
    {
        Log.i(TAG, "onClick from ButtonSectionFragment");
        if(v.getId() == R.id.buttonCall) {
            Editable editText = mTextEntry.getText();
            editText.clear();
        }
    }
}
