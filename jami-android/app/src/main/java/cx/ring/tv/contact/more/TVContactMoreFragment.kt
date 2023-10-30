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
package cx.ring.tv.contact.more

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.tv.account.JamiPreferenceFragment
import cx.ring.utils.ConversationPath.Companion.fromIntent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TVContactMoreFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment.newInstance())
    }

    override fun onPreferenceStartFragment(preferenceFragment: PreferenceFragmentCompat, preference: Preference): Boolean {
        return false
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        return false
    }

    @AndroidEntryPoint
    class PrefsFragment : JamiPreferenceFragment<TVContactMorePresenter>(), TVContactMoreView {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.tv_contact_more_pref, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            presenter.setContact(fromIntent(requireActivity().intent)!!)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key == "Contact.clear") {
                createDialog(
                    getString(R.string.conversation_action_history_clear_title),
                    getString(R.string.clear_history)
                ) { dialog: DialogInterface?, whichButton: Int -> presenter.clearHistory() }
            } else if (preference.key == "Contact.delete") {
                createDialog(
                    getString(R.string.conversation_action_remove_this_title),
                    getString(R.string.menu_delete)
                ) { dialog: DialogInterface?, whichButton: Int -> presenter.removeContact() }
            }
            return super.onPreferenceTreeClick(preference)
        }

        private fun createDialog(
            title: String,
            buttonText: String,
            onClickListener: DialogInterface.OnClickListener
        ) {
            val alertDialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
                .setTitle(title)
                .setMessage("")
                .setPositiveButton(buttonText, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            alertDialog.window!!.setLayout(DIALOG_WIDTH, DIALOG_HEIGHT)
            alertDialog.setOwnerActivity(requireActivity())
            alertDialog.setOnShowListener {
                val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positive.isFocusable = true
                positive.isFocusableInTouchMode = true
                positive.requestFocus()
            }
            alertDialog.show()
        }

        override fun finishView(finishParent: Boolean) {
            val activity: Activity? = activity
            if (activity != null) {
                activity.setResult(if (finishParent) DELETE else CLEAR)
                activity.finish()
            }
        }

        companion object {
            fun newInstance(): PrefsFragment {
                return PrefsFragment()
            }
        }
    }

    companion object {
        const val CLEAR = 101
        const val DELETE = 102
        private const val DIALOG_WIDTH = 900
        private const val DIALOG_HEIGHT = 400
    }
}