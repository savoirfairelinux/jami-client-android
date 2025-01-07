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
package cx.ring.account

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogConfirmRevocationBinding
import net.jami.services.AccountService

class AccountPasswordDialog : DialogFragment() {
    private var accountId: String = ""
    private var authReason: String? = null
    private var binding: DialogConfirmRevocationBinding? = null
    var listener: UnlockAccountListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let { args ->
            accountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY, "")
            authReason = args.getString(AUTH_REASON_KEY)
        }
        val binding = DialogConfirmRevocationBinding.inflate(requireActivity().layoutInflater).also {
            it.passwordTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val validationResult = validate()
                    if (validationResult) {
                        dismiss()
                    }
                    return@setOnEditorActionListener validationResult
                }
                false
            }
            this.binding = it
        }
        val result = MaterialAlertDialogBuilder(requireContext())
            .setTitle(authReason ?: getString(R.string.account_enter_password))
            .setMessage(R.string.account_new_device_password)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, whichButton: Int -> dismiss() }
            .create()
        result.setOnShowListener { dialog: DialogInterface ->
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validate()) {
                    dismiss()
                }
            }
        }
        result.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return result
    }

    private fun validate(): Boolean {
        listener?.let {
            it.onUnlockAccount(AccountService.ACCOUNT_SCHEME_PASSWORD, binding!!.passwordTxt.text.toString())
            return true
        }
        return false
    }

    fun interface UnlockAccountListener {
        fun onUnlockAccount(scheme: String, password: String)
    }

    companion object {
        val TAG = AccountPasswordDialog::class.simpleName!!
        const val AUTH_REASON_KEY = "auth_reason"
    }
}