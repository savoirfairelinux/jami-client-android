/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.fragments;

import java.util.Locale;

import android.content.Intent;
import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.model.Account;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

public class SecurityAccountFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    private static final String TAG = SecurityAccountFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public Account getAccount() {
            return null;
        }

        @Override
        public void displayCredentialsScreen() {
        }

        @Override
        public void displaySRTPScreen() {
        }

        @Override
        public void displayTLSScreen() {
        }

    };

    public interface Callbacks {

        public Account getAccount();

        public void displayCredentialsScreen();

        public void displaySRTPScreen();

        public void displayTLSScreen();

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
    public void onResume() {
        super.onResume();
        if(mCallbacks.getAccount().getTlsDetails().getDetailBoolean("TLS.enable")){
            findPreference("TLS.details").setSummary(getString(R.string.account_tls_enabled_label));
        } else {
            findPreference("TLS.details").setSummary(getString(R.string.account_tls_disabled_label));
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_security_prefs);
        updateSummaries();
        findPreference("Credential.count").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                mCallbacks.displayCredentialsScreen();
                return false;
            }
        });

        setSrtpPreferenceDetails(mCallbacks.getAccount().getSrtpDetails());
        addPreferenceListener(mCallbacks.getAccount().getSrtpDetails(), changeSrtpModeListener);

        findPreference("TLS.details").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displayTLSScreen();
                return false;
            }
        });

    }

    public void updateSummaries() {
        findPreference("Credential.count").setSummary("" + mCallbacks.getAccount().getCredentials().size());
        if(mCallbacks.getAccount().getTlsDetails().getDetailBoolean("TLS.enable")){
            findPreference("TLS.details").setSummary(getString(R.string.account_tls_enabled_label));
        } else {
            findPreference("TLS.details").setSummary(getString(R.string.account_tls_disabled_label));
        }
    }

    private void setSrtpPreferenceDetails(AccountDetailSrtp details) {

        if (details.getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)) {
            findPreference(AccountDetailSrtp.CONFIG_SRTP_ENABLE).setSummary(
                    details.getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE).toUpperCase(Locale.getDefault()));

        } else {
            findPreference(AccountDetailSrtp.CONFIG_SRTP_ENABLE).setSummary(getResources().getString(R.string.account_srtp_deactivated));

        }

        findPreference("SRTP.details").setEnabled(details.getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE));
    }

    private void addPreferenceListener(AccountDetail details, OnPreferenceChangeListener listener) {

        findPreference(AccountDetailSrtp.CONFIG_SRTP_ENABLE).setOnPreferenceChangeListener(changeSrtpModeListener);
        findPreference("SRTP.details").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                mCallbacks.displaySRTPScreen();
                return false;
            }
        });

    }

    Preference.OnPreferenceChangeListener changeSrtpModeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if (((String) newValue).contentEquals("NONE")) {
                mCallbacks.getAccount().getSrtpDetails().setDetailString(AccountDetailSrtp.CONFIG_SRTP_ENABLE, AccountDetail.FALSE_STR);
                preference.setSummary(getResources().getString(R.string.account_srtp_deactivated));
            } else {
                mCallbacks.getAccount().getSrtpDetails().setDetailString(AccountDetailSrtp.CONFIG_SRTP_ENABLE, AccountDetail.TRUE_STR);
                mCallbacks.getAccount().getSrtpDetails()
                        .setDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, ((String) newValue).toLowerCase(Locale.getDefault()));
                preference.setSummary(((String) newValue));
            }
            findPreference("SRTP.details").setEnabled(!((String) newValue).contentEquals("NONE"));
            mCallbacks.getAccount().notifyObservers();
            return true;
        }
    };

}