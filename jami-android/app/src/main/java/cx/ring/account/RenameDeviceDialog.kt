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
import cx.ring.databinding.DialogDeviceRenameBinding

class RenameDeviceDialog : DialogFragment() {
    private var mListener: RenameDeviceListener? = null
    private var binding: DialogDeviceRenameBinding? = null
    fun setListener(listener: RenameDeviceListener?) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDeviceRenameBinding.inflate(layoutInflater)
        binding!!.ringDeviceNameTxt.setText(requireArguments().getString(DEVICENAME_KEY))
        binding!!.ringDeviceNameTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val validationResult = validate()
                if (validationResult) {
                    requireDialog().dismiss()
                }
                return@setOnEditorActionListener validationResult
            }
            false
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding!!.root)
            .setTitle(R.string.rename_device_title)
            .setMessage(R.string.rename_device_message)
            .setPositiveButton(R.string.rename_device_button, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener { d: DialogInterface ->
                    val button = (d as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        if (validate()) {
                            d.dismiss()
                        }
                    }
                }
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
    }

    override fun onDestroy() {
        mListener = null
        super.onDestroy()
    }

    private fun checkInput(input: String): Boolean {
        if (input.isEmpty()) {
            binding?.apply {
                ringDeviceNameTxtBox.isErrorEnabled = true
                ringDeviceNameTxtBox.error = getText(R.string.account_device_name_empty)
            }
            return false
        } else {
            binding?.apply {
                ringDeviceNameTxtBox.isErrorEnabled = false
                ringDeviceNameTxtBox.error = null
            }
        }
        return true
    }

    private fun validate(): Boolean {
        val input = binding!!.ringDeviceNameTxt.text.toString().trim { it <= ' ' }
        if (checkInput(input) && mListener != null) {
            mListener!!.onDeviceRename(input)
            return true
        }
        return false
    }

    interface RenameDeviceListener {
        fun onDeviceRename(newName: String)
    }

    companion object {
        const val DEVICENAME_KEY = "devicename_key"
    }
}