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
package cx.ring.account

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogSetPasswordBinding

class ChangePasswordDialog : DialogFragment() {
    private var mListener: PasswordChangedListener? = null
    private var binding: DialogSetPasswordBinding? = null
    fun setListener(listener: PasswordChangedListener?) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val hasPassword = arguments?.getBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, true) ?: true
        val passwordMessage = if (hasPassword) R.string.account_password_change else R.string.account_password_set

        binding = DialogSetPasswordBinding.inflate(requireActivity().layoutInflater).apply {
            oldPasswordTxtBox.isVisible = hasPassword
            newPasswordRepeatTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (validate()) {
                        dialog!!.dismiss()
                        return@setOnEditorActionListener true
                    }
                }
                false
            }
        }
        val result = MaterialAlertDialogBuilder(requireContext())
            .setView(binding!!.root)
            .setMessage(R.string.help_password_choose)
            .setTitle(passwordMessage)
            .setPositiveButton(passwordMessage, null) //Set to null. We override the onclick
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
        result.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return result
    }

    private fun checkInput(): Boolean {
        if (!binding!!.newPasswordTxt.text.toString().contentEquals(binding!!.newPasswordRepeatTxt.text)) {
            binding!!.newPasswordTxtBox.isErrorEnabled = true
            binding!!.newPasswordTxtBox.error = getText(R.string.error_passwords_not_equals)
            binding!!.newPasswordRepeatTxtBox.isErrorEnabled = true
            binding!!.newPasswordRepeatTxtBox.error = getText(R.string.error_passwords_not_equals)
            return false
        } else {
            binding!!.newPasswordTxtBox.isErrorEnabled = false
            binding!!.newPasswordTxtBox.error = null
            binding!!.newPasswordRepeatTxtBox.isErrorEnabled = false
            binding!!.newPasswordRepeatTxtBox.error = null
        }
        return true
    }

    private fun validate(): Boolean {
        if (checkInput() && mListener != null) {
            val oldPassword = binding!!.oldPasswordTxt.text.toString()
            val newPassword = binding!!.newPasswordTxt.text.toString()
            mListener!!.onPasswordChanged(oldPassword, newPassword)
            return true
        }
        return false
    }

    interface PasswordChangedListener {
        fun onPasswordChanged(oldPassword: String, newPassword: String)
    }

    companion object {
        val TAG = ChangePasswordDialog::class.simpleName!!
    }
}