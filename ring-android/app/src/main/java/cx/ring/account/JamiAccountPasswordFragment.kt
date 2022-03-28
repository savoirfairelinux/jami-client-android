/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.TextView
import cx.ring.R
import cx.ring.databinding.FragAccJamiPasswordBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountCreationPresenter
import net.jami.account.JamiAccountCreationView
import net.jami.account.JamiAccountCreationView.UsernameAvailabilityStatus
import net.jami.model.AccountCreationModel

@AndroidEntryPoint
class JamiAccountPasswordFragment : BaseSupportFragment<JamiAccountCreationPresenter, JamiAccountCreationView>(),
    JamiAccountCreationView {
    private var model: AccountCreationModel? = null
    private var binding: FragAccJamiPasswordBinding? = null
    private var mIsChecked = false
    override fun onSaveInstanceState(outState: Bundle) {
        if (model != null) outState.putSerializable(KEY_MODEL, model)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null && model == null) {
            model = savedInstanceState.getSerializable(KEY_MODEL) as AccountCreationModelImpl?
        }
        return FragAccJamiPasswordBinding.inflate(inflater, container, false).apply {
            createAccount.setOnClickListener { presenter.createAccount() }
            ringPasswordSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                mIsChecked = isChecked
                if (isChecked) {
                    passwordTxtBox.visibility = View.VISIBLE
                    ringPasswordRepeatTxtBox.visibility = View.VISIBLE
                    placeholder.visibility = View.GONE
                    val password: CharSequence? = password.text
                    presenter.passwordChanged(password.toString(), ringPasswordRepeat.text!!)
                } else {
                    passwordTxtBox.visibility = View.GONE
                    ringPasswordRepeatTxtBox.visibility = View.GONE
                    placeholder.visibility = View.VISIBLE
                    presenter.passwordUnset()
                }
            }
            password.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    presenter.passwordChanged(s.toString())
                }

                override fun afterTextChanged(s: Editable) {}
            })
            ringPasswordRepeat.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    presenter.passwordConfirmChanged(s.toString())
                }

                override fun afterTextChanged(s: Editable) {}
            })
            ringPasswordRepeat.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    presenter.createAccount()
                }
                false
            }
            ringPasswordRepeat.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE && binding!!.createAccount.isEnabled) {
                    val inputMethodManager = v.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    presenter.createAccount()
                    return@setOnEditorActionListener true
                }
                false
            }
            binding = this
        }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.init(model)
    }

    override fun updateUsernameAvailability(status: UsernameAvailabilityStatus) {}

    override fun showInvalidPasswordError(display: Boolean) {
        binding!!.passwordTxtBox.error = if (display) getString(R.string.error_password_char_count) else null
    }

    override fun showNonMatchingPasswordError(display: Boolean) {
        binding!!.ringPasswordRepeatTxtBox.error = if (display) getString(R.string.error_passwords_not_equals) else null
    }

    override fun enableNextButton(enabled: Boolean) {
        binding!!.createAccount.isEnabled = if (mIsChecked) enabled else true
    }

    override fun goToAccountCreation(accountCreationModel: AccountCreationModel) {
        val wizardActivity = activity as AccountWizardActivity? ?: return
        wizardActivity.createAccount(accountCreationModel)
        val parent = parentFragment as JamiAccountCreationFragment?
        parent?.scrollPagerFragment(accountCreationModel)
        val imm = wizardActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding!!.password.windowToken, 0)
    }

    override fun cancel() {
        val wizardActivity: Activity? = activity
        wizardActivity?.onBackPressed()
    }

    fun setUsername(username: String) {
        model!!.username = username
    }

    companion object {
        private const val KEY_MODEL = "model"
        fun newInstance(ringAccountViewModel: AccountCreationModelImpl): JamiAccountPasswordFragment {
            val fragment = JamiAccountPasswordFragment()
            fragment.model = ringAccountViewModel
            return fragment
        }
    }
}