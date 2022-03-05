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
import cx.ring.R
import cx.ring.databinding.FragAccJamiLinkPasswordBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiLinkAccountPresenter
import net.jami.account.JamiLinkAccountView
import net.jami.model.AccountCreationModel

@AndroidEntryPoint
class JamiLinkAccountPasswordFragment : BaseSupportFragment<JamiLinkAccountPresenter, JamiLinkAccountView>(),
    JamiLinkAccountView {
    private var model: AccountCreationModel? = null
    private var mBinding: FragAccJamiLinkPasswordBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (model == null) return null
        mBinding = FragAccJamiLinkPasswordBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding!!.linkButton.setOnClickListener { v: View? -> presenter.linkClicked() }
        mBinding!!.ringAddPin.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.linkClicked()
            }
            false
        }
        mBinding!!.ringAddPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.pinChanged(s.toString())
            }
        })
        mBinding!!.ringExistingPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.passwordChanged(s.toString())
            }
        })
    }

    override fun initPresenter(presenter: JamiLinkAccountPresenter) {
        presenter.init(model)
    }

    override fun enableLinkButton(enable: Boolean) {
        mBinding!!.linkButton.isEnabled = enable
    }

    override fun showPin(show: Boolean) {
        mBinding!!.pinBox.visibility = if (show) View.VISIBLE else View.GONE
        mBinding!!.pinHelpMessage.visibility = if (show) View.VISIBLE else View.GONE
        mBinding!!.linkButton.setText(if (show) R.string.account_link_device else R.string.account_link_archive_button)
    }

    override fun createAccount(accountCreationModel: AccountCreationModel) {
        (requireActivity() as AccountWizardActivity).createAccount(accountCreationModel)
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mBinding!!.ringExistingPassword.windowToken, 0)
    }

    override fun cancel() {
        activity?.onBackPressed()
    }

    companion object {
        val TAG = JamiLinkAccountPasswordFragment::class.simpleName!!
        fun newInstance(ringAccountViewModel: AccountCreationModel): JamiLinkAccountPasswordFragment {
            val fragment = JamiLinkAccountPasswordFragment()
            fragment.model = ringAccountViewModel
            return fragment
        }
    }
}