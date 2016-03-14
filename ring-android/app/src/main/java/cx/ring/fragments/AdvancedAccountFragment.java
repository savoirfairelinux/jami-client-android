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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import cx.ring.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v14.preference.EditTextPreferenceDialogFragment;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.Account;
import cx.ring.views.EditTextIntegerPreference;

public class AdvancedAccountFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public Account getAccount() {
            return null;
        }
    };
    public interface Callbacks {
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
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);

        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            setPreferenceDetails(acc.getAdvancedDetails());
            addPreferenceListener(acc.getAdvancedDetails(), changeAdvancedPreferenceListener);
        }
    }

    public static class EditTextPreferenceDialog extends EditTextPreferenceDialogFragment {
        public static EditTextPreferenceDialog newInstance(String key) {
            final EditTextPreferenceDialog fragment = new EditTextPreferenceDialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            EditText text = (EditText)view.findViewById(android.R.id.edit);
            text.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof EditTextIntegerPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (p.mKey.equals(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE)) {
                    ArrayList<CharSequence> entries = getNetworkInterfaces();
                    CharSequence[] display = entries.toArray(new CharSequence[entries.size()]);
                    ListPreference lp = (ListPreference) pref;
                    lp.setEntries(display);
                    lp.setEntryValues(display);
                    lp.setSummary(p.mValue);
                    lp.setValue(p.mValue);
                    continue;
                }
                if (!p.isTwoState) {
                    pref.setSummary(p.mValue);
                    if (pref instanceof EditTextPreference)
                        ((EditTextPreference) pref).setText(p.mValue);
                } else if (pref.getKey().contentEquals(AccountDetailAdvanced.CONFIG_STUN_ENABLE)) {
                    ((SwitchPreference) pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_STUN_SERVER).setEnabled(p.mValue.contentEquals("true"));
                } else if (pref.getKey().contentEquals("Account.publishedSameAsLocal")) {
                    ((CheckBoxPreference) pref).setChecked(p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT).setEnabled(!p.mValue.contentEquals("true"));
                    findPreference(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS).setEnabled(!p.mValue.contentEquals("true"));
                }
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = findPreference(p.mKey);
            if (pref != null)
                pref.setOnPreferenceChangeListener(listener);
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

                preference.setSummary(newValue.toString());
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
