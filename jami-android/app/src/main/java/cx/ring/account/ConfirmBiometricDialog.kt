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
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogConfirmRevocationBinding

class ConfirmBiometricDialog : DialogFragment() {
    private var mListener: ((String) -> Unit)? = null
    private var binding: DialogConfirmRevocationBinding? = null
    fun setListener(listener: (String) -> Unit) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogConfirmRevocationBinding.inflate(requireActivity().layoutInflater)
        val result = MaterialAlertDialogBuilder(requireContext())
            .setView(binding!!.root)
            .setTitle(R.string.account_enter_password)
            .setMessage(R.string.account_new_device_password)
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
        binding!!.passwordTxt.setOnEditorActionListener { _, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val validationResult = validate()
                if (validationResult) {
                    dialog!!.dismiss()
                }
                return@setOnEditorActionListener validationResult
            }
            false
        }
        return result
    }

    private fun checkInput(): Boolean {
        if (binding!!.passwordTxt.text.toString().isEmpty()) {
            binding!!.passwordTxtBox.isErrorEnabled = true
            binding!!.passwordTxtBox.error = getText(R.string.enter_password)
            return false
        }
        return true
    }

    private fun validate(): Boolean {
        if (checkInput() && mListener != null) {
            mListener!!(binding!!.passwordTxt.text.toString())
            return true
        }
        return false
    }

    companion object {
        val TAG = ConfirmBiometricDialog::class.simpleName!!
    }
}
