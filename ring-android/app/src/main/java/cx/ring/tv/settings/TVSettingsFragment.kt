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
package cx.ring.tv.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.*
import cx.ring.R
import cx.ring.services.SharedPreferencesServiceImpl
import cx.ring.tv.account.JamiPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Account
import net.jami.model.ConfigKey
import net.jami.settings.GeneralAccountPresenter
import net.jami.settings.GeneralAccountView
import java.util.*

@AndroidEntryPoint
class TVSettingsFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment.newInstance())
    }

    override fun onPreferenceStartFragment(preferenceFragment: PreferenceFragmentCompat, preference: Preference): Boolean {
        val args = preference.extras
        val f = childFragmentManager.fragmentFactory.instantiate(requireActivity().classLoader, preference.fragment)
        f.arguments = args
        f.setTargetFragment(preferenceFragment, 0)
        if (f is PreferenceFragmentCompat
            || f is PreferenceDialogFragmentCompat
        ) {
            startPreferenceFragment(f)
        } else {
            startImmersiveFragment(f)
        }
        return true
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        val prefsFragment: Fragment = PrefsFragment.newInstance()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        prefsFragment.arguments = args
        startPreferenceFragment(prefsFragment)
        return true
    }

    @AndroidEntryPoint
    class PrefsFragment : JamiPreferenceFragment<GeneralAccountPresenter>(), GeneralAccountView {
        private var autoAnswer = false
        private var rendezvousMode = false
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            presenter.init()
        }

        override fun addJamiPreferences(accountId: String) {}
        override fun addSipPreferences() {}
        override fun accountChanged(account: Account) {
            // load information from account to ui
            autoAnswer = account.config.getBool(ConfigKey.ACCOUNT_AUTOANSWER)
            rendezvousMode = account.config.getBool(ConfigKey.ACCOUNT_ISRENDEZVOUS)
            val pref = findPreference<SwitchPreference>(ConfigKey.ACCOUNT_AUTOANSWER.key())
            if (pref != null) pref.isChecked = autoAnswer
            val prefRdv = findPreference<SwitchPreference>(ConfigKey.ACCOUNT_ISRENDEZVOUS.key())
            if (prefRdv != null) prefRdv.isChecked = rendezvousMode
        }

        override fun finish() {
            requireActivity().onBackPressed()
        }

        override fun updateResolutions(maxResolution: Pair<Int, Int>?, currentResolution: Int) {
            val videoResolutionsNames = resources.getStringArray(R.array.video_resolutionStrings)
            val videoResolutionsValues =
                filterResolutions(resources.getStringArray(R.array.video_resolutions), currentResolution, maxResolution)
            val lpVideoResolution = findPreference<ListPreference>(SharedPreferencesServiceImpl.PREF_RESOLUTION)
            if (lpVideoResolution != null) {
                lpVideoResolution.entries = videoResolutionsNames.copyOfRange(0, videoResolutionsValues.size)
                lpVideoResolution.entryValues = videoResolutionsValues
            }
        }

        override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
            val pm = preferenceManager
            pm.sharedPreferencesMode = Context.MODE_PRIVATE
            pm.sharedPreferencesName = SharedPreferencesServiceImpl.PREFS_VIDEO
            setPreferencesFromResource(R.xml.tv_account_general_pref, rootKey)
        }

        private fun filterResolutions(
            videoResolutionsValues: Array<String>,
            currentResolution: Int,
            maxResolution: Pair<Int, Int>?
        ): Array<String> {
            if (maxResolution == null) return videoResolutionsValues
            if (currentResolution > maxResolution.second) return videoResolutionsValues
            val resolutions = ArrayList<String>()
            for (videoResolutionsValue in videoResolutionsValues) {
                val resolutionValueInt = videoResolutionsValue.toInt()
                if (resolutionValueInt <= maxResolution.second) {
                    resolutions.add(videoResolutionsValue)
                }
            }
            return resolutions.toTypedArray()
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key == "Account.about") {
            } else if (preference.key == ConfigKey.ACCOUNT_AUTOANSWER.key()) {
                presenter.twoStatePreferenceChanged(ConfigKey.ACCOUNT_AUTOANSWER, !autoAnswer)
                autoAnswer = !autoAnswer
            } else if (preference.key == ConfigKey.ACCOUNT_ISRENDEZVOUS.key()) {
                presenter.twoStatePreferenceChanged(ConfigKey.ACCOUNT_ISRENDEZVOUS, !rendezvousMode)
                rendezvousMode = !rendezvousMode
            }
            return super.onPreferenceTreeClick(preference)
        }

        companion object {
            fun newInstance(): PrefsFragment {
                return PrefsFragment()
            }
        }
    }
}