package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.account.CredentialsManager;
import org.sflphone.model.Account;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class NestedSettingsFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();


    private Callbacks mCallbacks = sDummyCallbacks;

    CredentialsManager mCredsManager;

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public Account getAccount() {
            return null;
        }

    };

    public interface Callbacks {

        public Account getAccount();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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

        setHasOptionsMenu(true);

        // Load the preferences from an XML resource
        switch (getArguments().getInt("MODE")) {
        case 0:
            addPreferencesFromResource(R.xml.account_credentials);
            mCredsManager = new CredentialsManager();
            mCredsManager.onCreate(getActivity(), getPreferenceScreen(), mCallbacks.getAccount());
            mCredsManager.reloadCredentials();
            mCredsManager.setAddCredentialListener();
            break;
        case 1:
            
            break;
        case 2:
            break;
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
        return view;
    }

}