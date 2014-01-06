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

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.model.Account;
import org.sflphone.views.PasswordPreference;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class GeneralAccountFragment extends PreferenceFragment {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
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
        addPreferencesFromResource(R.xml.account_general_prefs);
        setPreferenceDetails(mCallbacks.getAccount().getBasicDetails());
        addPreferenceListener(mCallbacks.getAccount().getBasicDetails(), changeBasicPreferenceListener);

    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    ((EditTextPreference) pref).setText(p.mValue);
                    if (pref instanceof PasswordPreference) {
                        String tmp = new String();
                        for (int i = 0; i < p.mValue.length(); ++i) {
                            tmp += "*";

                        }
                        pref.setSummary(tmp);
                    } else {
                        pref.setSummary(p.mValue);
                    }
                } else {
                    ((CheckBoxPreference) pref).setChecked(p.isChecked());
                }
            } else {
                Log.w(TAG, "pref not found");
            }
        }
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

    Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            Log.i(TAG, "Changing preference value:" + newValue);
            if (preference instanceof CheckBoxPreference) {
                mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((Boolean) newValue).toString());
            } else {
                if (preference instanceof PasswordPreference) {
                    String tmp = new String();
                    for (int i = 0; i < ((String) newValue).length(); ++i) {
                        tmp += "*";

                    }
                    preference.setSummary(tmp);
                } else
                    preference.setSummary((CharSequence) newValue);
                
                mCallbacks.getAccount().getBasicDetails().setDetailString(preference.getKey(), ((CharSequence) newValue).toString());
            }
            mCallbacks.getAccount().notifyObservers();
            return true;
        }
    };

}
