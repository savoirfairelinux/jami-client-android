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
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.JamiApplication;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.views.EditTextIntegerPreference;
import cx.ring.views.EditTextPreferenceDialog;
import cx.ring.views.PasswordPreference;

public class AdvancedAccountFragment extends BasePreferenceFragment<AdvancedAccountPresenter> implements AdvancedAccountView, Preference.OnPreferenceChangeListener {

    private static final String DIALOG_FRAGMENT_TAG = "android.support.v14.preference.PreferenceFragment.DIALOG";

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreatePreferences(bundle, s);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);

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

    @Override
    public void initView(AccountConfig config, ArrayList<CharSequence> networkInterfaces) {
        for (ConfigKey confKey : config.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
                if (confKey == ConfigKey.LOCAL_INTERFACE) {
                    String val = config.get(confKey);
                    CharSequence[] display = networkInterfaces.toArray(new CharSequence[networkInterfaces.size()]);
                    ListPreference listPref = (ListPreference) pref;
                    listPref.setEntries(display);
                    listPref.setEntryValues(display);
                    listPref.setSummary(val);
                    listPref.setValue(val);
                } else if (!confKey.isTwoState()) {
                    String val = config.get(confKey);
                    pref.setSummary(val);
                    if (pref instanceof EditTextPreference) {
                        ((EditTextPreference) pref).setText(val);
                    }
                } else {
                    ((TwoStatePreference) pref).setChecked(config.getBool(confKey));
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ConfigKey key = ConfigKey.fromString(preference.getKey());

        presenter.preferenceChanged(key, newValue);


        if (preference instanceof TwoStatePreference) {
            presenter.twoStatePreferenceChanged(key, newValue);
        } else if (preference instanceof PasswordPreference) {
            presenter.passwordPreferenceChanged(key, newValue);
            preference.setSummary(TextUtils.isEmpty(newValue.toString()) ? "" : "******");
        } else {
            presenter.preferenceChanged(key, newValue);
            preference.setSummary(newValue.toString());
        }
        return true;
    }
}
