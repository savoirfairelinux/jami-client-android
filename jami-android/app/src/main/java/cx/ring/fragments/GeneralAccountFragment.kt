/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.interfaces.AppBarStateListener
import cx.ring.mvp.BasePreferenceFragment
import cx.ring.services.SharedPreferencesServiceImpl
import cx.ring.views.EditTextIntegerPreference
import cx.ring.views.EditTextPreferenceDialog
import cx.ring.views.PasswordPreference
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.ConfigKey.Companion.fromString
import net.jami.settings.GeneralAccountPresenter
import net.jami.settings.GeneralAccountView

@AndroidEntryPoint
class GeneralAccountFragment : BasePreferenceFragment<GeneralAccountPresenter>(), GeneralAccountView {
    private val changeAccountStatusListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
        presenter.setEnabled(newValue as Boolean)
        false
    }
    private val changeBasicPreferenceListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
        Log.i(TAG, "Changing preference ${preference.key} type ${preference.javaClass.simpleName} to value:$newValue")
        val key = fromString(preference.key) ?: return@OnPreferenceChangeListener false
        if (preference is TwoStatePreference) {
            presenter.twoStatePreferenceChanged(key, newValue)
        } else if (preference is PasswordPreference) {
            preference.setSummary((newValue as CharSequence).map { "•" }.joinToString(""))
            presenter.passwordPreferenceChanged(key, newValue)
        } else if (key === ConfigKey.ACCOUNT_USERNAME) {
            presenter.userNameChanged(key, newValue)
            preference.summary = newValue as CharSequence
        } else {
            preference.summary = newValue as CharSequence
            presenter.preferenceChanged(key, newValue)
        }
        true
    }

    override fun accountChanged(account: Account) {
        val pm = preferenceManager
        pm.sharedPreferencesMode = Context.MODE_PRIVATE
        pm.sharedPreferencesName = SharedPreferencesServiceImpl.PREFS_ACCOUNT + account.accountId
        setPreferenceDetails(account.config)
        val pref = findPreference<SwitchPreference>("Account.status")
        if (account.isSip && pref != null) {
            pref.title = account.alias
            val status: String = getString(if (account.isEnabled) {
                when {
                    account.isTrying -> R.string.account_status_connecting
                    account.needsMigration() -> R.string.account_update_needed
                    account.isInError -> R.string.account_status_connection_error
                    account.isRegistered -> R.string.account_status_online
                    else -> R.string.account_status_unknown
                }
            } else {
                R.string.account_status_offline
            })
            pref.summary = status
            pref.isChecked = account.isEnabled

            // An ip2ip account is always ready
            pref.isEnabled = !account.isIP2IP
            pref.onPreferenceChangeListener = changeAccountStatusListener
        }
        setPreferenceListener(account.config, changeBasicPreferenceListener)
    }

    override fun finish() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun updateResolutions(maxResolution: Pair<Int, Int>?, currentResolution: Int) {}
    private fun getFileSizeSummary(size: Int, maxSize: Int): CharSequence {
        return if (size == 0) {
            getText(R.string.account_accept_files_never)
        } else if (size == maxSize) {
            getText(R.string.account_accept_files_always)
        } else {
            Formatter.formatFileSize(requireContext(), size * 1000L * 1000L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (parentFragment as? AppBarStateListener)?.onAppBarScrollTargetViewChanged(listView)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val args = arguments
        presenter.init(args?.getString(AccountEditionFragment.ACCOUNT_ID_KEY))
        val filePref = findPreference<SeekBarPreference>("acceptIncomingFilesMaxSize")
        if (filePref != null) {
            filePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { p: Preference, v: Any ->
                val pref = p as SeekBarPreference
                p.setSummary(getFileSizeSummary(v as Int, pref.max))
                true
            }
            filePref.summary = getFileSizeSummary(filePref.value, filePref.max)
        }
        val deletePref = findPreference<Preference>("Account.delete")
        if (deletePref != null) {
            deletePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val deleteDialog = createDeleteDialog()
                deleteDialog.show()
                false
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }
        if (preference is EditTextIntegerPreference) {
            val f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_NUMBER)
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

    private fun setPreferenceDetails(details: AccountConfig) {
        for (confKey in details.keys) {
            val pref = findPreference<Preference>(confKey.key) ?: continue
            if (!confKey.isBool) {
                val value = details[confKey]
                (pref as EditTextPreference).text = value
                if (pref is PasswordPreference) {
                    pref.setSummary(value.map { "•" }.joinToString(""))
                } else {
                    pref.setSummary(value)
                }
            } else {
                (pref as TwoStatePreference).isChecked = details.getBool(confKey)
            }
        }
    }

    private fun setPreferenceListener(details: AccountConfig, listener: Preference.OnPreferenceChangeListener) {
        for (confKey in details.keys) {
            val pref = findPreference<Preference>(confKey.key)
            if (pref != null) {
                pref.onPreferenceChangeListener = listener
            }
        }
    }

    override fun addJamiPreferences(accountId: String) {
        val pm = preferenceManager
        pm.sharedPreferencesMode = Context.MODE_PRIVATE
        pm.sharedPreferencesName = SharedPreferencesServiceImpl.PREFS_ACCOUNT + accountId
        addPreferencesFromResource(R.xml.account_prefs_jami)
    }

    override fun addSipPreferences() {
        addPreferencesFromResource(R.xml.account_general_prefs)
    }

    private fun createDeleteDialog(): AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.account_delete_dialog_message)
            .setTitle(R.string.account_delete_dialog_title)
            .setPositiveButton(R.string.menu_delete) { dialog: DialogInterface?, whichButton: Int -> presenter.removeAccount() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        val activity: Activity? = activity
        if (activity != null) alertDialog.setOwnerActivity(activity)
        return alertDialog
    }

    companion object {
        val TAG = GeneralAccountFragment::class.simpleName!!
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
        fun newInstance(accountId: String) = GeneralAccountFragment().apply {
            arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
        }
    }
}