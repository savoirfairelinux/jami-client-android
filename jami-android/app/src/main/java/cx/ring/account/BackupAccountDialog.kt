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
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogConfirmRevocationBinding

class BackupAccountDialog : DialogFragment() {
    private var mAccountId: String? = null
    private var mListener: UnlockAccountListener? = null
    private var binding: DialogConfirmRevocationBinding? = null
    fun setListener(listener: UnlockAccountListener?) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogConfirmRevocationBinding.inflate(requireActivity().layoutInflater)
        arguments?.let { args ->
            mAccountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY)
        }
        binding!!.passwordTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val validationResult = validate()
                if (validationResult) {
                    dialog!!.dismiss()
                }
                return@setOnEditorActionListener validationResult
            }
            false
        }
        val result = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_enter_password)
            .setMessage(R.string.account_new_device_password)
            .setView(binding!!.root)
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
        if (mListener != null) {
            mListener!!.onUnlockAccount(mAccountId!!, binding!!.passwordTxt.text.toString())
            return true
        }
        return false
    }

    interface UnlockAccountListener {
        fun onUnlockAccount(accountId: String, password: String)
    }

    companion object {
        val TAG = BackupAccountDialog::class.simpleName!!
    }
}