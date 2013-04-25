package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.HistoryAdapter;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HistoryFragment extends ListFragment {

    private static final String TAG = HistoryFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";
    private boolean isReady;
    private ISipService service;
    HistoryAdapter mAdapter;
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallSelected(SipCall c) {
        }

        @Override
        public ISipService getService() {
            Log.i(TAG, "Dummy");
            return null;
        }

    };

    public interface Callbacks {
        public void onCallSelected(SipCall c);

        public ISipService getService();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        isReady = false;
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new HistoryAdapter(getActivity(),new ArrayList<HashMap<String, String>>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_history, parent, false);
        isReady = true;
        if (isReady) {
            Log.i(TAG, "C PRET");
        }
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.w(TAG, "onStart");
        if (mCallbacks.getService() != null) {

            Log.i(TAG, "oncreateview");
            onServiceSipBinded(mCallbacks.getService());
        }
    }

    /**
     * Called by activity to pass a reference to sipservice to Fragment.
     * 
     * @param isip
     */
    public void onServiceSipBinded(ISipService isip) {
        Log.w(TAG, "onServiceSipBinded");
        if (isReady) {
            service = isip;
            ArrayList<HashMap<String, String>> history;
            try {
                history = (ArrayList<HashMap<String, String>>) mCallbacks.getService().getHistory();
                Log.i(TAG, "history size:" + history.size());
                mAdapter = new HistoryAdapter(getActivity(), history);
                getListView().setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();

            } catch (RemoteException e) {
                Log.i(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "nor Ready");
        }

    }
}
