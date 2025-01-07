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
package cx.ring.tv.account

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountConnectPresenter
import net.jami.account.JamiConnectAccountView
import net.jami.utils.StringUtils.toPassword

@AndroidEntryPoint
class TVJamiAccountConnectFragment :
    JamiGuidedStepFragment<JamiAccountConnectPresenter, JamiConnectAccountView>(),
    JamiConnectAccountView {
    private val model: AccountCreationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val m = model.model
        presenter.init(m)
        if (m.photo != null) {
            guidanceStylist.iconView?.setImageBitmap(m.photo as Bitmap?)
        }
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val title = getString(R.string.account_connect_server)
        val breadcrumb = ""
        val description = getString(R.string.help_credentials_enter)
        val icon = requireContext().getDrawable(R.drawable.ic_contact_picture_fallback)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            CONNECT -> presenter.connectClicked()
        }
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        when (action.id) {
            SERVER -> {
                action.title = action.editTitle.toString()
                    .ifEmpty { getString(R.string.prompt_server) }
                presenter.serverChanged(action.editTitle.toString())
            }
            USERNAME -> {
                action.title = action.editTitle.toString()
                    .ifEmpty { getString(R.string.account_username_label) }
                presenter.usernameChanged(action.editTitle.toString())
            }
            PASSWORD -> {
                val description = action.editDescription.toString()
                if (description.isNotEmpty()) action.description = toPassword(description)
                else action.description = getString(R.string.account_enter_password)
                presenter.passwordChanged(action.editDescription.toString())
            }
        }
        notifyActionChanged(findActionPositionById(action.id))
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun enableConnectButton(enable: Boolean) {
        findActionPositionById(CONNECT).takeUnless { it == -1 }?.also { position ->
            actions[position]?.isEnabled = enable
            notifyActionChanged(position)
        }
    }

    override fun createAccount() {
        (activity as TVAccountWizard?)?.createAccount()
    }


    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addEditTextAction(
            context, actions,
            id = SERVER,
            title = R.string.prompt_server,
            desc = R.string.account_enter_servername
        )
        addEditTextAction(
            context, actions,
            id = USERNAME,
            title = R.string.account_username_label,
            desc = R.string.account_enter_username
        )
        addPasswordAction(
            context, actions,
            id = PASSWORD,
            title = getString(R.string.account_enter_password),
            desc = "", editdesc = ""
        )
        addDisabledAction(
            context, actions,
            id = CONNECT,
            title = getString(R.string.account_connect_button),
            desc = "", icon = null, next = true
        )
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    companion object {
        private const val SERVER = 0L
        private const val USERNAME = 1L
        private const val PASSWORD = 2L
        private const val CONNECT = 3L
    }
}