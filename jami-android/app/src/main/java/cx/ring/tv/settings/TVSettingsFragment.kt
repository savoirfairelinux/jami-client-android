/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.tv.settings

import android.content.Context
import android.os.Bundle
import android.view.View
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
        val f = childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, preference.fragment!!).apply {
            arguments = args
            setTargetFragment(preferenceFragment, 0)
        }
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
        startPreferenceFragment(PrefsFragment.newInstance().apply {
            arguments = Bundle().apply {
                putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
            }
        })
        return true
    }

    @AndroidEntryPoint
    class PrefsFragment : JamiPreferenceFragment<GeneralAccountPresenter>(), GeneralAccountView {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            presenter.init()
        }

        override fun addJamiPreferences(accountId: String) {}
        override fun addSipPreferences() {}
        override fun accountChanged(account: Account) {
            findPreference<SwitchPreference>(ConfigKey.ACCOUNT_AUTOANSWER.key)?.isChecked = account.config.getBool(ConfigKey.ACCOUNT_AUTOANSWER)
            findPreference<SwitchPreference>(ConfigKey.ACCOUNT_ISRENDEZVOUS.key)?.isChecked = account.config.getBool(ConfigKey.ACCOUNT_ISRENDEZVOUS)
        }

        override fun finish() {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        override fun updateResolutions(maxResolution: Pair<Int, Int>?, currentResolution: Int) {
            val videoResolutionsNames = resources.getStringArray(R.array.video_resolutionStrings)
            val videoResolutionsValues =
                filterResolutions(resources.getStringArray(R.array.video_resolutions), currentResolution, maxResolution)
            findPreference<ListPreference>(SharedPreferencesServiceImpl.PREF_RESOLUTION)?.apply {
                entries = videoResolutionsNames.copyOfRange(0, videoResolutionsValues.size)
                entryValues = videoResolutionsValues
            }
        }

        override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
            preferenceManager?.apply {
                sharedPreferencesMode = Context.MODE_PRIVATE
                sharedPreferencesName = SharedPreferencesServiceImpl.PREFS_VIDEO
            }
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
                if (videoResolutionsValue.toInt() <= maxResolution.second)
                    resolutions.add(videoResolutionsValue)
            }
            return resolutions.toTypedArray()
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val key = ConfigKey.fromString(preference.key)
            if (key != null && key.isBool)
                presenter.twoStatePreferenceChanged(key, (preference as SwitchPreference).isChecked)
            return super.onPreferenceTreeClick(preference)
        }

        companion object {
            fun newInstance(): PrefsFragment = PrefsFragment()
        }
    }
}