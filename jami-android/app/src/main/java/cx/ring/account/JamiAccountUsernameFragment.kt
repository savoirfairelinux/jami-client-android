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
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import cx.ring.databinding.FragAccJamiUsernameBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.RegisteredNameFilter
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountCreationPresenter
import net.jami.account.JamiAccountCreationView
import net.jami.account.JamiAccountCreationView.UsernameAvailabilityStatus

@AndroidEntryPoint
class JamiAccountUsernameFragment : BaseSupportFragment<JamiAccountCreationPresenter, JamiAccountCreationView>(),
    JamiAccountCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiUsernameBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccJamiUsernameBinding.inflate(inflater, container, false).apply {
            ringUsername.filters = arrayOf<InputFilter>(RegisteredNameFilter())
            createAccount.setOnClickListener { presenter.createAccount() }
            skip.setOnClickListener {
                presenter.registerUsernameChanged(false)
                presenter.createAccount()
            }
            ringUsername.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.userNameChanged(s.toString())
                }
            })
            ringUsername.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE && binding!!.createAccount.isEnabled) {
                    val inputMethodManager =
                        requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
                    presenter.createAccount()
                    return@OnEditorActionListener true
                }
                false
            })
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.ringUsername.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding!!.ringUsername, InputMethodManager.SHOW_IMPLICIT)
        presenter.init(model.model)
        presenter.setPush(true)
    }

    override fun updateUsernameAvailability(status: UsernameAvailabilityStatus) {
        val binding = binding ?: return
        binding.ringUsernameAvailabilitySpinner.visibility = View.GONE
        when (status) {
            UsernameAvailabilityStatus.ERROR -> {
                binding.ringUsernameTxtBox.isErrorEnabled = true
                binding.ringUsernameTxtBox.error = getString(R.string.unknown_error)
                binding.ringUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_INVALID -> {
                binding.ringUsernameTxtBox.isErrorEnabled = true
                binding.ringUsernameTxtBox.error = getString(R.string.invalid_username)
                binding.ringUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN -> {
                binding.ringUsernameTxtBox.isErrorEnabled = true
                binding.ringUsernameTxtBox.error = getString(R.string.username_already_taken)
                binding.ringUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.LOADING -> {
                binding.ringUsernameTxtBox.isErrorEnabled = false
                binding.ringUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
                binding.ringUsernameAvailabilitySpinner.visibility = View.VISIBLE
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.AVAILABLE -> {
                binding.ringUsernameTxtBox.isErrorEnabled = false
                binding.ringUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_CUSTOM
                binding.ringUsernameTxtBox.setEndIconDrawable(R.drawable.ic_good_green)
                enableNextButton(true)
            }
            UsernameAvailabilityStatus.RESET -> {
                binding.ringUsernameTxtBox.isErrorEnabled = false
                binding.ringUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                enableNextButton(false)
            }
        }
    }

    override fun showInvalidPasswordError(display: Boolean) {}
    override fun showNonMatchingPasswordError(display: Boolean) {}
    override fun enableNextButton(enabled: Boolean) {
        binding!!.createAccount.isEnabled = enabled
    }

    override fun goToAccountCreation() {
        val parent = parentFragment as JamiAccountCreationFragment?
        if (parent != null) {
            parent.scrollPagerFragment()
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding!!.ringUsername.windowToken, 0)
        }
    }

    override fun cancel() {
        activity?.onBackPressed()
    }
}