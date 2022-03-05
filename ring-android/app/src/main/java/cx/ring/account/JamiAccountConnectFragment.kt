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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import cx.ring.databinding.FragAccJamiConnectBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountConnectPresenter
import net.jami.account.JamiConnectAccountView
import net.jami.model.AccountCreationModel

@AndroidEntryPoint
class JamiAccountConnectFragment : BaseSupportFragment<JamiAccountConnectPresenter, JamiConnectAccountView>(),
    JamiConnectAccountView {
    private var model: AccountCreationModel? = null
    private var mBinding: FragAccJamiConnectBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = FragAccJamiConnectBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun initPresenter(presenter: JamiAccountConnectPresenter) {
        presenter.init(model)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding!!.connectButton.setOnClickListener { presenter.connectClicked() }
        mBinding!!.usernameTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.usernameChanged(s.toString())
            }
        })
        mBinding!!.passwordTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.passwordChanged(s.toString())
            }
        })
        mBinding!!.promptServer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.serverChanged(s.toString())
            }
        })
        mBinding!!.passwordTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.connectClicked()
            }
            false
        }
    }

    override fun enableConnectButton(enable: Boolean) {
        mBinding!!.connectButton.isEnabled = enable
    }

    override fun createAccount(accountCreationModel: AccountCreationModel) {
        (requireActivity() as AccountWizardActivity).createAccount(accountCreationModel)
    }

    override fun cancel() {
        activity?.onBackPressed()
    }

    companion object {
        val TAG = JamiAccountConnectFragment::class.simpleName!!
        fun newInstance(ringAccountViewModel: AccountCreationModelImpl): JamiAccountConnectFragment {
            val fragment = JamiAccountConnectFragment()
            fragment.model = ringAccountViewModel
            return fragment
        }
    }
}