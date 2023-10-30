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
import net.jami.account.JamiLinkAccountPresenter
import net.jami.account.JamiLinkAccountView
import net.jami.utils.StringUtils.toPassword

@AndroidEntryPoint
class TVJamiLinkAccountFragment : JamiGuidedStepFragment<JamiLinkAccountPresenter, JamiLinkAccountView>(),
    JamiLinkAccountView {
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
        val title = getString(R.string.account_link_title)
        val breadcrumb = ""
        val description = """
            ${getString(R.string.help_password_enter)}
            ${getString(R.string.help_pin_enter)}
            """.trimIndent()
        val icon = requireContext().getDrawable(R.drawable.ic_contact_picture_fallback)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addPasswordAction(context, actions, PASSWORD, getString(R.string.account_enter_password), "", "")
        addPasswordAction(context, actions, PIN, getString(R.string.account_link_prompt_pin), "", "")
        addDisabledAction(context, actions, LINK, getString(R.string.account_link_title), "", null, true)
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == LINK) {
            presenter.linkClicked()
        }
    }

    override fun enableLinkButton(enable: Boolean) {
        findActionPositionById(LINK).takeUnless { it == -1 }?.also { position ->
            actions[position]?.isEnabled = enable
            notifyActionChanged(position)
        }
    }

    override fun showPin(show: Boolean) {
        // TODO
    }

    override fun createAccount() {
        (activity as TVAccountWizard?)?.createAccount()
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        val password = action.editDescription.toString()
        action.description = if (password.isNotEmpty()) toPassword(password) else getString(R.string.account_enter_password)
        when (action.id) {
            PASSWORD -> {
                notifyActionChanged(findActionPositionById(PASSWORD))
                presenter.passwordChanged(password)
            }
            PIN -> {
                notifyActionChanged(findActionPositionById(PIN))
                presenter.pinChanged(action.editDescription.toString())
            }
        }
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    companion object {
        private const val PASSWORD = 1L
        private const val PIN = 2L
        private const val LINK = 3L
    }
}