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
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogConfirmRevocationBinding

class ConfirmRevocationDialog : DialogFragment() {
    private var mDeviceId: String? = null
    private var mHasPassword = true
    private var mListener: ConfirmRevocationListener? = null
    private var binding: DialogConfirmRevocationBinding? = null
    fun setListener(listener: ConfirmRevocationListener) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogConfirmRevocationBinding.inflate(requireActivity().layoutInflater)
        mDeviceId = requireArguments().getString(DEVICEID_KEY)
        mHasPassword = requireArguments().getBoolean(HAS_PASSWORD_KEY)
        val result = MaterialAlertDialogBuilder(requireContext())
            .setView(binding!!.root)
            .setMessage(getString(R.string.revoke_device_message, mDeviceId))
            .setTitle(getText(R.string.revoke_device_title))
            .setPositiveButton(R.string.revoke_device_title, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
        result.setOnShowListener { dialog: DialogInterface ->
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validate()) {
                    dismiss()
                }
            }
        }
        if (mHasPassword) {
            result.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        } else {
            binding!!.passwordTxtBox.visibility = View.GONE
        }
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
        if (mHasPassword && binding!!.passwordTxt.text.toString().isEmpty()) {
            binding!!.passwordTxtBox.isErrorEnabled = true
            binding!!.passwordTxtBox.error = getText(R.string.enter_password)
            return false
        } else {
            binding!!.passwordTxtBox.isErrorEnabled = false
            binding!!.passwordTxtBox.error = null
        }
        return true
    }

    private fun validate(): Boolean {
        if (checkInput() && mListener != null) {
            val password = binding!!.passwordTxt.text.toString()
            mListener!!.onConfirmRevocation(mDeviceId!!, password)
            return true
        }
        return false
    }

    interface ConfirmRevocationListener {
        fun onConfirmRevocation(deviceId: String, password: String)
    }

    companion object {
        const val DEVICEID_KEY = "deviceid_key"
        const val HAS_PASSWORD_KEY = "has_password_key"
        val TAG = ConfirmRevocationDialog::class.simpleName!!
    }
}