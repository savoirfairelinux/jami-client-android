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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import cx.ring.databinding.FragAccJamiConnectBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountConnectPresenter
import net.jami.account.JamiConnectAccountView

@AndroidEntryPoint
class JamiAccountConnectFragment : BaseSupportFragment<JamiAccountConnectPresenter, JamiConnectAccountView>(),
    JamiConnectAccountView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiConnectBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccJamiConnectBinding.inflate(inflater, container, false).apply {
            connectButton.setOnClickListener { presenter.connectClicked() }
            usernameTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.usernameChanged(s.toString())
                }
            })
            passwordTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.passwordChanged(s.toString())
                }
            })
            promptServer.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.serverChanged(s.toString())
                }
            })
            passwordTxt.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    presenter.connectClicked()
                }
                false
            }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun initPresenter(presenter: JamiAccountConnectPresenter) {
        presenter.init(model.model)
    }

    override fun enableConnectButton(enable: Boolean) {
        binding!!.connectButton.isEnabled = enable
    }

    override fun createAccount() {
        (activity as AccountWizardActivity?)?.createAccount()
    }

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}