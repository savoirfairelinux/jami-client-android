/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            if (preference.key == PREF_DELETE_ACCOUNT) {
                showDeleteAccountDialog()
                return true
            }
            val key = ConfigKey.fromString(preference.key)
            if (key != null && key.isBool)
                presenter.twoStatePreferenceChanged(key, (preference as SwitchPreference).isChecked)
            return super.onPreferenceTreeClick(preference)
        }

        private fun showDeleteAccountDialog() {
            val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
                .setTitle(R.string.account_delete_dialog_title)
                .setMessage(R.string.account_delete_dialog_message)
                .setPositiveButton(R.string.menu_delete) { _, _ -> deleteAccount() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                }
            }
            dialog.show()
        }

        private fun deleteAccount() {
            presenter.removeAccount()
            val activity = requireActivity()
            Log.d("pavan", "settings calling dispatycher")
            activity.onBackPressedDispatcher.onBackPressed()
            activity.finish()
        }

        companion object {
            private const val PREF_DELETE_ACCOUNT = "Account.delete"
            fun newInstance(): PrefsFragment = PrefsFragment()
        }
    }
}