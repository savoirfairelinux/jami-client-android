/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import cx.ring.R;
import cx.ring.client.AccountCallbacks;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountConfig;
import cx.ring.model.account.ConfigKey;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class AdvancedAccountFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);

        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            setPreferenceDetails(acc.getConfig());
            setPreferenceListener(acc.getConfig(), changeAdvancedPreferenceListener);
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof EditTextIntegerPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_NUMBER);
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof PasswordPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setPreferenceDetails(AccountConfig details) {
        for (ConfigKey k : details.getKeys()) {
            //Log.i(TAG, "setPreferenceDetails: pref " + k.key() + " value "/* + p.mValue*/);
            Preference pref = findPreference(k.key());
            if (pref != null) {
                if (k == ConfigKey.LOCAL_INTERFACE) {
                    String val = details.get(k);
                    ArrayList<CharSequence> entries = getNetworkInterfaces();
                    CharSequence[] display = entries.toArray(new CharSequence[entries.size()]);
                    ListPreference lp = (ListPreference) pref;
                    lp.setEntries(display);
                    lp.setEntryValues(display);
                    lp.setSummary(val);
                    lp.setValue(val);
                } else if (!k.isTwoState()) {
                    String val = details.get(k);
                    pref.setSummary(val);
                    if (pref instanceof EditTextPreference)
                        ((EditTextPreference) pref).setText(val);
                } else {
                    ((TwoStatePreference) pref).setChecked(details.getBool(k));
                }
            }
        }
    }

    /*private void addPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = findPreference(p.mKey);
            if (pref != null)
                pref.setOnPreferenceChangeListener(listener);
        }
    }*/
    private void setPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey k : details.getKeys()) {
            //Log.i(TAG, "setPreferenceListener: pref " + p.mKey + " " + p.mValue);
            Preference pref = findPreference(k.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            } /*else {
                Log.w(TAG, "setPreferenceListener: pref not found");
            }*/
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
            Log.e(TAG, "Error enumerating interfaces: ", e);
        }
        return result;
    }

    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account acc = mCallbacks.getAccount();
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            Log.i(TAG, "Changing " + preference.getKey() + " value: " + newValue);

            if (preference instanceof TwoStatePreference) {
                acc.setDetail(key, newValue.toString());
            } else if (preference instanceof PasswordPreference) {
                acc.setDetail(key, newValue.toString());
                preference.setSummary(TextUtils.isEmpty(newValue.toString()) ? "" : "******");
            } else {
                if (key == ConfigKey.AUDIO_PORT_MAX || key == ConfigKey.AUDIO_PORT_MIN)
                    newValue = adjustRtpRange(Integer.valueOf((String) newValue));
                preference.setSummary(newValue.toString());
                acc.setDetail(key, newValue.toString());
            }

            mCallbacks.saveAccount();
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
