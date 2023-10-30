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
import cx.ring.databinding.DialogSwarmTitleBinding

class RenameSwarmDialog : DialogFragment() {
    private var mTitle: String? = null
    private var mText: String? = null
    private var mHint: String? = null
    private var mListener: RenameSwarmListener? = null
    private var binding: DialogSwarmTitleBinding? = null
    private var key: String? = null

    fun setTitle(title: String?) {
        mTitle = title
    }

    fun setHint(hint: String?) {
        mHint = hint
    }

    fun setText(text: String?) {
        mText = text
    }

    fun setListener(listener: RenameSwarmListener?) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSwarmTitleBinding.inflate(layoutInflater)
        key = requireArguments().getString(KEY)
        binding!!.titleTxt.setText(mText)
        binding!!.titleTxtBox.hint = mHint
        binding!!.titleTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
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
            .setTitle(mTitle)
            .setPositiveButton(R.string.rename_btn, null)
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

    private fun validate(): Boolean {
        val input = binding!!.titleTxt.text.toString().trim { it <= ' ' }
        if (mListener != null) {
            mListener!!.onSwarmRename(key!!, input)
            return true
        }
        return false
    }

    interface RenameSwarmListener {
        fun onSwarmRename(key: String, newName: String)
    }

    companion object {
        const val KEY = "key"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
    }
}