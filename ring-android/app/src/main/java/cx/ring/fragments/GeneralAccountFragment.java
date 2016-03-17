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
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.model.account.Account;
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

public class GeneralAccountFragment extends PreferenceFragment {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    private Callbacks mCallbacks = sDummyCallbacks;
    private static final Callbacks sDummyCallbacks = new Callbacks() {
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
        Account acc = mCallbacks.getAccount();
        if (acc != null) {
            if (acc.isRing()) {
                addPreferencesFromResource(R.xml.account_prefs_ring);
            } else {
                addPreferencesFromResource(R.xml.account_general_prefs);
            }
            setPreferenceDetails(acc.getBasicDetails());
            addPreferenceListener(acc.getBasicDetails(), changeBasicPreferenceListener);
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

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    ((EditTextPreference) pref).setText(p.mValue);
                    if (pref instanceof PasswordPreference) {
                        String tmp = "";
                        for (int i = 0; i < p.mValue.length(); ++i) {
                            tmp += "*";
                        }
                        pref.setSummary(tmp);
                    } else {
                        pref.setSummary(p.mValue);
                    }
                } else {
                    ((TwoStatePreference) pref).setChecked(p.isChecked());
                }
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            //Log.i(TAG, "addPreferenceListener: pref " + p.mKey + " " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
            } /*else {
                Log.w(TAG, "addPreferenceListener: pref not found");
            }*/
        }
    }

    Preference.OnPreferenceChangeListener changeBasicPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            Log.i(TAG, "Changing preference " + preference.getKey() + " to value:" + newValue);
            final Account acc = mCallbacks.getAccount();
            if (preference instanceof TwoStatePreference) {
                acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
            } else {
                if (preference instanceof PasswordPreference) {
                    String tmp = "";
                    for (int i = 0; i < ((String) newValue).length(); ++i) {
                        tmp += "*";
                    }
                    if(acc.isSip())
                        acc.getCredentials().get(0).setDetailString(preference.getKey(), newValue.toString());
                    preference.setSummary(tmp);
                } else if(preference.getKey().contentEquals(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME)) {
					if(acc.isSip()){
                        acc.getCredentials().get(0).setDetailString(preference.getKey(), newValue.toString());
					}
                    preference.setSummary((CharSequence) newValue);
                } else {
                    preference.setSummary((CharSequence) newValue);
                }

                acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
            }
            acc.notifyObservers();
            return true;
        }
    };

}
