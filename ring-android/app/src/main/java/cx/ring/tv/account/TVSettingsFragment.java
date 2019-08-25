/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.account;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.GeneralAccountPresenter;
import cx.ring.fragments.GeneralAccountView;
import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.services.SharedPreferencesServiceImpl;

public class TVSettingsFragment extends LeanbackSettingsFragmentCompat {

    @Override
    public void onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment.newInstance());
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat preferenceFragment, Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        final Fragment prefsFragment = PrefsFragment.newInstance();
        final Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        prefsFragment.setArguments(args);
        startPreferenceFragment(prefsFragment);
        return true;
    }

    public static class PrefsFragment extends RingPreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {
        private boolean autoAnswer;

        public static PrefsFragment newInstance() {
            return new PrefsFragment();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
            super.onViewCreated(view, savedInstanceState);
            presenter.init();
        }

        @Override
        public void addRingPreferences() {

        }

        @Override
        public void addSIPPreferences() {

        }

        @Override
        public void accountChanged(Account account) {
            if (account == null) {
                Log.d(TAG, "accountChanged: Null account");
                return;
            }

            // load information from account to ui
            autoAnswer = account.getConfig().getBool(ConfigKey.ACCOUNT_AUTOANSWER);

            SwitchPreference pref = findPreference(ConfigKey.ACCOUNT_AUTOANSWER.key());
            pref.setChecked(autoAnswer);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            PreferenceManager pm = getPreferenceManager();
            pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
            pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_VIDEO);
            setPreferencesFromResource(R.xml.tv_account_general_pref, rootKey);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals(ConfigKey.ACCOUNT_AUTOANSWER.key())) {
                presenter.twoStatePreferenceChanged(ConfigKey.ACCOUNT_AUTOANSWER, !autoAnswer);
                this.autoAnswer = !autoAnswer;
            }
            return super.onPreferenceTreeClick(preference);
        }
    }
}
