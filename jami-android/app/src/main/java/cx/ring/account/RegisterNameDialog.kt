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

    private fun onLookupResult(name: String, address: String?, state: AccountService.LookupState) {
        binding?.let { binding ->
            val actualName = binding.inputUsername.text?.toString() ?: ""
            if (actualName.isEmpty()) {
                binding.inputUsernameTxtBox.isErrorEnabled = false
                binding.inputUsernameTxtBox.error = null
                return
            }
            if (name != actualName)
                return
            when (state) {
                AccountService.LookupState.Success -> {
                    // on found
                    binding.inputUsernameTxtBox.isErrorEnabled = true
                    binding.inputUsernameTxtBox.error = getText(R.string.username_already_taken)
                }
                AccountService.LookupState.Invalid -> {
                    // invalid name
                    binding.inputUsernameTxtBox.isErrorEnabled = true
                    binding.inputUsernameTxtBox.error = getText(R.string.invalid_username)
                }
                else -> {
                    // on error
                    binding.inputUsernameTxtBox.isErrorEnabled = false
                    binding.inputUsernameTxtBox.error = null
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragRegisterNameBinding.inflate(layoutInflater).also { this.binding = it }
        val view: View = binding.root
        binding.inputUsername.filters = arrayOf<InputFilter>(RegisteredNameFilter())
        binding.inputUsername.addTextChangedListener(object: TextWatcher {
            val mLookingForAvailability = getString(R.string.looking_for_username_availability)

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.inputUsername.apply { error = null }
            }

            override fun afterTextChanged(txt: Editable) {
                val name = txt.toString()
                if (name.isBlank()) {
                    binding.inputUsernameTxtBox.isErrorEnabled = false
                    binding.inputUsernameTxtBox.error = null
                } else {
                    binding.inputUsernameTxtBox.isErrorEnabled = true
                    binding.inputUsernameTxtBox.error = mLookingForAvailability
                    contactQuery.onNext(name.trim())
                }
            }
        })
        // binding.inputUsername.setOnEditorActionListener((v, actionId, event) -> RegisterNameDialog.this.onEditorAction(v, actionId));
        binding.inputUsername.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
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
        get() = binding!!.inputUsernameTxtBox.error == null

    private fun checkInput(): Boolean {
        binding?.let { binding ->
            if (binding.inputUsername.text == null || binding.inputUsername.text!!.isEmpty()) {
                binding.inputUsernameTxtBox.isErrorEnabled = true
                binding.inputUsernameTxtBox.error = getText(R.string.prompt_new_username)
                return false
            }
            if (!isValidUsername) {
                binding.inputUsername.requestFocus()
                return false
            }
            binding.inputUsernameTxtBox.isErrorEnabled = false
            binding.inputUsernameTxtBox.error = null
        }
        return true
    }

    private fun validate(): Boolean {
        val binding = binding
        val listener = mListener
        if (checkInput() && listener != null && binding != null) {
            val username = binding.inputUsername.text!!.toString()
            listener.onRegisterName(username)
            return true
        }
        return false
    }

    private fun onEditorAction(v: TextView, actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val validationResult = validate()
            if (validationResult) {
                dialog?.dismiss()
            }
            return validationResult
        }
        return false
    }

    interface RegisterNameDialogListener {
        fun onRegisterName(name: String)
    }
}