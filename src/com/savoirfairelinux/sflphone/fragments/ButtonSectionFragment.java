package com.savoirfairelinux.sflphone.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Numpad;

public class ButtonSectionFragment extends Fragment
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
        view = inflater.inflate(R.layout.frag_button_section, parent, false);
        
        Numpad numpad = (Numpad) view.findViewById(R.id.numPad);

        mTextEntry = (EditText) parent.findViewById(R.id.phoneNumberTextEntry);
        if(mTextEntry == null){
            Log.i(TAG,"NULL");
        }

        numpad.setEditText(mTextEntry);
        

        return view;
    }
}
