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

import cx.ring.R;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountConfig;
import cx.ring.model.account.ConfigKey;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

import android.app.Activity;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;

public class GeneralAccountFragment extends PreferenceFragment implements AccountChangedListener {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";
    private static final String KEY_IS_RING = "accountIsRing";
    private AccountCallbacks mCallbacks = DUMMY_CALLBACKS;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
        mCallbacks.addOnAccountChanged(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
        if (mCallbacks != null) {
            mCallbacks.removeOnAccountChanged(this);
        }
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void accountChanged(Account acc) {
        setPreferenceDetails(acc.getConfig());
        setPreferenceListener(acc.getConfig(), changeBasicPreferenceListener);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Log.i(TAG, "onCreatePreferences " + bundle + " " + s);
        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            if (acc.isRing()) {
                addPreferencesFromResource(R.xml.account_prefs_ring);
            } else {
                addPreferencesFromResource(R.xml.account_general_prefs);
            }
            accountChanged(acc);
        } else {
            if (bundle != null) {
                Log.w(TAG, "onCreatePreferences: null account, from bundle");
                boolean isRing = bundle.getBoolean(KEY_IS_RING);
                if (isRing) {
                    addPreferencesFromResource(R.xml.account_prefs_ring);
                } else {
                    addPreferencesFromResource(R.xml.account_general_prefs);
                }
            } else {
                Log.w(TAG, "onCreatePreferences: null account");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            outState.putBoolean(KEY_IS_RING, acc.isRing());
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
            Preference pref = findPreference(k.key());
            if (pref != null) {
                if (!k.isTwoState()) {
                    String val = details.get(k);
                    ((EditTextPreference) pref).setText(val);
                    if (pref instanceof PasswordPreference) {
                        String tmp = "";
                        for (int i = 0; i < val.length(); ++i) {
                            tmp += "*";
                        }
                        pref.setSummary(tmp);
                    } else {
                        pref.setSummary(val);
                    }
                } else {
                    ((TwoStatePreference) pref).setChecked(details.getBool(k));
                }
            }
        }
    }

    private void setPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey k : details.getKeys()) {
            //Log.i(TAG, "setPreferenceListener: pref " + p.mKey + " " + p.mValue);
            Preference pref = findPreference(k.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            }
        }
    }

    private final Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            Log.i(TAG, "Changing preference " + preference.getKey() + " to value:" + newValue);
            final Account acc = mCallbacks.getAccount();
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (preference instanceof TwoStatePreference) {
                acc.setDetail(key, newValue.toString());
            } else {
                if (preference instanceof PasswordPreference) {
                    String tmp = "";
                    for (int i = 0; i < ((String) newValue).length(); ++i) {
                        tmp += "*";
                    }
                    if(acc.isSip())
                        acc.getCredentials().get(0).setDetail(key, newValue.toString());
                    preference.setSummary(tmp);
                } else if(key == ConfigKey.ACCOUNT_USERNAME) {
					if(acc.isSip()){
                        acc.getCredentials().get(0).setDetail(key, newValue.toString());
					}
                    preference.setSummary((CharSequence) newValue);
                } else {
                    preference.setSummary((CharSequence) newValue);
                }

                acc.setDetail(key, newValue.toString());
            }
            mCallbacks.saveAccount();
            return true;
        }
    };

}
