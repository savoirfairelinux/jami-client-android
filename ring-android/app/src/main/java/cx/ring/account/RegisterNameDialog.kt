/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.account

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragRegisterNameBinding
import cx.ring.utils.RegisteredNameFilter
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.account.JamiAccountCreationPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.RegisteredName
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RegisterNameDialog : DialogFragment() {
    @Inject
    lateinit var mAccountService: AccountService

    private var mListener: RegisterNameDialogListener? = null
    private val contactQuery = PublishSubject.create<String>()
    private var mDisposableListener: Disposable? = null
    private var binding: FragRegisterNameBinding? = null

    fun setListener(l: RegisterNameDialogListener) {
        mListener = l
    }

    private fun onLookupResult(name: String, address: String?, state: Int) {
        binding?.let { binding ->
            val actualName = binding.ringUsername.text?.toString() ?: ""
            if (actualName.isEmpty()) {
                binding.ringUsernameTxtBox.isErrorEnabled = false
                binding.ringUsernameTxtBox.error = null
                return
            }
            if (name != actualName)
                return
            when (state) {
                0 -> {
                    // on found
                    binding.ringUsernameTxtBox.isErrorEnabled = true
                    binding.ringUsernameTxtBox.error = getText(R.string.username_already_taken)
                }
                1 -> {
                    // invalid name
                    binding.ringUsernameTxtBox.isErrorEnabled = true
                    binding.ringUsernameTxtBox.error = getText(R.string.invalid_username)
                }
                else -> {
                    // on error
                    binding.ringUsernameTxtBox.isErrorEnabled = false
                    binding.ringUsernameTxtBox.error = null
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragRegisterNameBinding.inflate(layoutInflater).also { this.binding = it }
        val view: View = binding.root
        var hasPassword = true
        val args = arguments
        if (args != null) {
            hasPassword = args.getBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, true)
        }

        binding.ringUsername.filters = arrayOf<InputFilter>(RegisteredNameFilter())
        binding.ringUsername.addTextChangedListener(object: TextWatcher {
            val mLookingForAvailability = getString(R.string.looking_for_username_availability)

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.ringUsername.apply { error = null }
            }

            override fun afterTextChanged(txt: Editable) {
                val name = txt.toString()
                if (name.isBlank()) {
                    binding.ringUsernameTxtBox.isErrorEnabled = false
                    binding.ringUsernameTxtBox.error = null
                } else {
                    binding.ringUsernameTxtBox.isErrorEnabled = true
                    binding.ringUsernameTxtBox.error = mLookingForAvailability
                    contactQuery.onNext(name.trim())
                }
            }
        })
        // binding.ringUsername.setOnEditorActionListener((v, actionId, event) -> RegisterNameDialog.this.onEditorAction(v, actionId));
        binding.passwordTxtBox.visibility = if (hasPassword) View.VISIBLE else View.GONE
        binding.passwordTxt.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
            onEditorAction(v, actionId)
        }
        val dialog = dialog as AlertDialog?
        if (dialog != null) {
            dialog.setView(view)
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        val result = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setMessage(R.string.register_username)
            .setTitle(R.string.register_name)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel) { d: DialogInterface?, b: Int -> dismiss() }
            .create()
        result.setOnShowListener { d: DialogInterface ->
            val positiveButton = (d as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validate()) {
                    dismiss()
                }
            }
        }
        return result
    }

    override fun onStart() {
        val accountId = arguments?.getString(AccountEditionFragment.ACCOUNT_ID_KEY) ?: ""
        mDisposableListener = contactQuery
            .debounce(JamiAccountCreationPresenter.TYPING_DELAY, TimeUnit.MILLISECONDS)
            .switchMapSingle { q: String -> mAccountService.findRegistrationByName(accountId, "", q) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { q: RegisteredName -> onLookupResult(q.name, q.address, q.state) }
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        mDisposableListener?.dispose()
        mDisposableListener = null
    }

    private val isValidUsername: Boolean
        get() = binding!!.ringUsernameTxtBox.error == null

    private fun checkInput(): Boolean {
        binding?.let { binding ->
            if (binding.ringUsername.text == null || binding.ringUsername.text!!.isEmpty()) {
                binding.ringUsernameTxtBox.isErrorEnabled = true
                binding.ringUsernameTxtBox.error = getText(R.string.prompt_new_username)
                return false
            }
            if (!isValidUsername) {
                binding.ringUsername.requestFocus()
                return false
            }
            binding.ringUsernameTxtBox.isErrorEnabled = false
            binding.ringUsernameTxtBox.error = null
            if (binding.passwordTxtBox.visibility == View.VISIBLE) {
                if (binding.passwordTxt.text == null || binding.passwordTxt.text!!.isEmpty()) {
                    binding.passwordTxtBox.isErrorEnabled = true
                    binding.passwordTxtBox.error = getString(R.string.prompt_password)
                    return false
                } else {
                    binding.passwordTxtBox.isErrorEnabled = false
                    binding.passwordTxtBox.error = null
                }
            }
        }
        return true
    }

    private fun validate(): Boolean {
        if (checkInput() && mListener != null) {
            val username = binding!!.ringUsername.text!!.toString()
            val password = binding!!.passwordTxt.text!!.toString()
            mListener!!.onRegisterName(username, password)
            return true
        }
        return false
    }

    private fun onEditorAction(v: TextView, actionId: Int): Boolean {
        if (v === binding?.passwordTxt) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val validationResult = validate()
                if (validationResult) {
                    dialog?.dismiss()
                }
                return validationResult
            }
        }
        return false
    }

    interface RegisterNameDialogListener {
        fun onRegisterName(name: String, password: String?)
    }
}