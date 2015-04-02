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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.sflphone.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import org.sflphone.model.account.AccountDetail;
import org.sflphone.model.account.AccountDetailAdvanced;
import org.sflphone.model.account.Account;

public class AdvancedAccountFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
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

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);
        setPreferenceDetails(mCallbacks.getAccount().getAdvancedDetails());
        addPreferenceListener(mCallbacks.getAccount().getAdvancedDetails(), changeAdvancedPreferenceListener);

    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (p.mKey == AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE) {
                    ArrayList<CharSequence> entries = getNetworkInterfaces();
                    CharSequence[] display = new CharSequence[entries.size()];
                    entries.toArray(display);
                    ((ListPreference) pref).setEntries(display);
                    ((ListPreference) pref).setEntryValues(display);
                    pref.setSummary(p.mValue);
                    continue;
                }
                if (!p.isTwoState) {
                    pref.setSummary(p.mValue);
                } else if (pref.getKey().contentEquals(AccountDetailAdvanced.CONFIG_STUN_ENABLE)) {
                    ((CheckBoxPreference) pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_STUN_SERVER).setEnabled(p.mValue.contentEquals("true"));
                } else if (pref.getKey().contentEquals("Account.publishedSameAsLocal")) {
                    ((CheckBoxPreference) pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT).setEnabled(!p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS).setEnabled(!p.mValue.contentEquals("true"));
                }
            } else {
                Log.w(TAG, "pref not found");
            }
        }
    }

    private ArrayList<CharSequence> getNetworkInterfaces() {
        ArrayList<CharSequence> result = new ArrayList<CharSequence>();
        result.add("default");
        try {

            for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();) {
                NetworkInterface i = list.nextElement();
                if (i.isUp())
                    result.add(i.getDisplayName());
            }
        } catch (SocketException e) {
            Log.e(TAG, e.toString());
        }
        return result;
    }

    private void addPreferenceListener(AccountDetail details, OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "addPreferenceListener: pref " + p.mKey + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {

                pref.setOnPreferenceChangeListener(listener);

            } else {
                Log.w(TAG, "addPreferenceListener: pref not found");
            }
        }
    }

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if (preference instanceof CheckBoxPreference) {
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), newValue.toString());
                if (preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_STUN_ENABLE)) {
                    findPreference(AccountDetailAdvanced.CONFIG_STUN_SERVER).setEnabled((Boolean) newValue);
                } else if (preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL)) {
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT).setEnabled(!(Boolean) newValue);
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS).setEnabled(!(Boolean) newValue);
                }
            } else {
                Log.i(TAG, "Changing" + preference.getKey() + " value:" + newValue);
                if(preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MAX) ||
                        preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MIN))
                    newValue = adjustRtpRange(Integer.valueOf((String) newValue));

                preference.setSummary((CharSequence) newValue);
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), newValue.toString());
            }

            mCallbacks.getAccount().notifyObservers();
            return true;
        }
    };

    private String adjustRtpRange(int newValue) {
        if(newValue < 1024)
            return "1024";
        if(newValue > 65534)
            return "65534";
        return String.valueOf(newValue);
    }

}
