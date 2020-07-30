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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.fragments.GeneralAccountPresenter;
import cx.ring.fragments.GeneralAccountView;
import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.utils.Tuple;

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

    public static class PrefsFragment extends JamiPreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {
        private boolean autoAnswer;

        public static PrefsFragment newInstance() {
            return new PrefsFragment();
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            ((JamiApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
            super.onViewCreated(view, savedInstanceState);
            presenter.init();
        }

        @Override
        public void addJamiPreferences(String accountId) {

        }

        @Override
        public void addSipPreferences() {

        }

        @Override
        public void accountChanged(@NonNull Account account) {
            // load information from account to ui
            autoAnswer = account.getConfig().getBool(ConfigKey.ACCOUNT_AUTOANSWER);

            SwitchPreference pref = findPreference(ConfigKey.ACCOUNT_AUTOANSWER.key());
            if (pref != null)
                pref.setChecked(autoAnswer);
        }

        @Override
        public void finish() {
            requireActivity().onBackPressed();
        }

        @Override
        public void updateResolutions(Tuple<Integer, Integer> maxResolution) {
            String[] videoResolutionsNames = getResources().getStringArray(R.array.video_resolutionStrings);
            String[] videoResolutionsValues = getResources().getStringArray(R.array.video_resolutions);

            int currentResolution = presenter.getPreferenceService().getResolution();
            videoResolutionsValues = filterResolutions(videoResolutionsValues, currentResolution, maxResolution);

            ListPreference lpVideoResolution = findPreference("video_resolution");
            if (lpVideoResolution != null) {
                lpVideoResolution.setEntries(Arrays.copyOfRange(videoResolutionsNames, 0, videoResolutionsValues.length));
                lpVideoResolution.setEntryValues(videoResolutionsValues);
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            PreferenceManager pm = getPreferenceManager();
            pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
            pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_VIDEO);
            setPreferencesFromResource(R.xml.tv_account_general_pref, rootKey);
        }

        private String[] filterResolutions(String[] videoResolutionsValues, int currentResolution, Tuple<Integer, Integer> maxResolution) {
            if (maxResolution == null) return videoResolutionsValues;
            if (currentResolution > maxResolution.second) return videoResolutionsValues;

            ArrayList<String> resolutions = new ArrayList<>();
            for (String videoResolutionsValue : videoResolutionsValues) {
                int resolutionValueInt = Integer.parseInt(videoResolutionsValue);
                if (resolutionValueInt <= maxResolution.second) {
                    resolutions.add(videoResolutionsValue);
                }
            }

            return resolutions.toArray(new String[0]);
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
