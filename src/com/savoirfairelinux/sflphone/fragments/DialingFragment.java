package com.savoirfairelinux.sflphone.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.HistoryAdapter;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class DialingFragment extends Fragment{
    
    private static final String TAG = HistoryFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";
    private boolean isReady;
    private ISipService service;
    HistoryAdapter mAdapter;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new HistoryAdapter(getActivity(),new ArrayList<HashMap<String, String>>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_dialing, parent, false);

        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }


}
