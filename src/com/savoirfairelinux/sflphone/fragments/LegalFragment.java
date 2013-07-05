package com.savoirfairelinux.sflphone.fragments;

import com.savoirfairelinux.sflphone.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LegalFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_legal, parent, false);

        return inflatedView;
    }


}
