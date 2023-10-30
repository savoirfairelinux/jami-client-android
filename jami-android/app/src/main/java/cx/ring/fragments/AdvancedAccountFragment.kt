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
package cx.ring.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.mvp.BasePreferenceFragment
import cx.ring.views.EditTextIntegerPreference
import cx.ring.views.EditTextPreferenceDialog
import cx.ring.views.PasswordPreference
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.settings.AdvancedAccountPresenter
import net.jami.settings.AdvancedAccountView

@AndroidEntryPoint
class AdvancedAccountFragment : BasePreferenceFragment<AdvancedAccountPresenter>(),
    AdvancedAccountView, Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs)
        presenter.init(requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }
        if (preference is EditTextIntegerPreference) {
            val f = EditTextPreferenceDialog.newInstance(
                preference.getKey(),
                EditorInfo.TYPE_CLASS_NUMBER
            )
            f.setTargetFragment(this, 0)
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG)
        } else if (preference is PasswordPreference) {
            val f = EditTextPreferenceDialog.newInstance(
                preference.getKey(),
                EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            )
            f.setTargetFragment(this, 0)
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun initView(config: AccountConfig, networkInterfaces: ArrayList<CharSequence>) {
        for (confKey in config.keys) {
            val pref = findPreference<Preference>(confKey.key)
            if (pref != null) {
                pref.onPreferenceChangeListener = this
                if (confKey == ConfigKey.LOCAL_INTERFACE) {
                    val value = config[confKey]
                    val display = networkInterfaces.toTypedArray()
                    val listPref = pref as ListPreference
                    listPref.entries = display
                    listPref.entryValues = display
                    listPref.summary = value
                    listPref.value = value
                } else if (!confKey.isBool) {
                    val value = config[confKey]
                    pref.summary = value
                    if (pref is EditTextPreference) {
                        pref.text = value
                    }
                } else {
                    (pref as TwoStatePreference).isChecked = config.getBool(confKey)
                }
            }
        }
        val isJamiAccount = config[ConfigKey.ACCOUNT_TYPE] == AccountConfig.ACCOUNT_TYPE_JAMI
        val bootstrap = findPreference<Preference>(ConfigKey.ACCOUNT_HOSTNAME.key)
        bootstrap?.isVisible = isJamiAccount
        val sipLocalPort = findPreference<Preference>(ConfigKey.LOCAL_PORT.key)
        sipLocalPort?.isVisible = !isJamiAccount
        val sipLocalInterface = findPreference<Preference>(ConfigKey.LOCAL_INTERFACE.key)
        sipLocalInterface?.isVisible = !isJamiAccount
        val registrationExpire = findPreference<Preference>(ConfigKey.REGISTRATION_EXPIRE.key)
        registrationExpire?.isVisible = !isJamiAccount
        val publishedSameAsLocal = findPreference<Preference>(ConfigKey.PUBLISHED_SAMEAS_LOCAL.key)
        publishedSameAsLocal?.isVisible = !isJamiAccount
        val publishedPort = findPreference<Preference>(ConfigKey.PUBLISHED_PORT.key)
        publishedPort?.isVisible = !isJamiAccount
        val publishedAddress = findPreference<Preference>(ConfigKey.PUBLISHED_ADDRESS.key)
        publishedAddress?.isVisible = !isJamiAccount
        val dhtproxy = findPreference<Preference>(ConfigKey.PROXY_ENABLED.key)
        dhtproxy?.parent?.isVisible = isJamiAccount
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val key = ConfigKey.fromString(preference.key)!!
        presenter.preferenceChanged(key, newValue)
        when (preference) {
            is TwoStatePreference -> {
                presenter.twoStatePreferenceChanged(key, newValue)
            }
            is PasswordPreference -> {
                presenter.passwordPreferenceChanged(key, newValue)
                preference.setSummary(if (TextUtils.isEmpty(newValue.toString())) "" else "******")
            }
            else -> {
                presenter.preferenceChanged(key, newValue)
                preference.summary = newValue.toString()
            }
        }
        return true
    }

    companion object {
        val TAG = AdvancedAccountFragment::class.simpleName!!
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}