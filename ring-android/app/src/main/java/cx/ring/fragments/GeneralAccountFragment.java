/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cx.ring.R;
import cx.ring.account.AccountEditionFragment;
import cx.ring.application.JamiApplication;
import net.jami.model.Account;
import net.jami.model.AccountConfig;
import net.jami.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.services.SharedPreferencesServiceImpl;
import net.jami.utils.Log;
import net.jami.utils.Tuple;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GeneralAccountFragment extends BasePreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {

    public static final String TAG = GeneralAccountFragment.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";
    private final Preference.OnPreferenceChangeListener changeAccountStatusListener = (preference, newValue) -> {
        presenter.setEnabled((Boolean) newValue);
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
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        GeneralAccountFragment generalAccountFragment = new GeneralAccountFragment();
        generalAccountFragment.setArguments(bundle);
        return generalAccountFragment;
    }

    @Override
    public void accountChanged(@NonNull Account account) {
        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_ACCOUNT+account.getAccountID());

        setPreferenceDetails(account.getConfig());

        SwitchPreference pref = findPreference("Account.status");
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
    public void finish() {
        Activity activity = getActivity();
        if (activity != null)
            activity.onBackPressed();
    }

    @Override
    public void updateResolutions(Tuple<Integer, Integer> maxResolution, int currentResolution) {
    }

    private CharSequence getFileSizeSummary(int size, int maxSize) {
        if (size == 0)  {
            return getText(R.string.account_accept_files_never);
        } else if (size == maxSize) {
            return getText(R.string.account_accept_files_always);
        } else {
            return Formatter.formatFileSize(requireContext(), size * 1000 * 1000);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        super.onCreatePreferences(bundle, rootKey);

        Bundle args = getArguments();
        presenter.init(args == null  ? null : args.getString(AccountEditionFragment.ACCOUNT_ID_KEY));

        SeekBarPreference filePref = findPreference("acceptIncomingFilesMaxSize");
        if (filePref != null) {
            filePref.setOnPreferenceChangeListener((p, v) ->  {
                SeekBarPreference pref = (SeekBarPreference)p;
                p.setSummary(getFileSizeSummary((Integer) v, pref.getMax()));
                return true;
            });
            filePref.setSummary(getFileSizeSummary(filePref.getValue(), filePref.getMax()));
        }

        Preference deletePref = findPreference("Account.delete");
        if (deletePref != null) {
            deletePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog deleteDialog = createDeleteDialog();
                    deleteDialog.show();
                    return false;
                }
            });
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        if (preference instanceof EditTextIntegerPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_NUMBER);
            f.setTargetFragment(this, 0);
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof PasswordPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            f.setTargetFragment(this, 0);
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG);
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
    public void addJamiPreferences(String accountId) {
        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_ACCOUNT+accountId);
        addPreferencesFromResource(R.xml.account_prefs_jami);
    }

    @Override
    public void addSipPreferences() {
        addPreferencesFromResource(R.xml.account_general_prefs);
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(R.string.menu_delete, (dialog, whichButton) -> presenter.removeAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Activity activity = getActivity();
        if (activity != null)
            alertDialog.setOwnerActivity(getActivity());
        return alertDialog;
    }

}
