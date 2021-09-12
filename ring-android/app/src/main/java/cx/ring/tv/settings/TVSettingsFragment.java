/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.tv.settings;

import android.content.Context;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Arrays;

import cx.ring.R;
import net.jami.settings.GeneralAccountPresenter;
import net.jami.settings.GeneralAccountView;
import net.jami.model.Account;
import net.jami.model.ConfigKey;
import net.jami.utils.Tuple;
import dagger.hilt.android.AndroidEntryPoint;

import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.tv.account.JamiPreferenceFragment;

@AndroidEntryPoint
public class TVSettingsFragment extends LeanbackSettingsFragmentCompat {

    @Override
    public void onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment.newInstance());
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat preferenceFragment, Preference preference) {
        final Bundle args = preference.getExtras();
        final Fragment f = getChildFragmentManager().getFragmentFactory().instantiate(requireActivity().getClassLoader(), preference.getFragment());
        f.setArguments(args);
        f.setTargetFragment(preferenceFragment, 0);
        if (f instanceof PreferenceFragmentCompat
                || f instanceof PreferenceDialogFragmentCompat) {
            startPreferenceFragment(f);
        } else {
            startImmersiveFragment(f);
        }
        return true;
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

    @AndroidEntryPoint
    public static class PrefsFragment extends JamiPreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {
        private boolean autoAnswer;
        private boolean rendezvousMode;

        public static PrefsFragment newInstance() {
            return new PrefsFragment();
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            presenter.init();
        }

        @Override
        public void addJamiPreferences(@NonNull String accountId) {

        }

        @Override
        public void addSipPreferences() {

        }

        @Override
        public void accountChanged(@NonNull Account account) {
            // load information from account to ui
            autoAnswer = account.getConfig().getBool(ConfigKey.ACCOUNT_AUTOANSWER);
            rendezvousMode = account.getConfig().getBool(ConfigKey.ACCOUNT_ISRENDEZVOUS);

            SwitchPreference pref = findPreference(ConfigKey.ACCOUNT_AUTOANSWER.key());
            if (pref != null)
                pref.setChecked(autoAnswer);

            SwitchPreference prefRdv = findPreference(ConfigKey.ACCOUNT_ISRENDEZVOUS.key());
            if (prefRdv != null)
                prefRdv.setChecked(rendezvousMode);
        }

        @Override
        public void finish() {
            requireActivity().onBackPressed();
        }

        @Override
        public void updateResolutions(Tuple<Integer, Integer> maxResolution, int currentResolution) {
            String[] videoResolutionsNames = getResources().getStringArray(R.array.video_resolutionStrings);
            String[] videoResolutionsValues = filterResolutions(getResources().getStringArray(R.array.video_resolutions), currentResolution, maxResolution);

            ListPreference lpVideoResolution = findPreference(SharedPreferencesServiceImpl.PREF_RESOLUTION);
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
            if (preference.getKey().equals("Account.about")) {
            } else if (preference.getKey().equals(ConfigKey.ACCOUNT_AUTOANSWER.key())) {
                presenter.twoStatePreferenceChanged(ConfigKey.ACCOUNT_AUTOANSWER, !autoAnswer);
                autoAnswer = !autoAnswer;
            } else if (preference.getKey().equals(ConfigKey.ACCOUNT_ISRENDEZVOUS.key())) {
                presenter.twoStatePreferenceChanged(ConfigKey.ACCOUNT_ISRENDEZVOUS, !rendezvousMode);
                rendezvousMode = !rendezvousMode;
            }
            return super.onPreferenceTreeClick(preference);
        }

    }
}
