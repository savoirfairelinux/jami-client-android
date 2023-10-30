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

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationModelImpl
import cx.ring.account.AccountCreationViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView

@AndroidEntryPoint
class TVHomeAccountCreationFragment : JamiGuidedStepFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private val model: AccountCreationViewModel by activityViewModels()

    override fun goToAccountCreation() {
        model.model = AccountCreationModelImpl().apply {
            isLink = false
        }
        add(parentFragmentManager, TVJamiAccountCreationFragment())
    }

    override fun goToAccountLink() {
        model.model = AccountCreationModelImpl().apply {
            isLink = true
        }
        add(parentFragmentManager, TVJamiLinkAccountFragment())
    }

    override fun goToAccountConnect() {
        //TODO
    }

    override fun goToSIPAccountCreation() {
        //TODO
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val title = getString(R.string.account_creation_home)
        val breadcrumb = ""
        val description = getString(R.string.help_ring)
        val icon = requireContext().getDrawable(R.drawable.ic_jami)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addAction(context, actions, LINK_ACCOUNT, getString(R.string.account_link_button), "", true)
        addAction(context, actions, CREATE_ACCOUNT, getString(R.string.account_create_title), "", true)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            LINK_ACCOUNT -> presenter.clickOnLinkAccount()
            CREATE_ACCOUNT -> presenter.clickOnCreateAccount()
            else -> requireActivity().finish()
        }
    }

    companion object {
        private const val LINK_ACCOUNT = 0L
        private const val CREATE_ACCOUNT = 1L
    }
}