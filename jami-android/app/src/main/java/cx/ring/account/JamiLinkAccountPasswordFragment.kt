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
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import cx.ring.R
import cx.ring.databinding.FragAccJamiLinkPasswordBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiLinkAccountPresenter
import net.jami.account.JamiLinkAccountView

@AndroidEntryPoint
class JamiLinkAccountPasswordFragment : BaseSupportFragment<JamiLinkAccountPresenter, JamiLinkAccountView>(),
    JamiLinkAccountView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiLinkPasswordBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccJamiLinkPasswordBinding.inflate(inflater, container, false).apply {
            linkButton.setOnClickListener { presenter.linkClicked() }
            ringAddPin.setOnEditorActionListener { _, actionId: Int, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    presenter.linkClicked()
                }
                false
            }
            ringAddPin.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.pinChanged(s.toString())
                }
            })
            ringExistingPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.passwordChanged(s.toString())
                }
            })
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun initPresenter(presenter: JamiLinkAccountPresenter) {
        presenter.init(model.model)
    }

    override fun enableLinkButton(enable: Boolean) {
        binding!!.linkButton.isEnabled = enable
    }

    override fun showPin(show: Boolean) {
        val binding = binding ?: return
        binding.pinBox.visibility = if (show) View.VISIBLE else View.GONE
        binding.pinHelpMessage.visibility = if (show) View.VISIBLE else View.GONE
        binding.linkButton.setText(if (show) R.string.account_link_device else R.string.account_link_archive_button)
    }

    override fun createAccount() {
        (activity as AccountWizardActivity?)?.createAccount()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding!!.ringExistingPassword.windowToken, 0)
    }

    override fun cancel() {
        activity?.onBackPressed()
    }

    companion object {
        val TAG = JamiLinkAccountPasswordFragment::class.simpleName!!
    }
}