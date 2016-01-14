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
 */

package cx.ring.fragments;

import android.content.Intent;
import android.os.RemoteException;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import cx.ring.model.account.CredentialsManager;
import cx.ring.model.account.SRTPManager;
import cx.ring.model.account.TLSManager;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

import java.util.ArrayList;

public class NestedSettingsFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;

    CredentialsManager mCredsManager;
    SRTPManager mSrtpManager;
    TLSManager mTlsManager;

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public IDRingService getRemoteService() {
            return null;
        }
        @Override
        public LocalService getService() {
            return null;
        }
        @Override
        public Account getAccount() {
            return null;
        }
    };

    public String[] getTlsMethods() {
        ArrayList<String> methods = null;
        try {
            methods = (ArrayList<String>) mCallbacks.getRemoteService().getTlsSupportedMethods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String[] results = new String[methods.size()];
        methods.toArray(results);
        return results;
    }

    public boolean checkCertificate(String crt) {
        /*try {
             return mCallbacks.getService().validateCertificate(crt);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    public boolean findRSAKey(String pemPath) {
        /*try {
            return mCallbacks.getService().checkForPrivateKey(pemPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    public interface Callbacks extends LocalService.Callbacks {
        Account getAccount();
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
            case 0: // Credentials
                addPreferencesFromResource(cx.ring.R.xml.account_credentials);
                mCredsManager = new CredentialsManager();
                mCredsManager.onCreate(getActivity(), getPreferenceScreen(), mCallbacks.getAccount());
                mCredsManager.reloadCredentials();
                mCredsManager.setAddCredentialListener();
                break;
            case 1: // SRTP
                mSrtpManager = new SRTPManager();
                if (mCallbacks.getAccount().hasSDESEnabled()) { // SDES
                    addPreferencesFromResource(cx.ring.R.xml.account_sdes);
                    mSrtpManager.onCreate(getPreferenceScreen(), mCallbacks.getAccount());
                    mSrtpManager.setSDESListener();
                } else { // ZRTP
                    addPreferencesFromResource(cx.ring.R.xml.account_zrtp);
                    mSrtpManager.onCreate(getPreferenceScreen(), mCallbacks.getAccount());
                    mSrtpManager.setZRTPListener();
                }
                break;
            case 2:
                addPreferencesFromResource(cx.ring.R.xml.account_tls);
                mTlsManager = new TLSManager();
                mTlsManager.onCreate(this, getPreferenceScreen(), mCallbacks.getAccount());
                mTlsManager.setTLSListener();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mTlsManager != null) {
            mTlsManager.onActivityResult(requestCode, resultCode, data);
        }

    }


}