/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.services.AccountService;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

public class AdvancedAccountFragment extends PreferenceFragment {

    private static final String TAG = AdvancedAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    @Inject
    protected ConversationFacade mConversationFacade;

    @Inject
    protected AccountService mAccountService;

    private String mAccountID;
    Preference.OnPreferenceChangeListener changeAdvancedPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mAccountService.getAccount(mAccountID);
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            Log.i(TAG, "Changing " + preference.getKey() + " value: " + newValue);

            if (preference instanceof TwoStatePreference) {
                if (key != null && key.equals(ConfigKey.DHT_PUBLIC_IN)) {
                    mConversationFacade.clearConversations();
                }
                account.setDetail(key, newValue.toString());
            } else if (preference instanceof PasswordPreference) {
                account.setDetail(key, newValue.toString());
                preference.setSummary(TextUtils.isEmpty(newValue.toString()) ? "" : "******");
            } else {
                if (key == ConfigKey.AUDIO_PORT_MAX || key == ConfigKey.AUDIO_PORT_MIN) {
                    newValue = adjustRtpRange(Integer.valueOf((String) newValue));
                }
                preference.setSummary(newValue.toString());
                account.setDetail(key, newValue.toString());
            }

            mAccountService.setCredentials(mAccountID, account.getCredentialsHashMapList());
            mAccountService.setAccountDetails(mAccountID, account.getDetails());
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() == null || getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY) == null) {
            return;
        }
        mAccountID = getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY);
        Account account = mAccountService.getAccount(mAccountID);
        if (account != null) {
            setPreferenceDetails(account.getConfig());
            setPreferenceListener(account.getConfig(), changeAdvancedPreferenceListener);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);
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
        for (ConfigKey confKey : details.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref != null) {
                if (confKey == ConfigKey.LOCAL_INTERFACE) {
                    String val = details.get(confKey);
                    ArrayList<CharSequence> entries = getNetworkInterfaces();
                    CharSequence[] display = entries.toArray(new CharSequence[entries.size()]);
                    ListPreference listPref = (ListPreference) pref;
                    listPref.setEntries(display);
                    listPref.setEntryValues(display);
                    listPref.setSummary(val);
                    listPref.setValue(val);
                } else if (!confKey.isTwoState()) {
                    String val = details.get(confKey);
                    pref.setSummary(val);
                    if (pref instanceof EditTextPreference) {
                        ((EditTextPreference) pref).setText(val);
                    }
                } else {
                    ((TwoStatePreference) pref).setChecked(details.getBool(confKey));
                }
            }
        }
    }

    private void setPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey confKey : details.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            }
        }
    }

    private ArrayList<CharSequence> getNetworkInterfaces() {
        ArrayList<CharSequence> result = new ArrayList<>();
        result.add("default");
        try {

            for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements(); ) {
                NetworkInterface i = list.nextElement();
                if (i.isUp()) {
                    result.add(i.getDisplayName());
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error enumerating interfaces: ", e);
        }
        return result;
    }

    private String adjustRtpRange(int newValue) {
        if (newValue < 1024) {
            return "1024";
        }

        if (newValue > 65534) {
            return "65534";
        }

        return String.valueOf(newValue);
    }

}
