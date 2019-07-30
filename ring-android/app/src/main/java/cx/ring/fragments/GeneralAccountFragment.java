/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;
import android.view.inputmethod.EditorInfo;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.utils.Log;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

public class GeneralAccountFragment extends BasePreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {

    private static final String TAG = GeneralAccountFragment.class.getSimpleName();
    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";
    private final Preference.OnPreferenceChangeListener changeAccountStatusListener = (preference, newValue) -> {
        presenter.setEnabled(newValue);
        return false;
    };
    private final Preference.OnPreferenceChangeListener changeBasicPreferenceListener = (preference, newValue) -> {
        Log.i(TAG, "Changing preference " + preference.getKey() + " to value:" + newValue);
        final ConfigKey key = ConfigKey.fromString(preference.getKey());
        if (preference instanceof TwoStatePreference) {
            presenter.twoStatePreferenceChanged(key, newValue);
        } else if (preference instanceof PasswordPreference) {
            StringBuilder tmp = new StringBuilder();
            for (int i = 0; i < ((String) newValue).length(); ++i) {
                tmp.append("*");
            }
            preference.setSummary(tmp.toString());
            presenter.passwordPreferenceChanged(key, newValue);
        } else if (key == ConfigKey.ACCOUNT_USERNAME) {
            presenter.userNameChanged(key, newValue);
            preference.setSummary((CharSequence) newValue);
        } else {
            preference.setSummary((CharSequence) newValue);
            presenter.preferenceChanged(key, newValue);
        }
        return true;
    };

    public static GeneralAccountFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionActivity.ACCOUNT_ID_KEY, accountId);
        GeneralAccountFragment generalAccountFragment = new GeneralAccountFragment();
        generalAccountFragment.setArguments(bundle);
        return generalAccountFragment;
    }

    @Override
    public void accountChanged(Account account) {
        if (account == null) {
            Log.d(TAG, "accountChanged: Null account");
            return;
        }

        setPreferenceDetails(account.getConfig());

        SwitchPreference pref = (SwitchPreference) findPreference("Account.status");
        if (account.isSip() && pref != null) {
            String status;
            pref.setTitle(account.getAlias());
            if (account.isEnabled()) {
                if (account.isTrying()) {
                    status = getString(R.string.account_status_connecting);
                } else if (account.needsMigration()) {
                    status = getString(R.string.account_update_needed);
                } else if (account.isInError()) {
                    status = getString(R.string.account_status_connection_error);
                } else if (account.isRegistered()) {
                    status = getString(R.string.account_status_online);
                } else {
                    status = getString(R.string.account_status_unknown);
                }
            } else {
                status = getString(R.string.account_status_offline);
            }
            pref.setSummary(status);
            pref.setChecked(account.isEnabled());

            // An ip2ip account is always ready
            pref.setEnabled(!account.isIP2IP());

            pref.setOnPreferenceChangeListener(changeAccountStatusListener);
        }

        setPreferenceListener(account.getConfig(), changeBasicPreferenceListener);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreatePreferences(bundle, rootKey);

        presenter.init(getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));
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
            if (pref == null) {
                continue;
            }
            if (!confKey.isTwoState()) {
                String val = details.get(confKey);
                ((EditTextPreference) pref).setText(val);
                if (pref instanceof PasswordPreference) {
                    StringBuilder tmp = new StringBuilder();
                    for (int i = 0; i < val.length(); ++i) {
                        tmp.append("*");
                    }
                    pref.setSummary(tmp.toString());
                } else {
                    pref.setSummary(val);
                }
            } else {
                ((TwoStatePreference) pref).setChecked(details.getBool(confKey));
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

    @Override
    public void addRingPreferences() {
        addPreferencesFromResource(R.xml.account_prefs_ring);
    }

    @Override
    public void addSIPPreferences() {
        addPreferencesFromResource(R.xml.account_general_prefs);
    }
}
