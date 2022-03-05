/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.account

import android.os.Bundle
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationModelImpl
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView

@AndroidEntryPoint
class TVHomeAccountCreationFragment : JamiGuidedStepFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    override fun goToAccountCreation() {
        add(parentFragmentManager, TVJamiAccountCreationFragment.newInstance(AccountCreationModelImpl().apply {
            isLink = false
        }))
    }

    override fun goToAccountLink() {
        add(parentFragmentManager, TVJamiLinkAccountFragment.newInstance(AccountCreationModelImpl().apply {
            isLink = true
        }))
    }

    override fun goToAccountConnect() {
        //TODO
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Ring_Leanback_GuidedStep_First
    }

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