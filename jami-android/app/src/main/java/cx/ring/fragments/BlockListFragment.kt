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
package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import cx.ring.account.AccountEditionFragment
import cx.ring.adapters.BlockListAdapter
import cx.ring.viewholders.BlockListViewHolder.BlockListListeners
import cx.ring.databinding.FragBlocklistBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.ActionHelper.launchUnblockContactAction
import dagger.hilt.android.AndroidEntryPoint
import net.jami.contactrequests.BlockListPresenter
import net.jami.contactrequests.BlockListView
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.services.AccountService
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class BlockListFragment : BaseSupportFragment<BlockListPresenter, BlockListView>(), BlockListView,
    BlockListListeners {

    @Inject
    @Singleton
    lateinit var mAccountService: AccountService

    private var mAdapter: BlockListAdapter? = null
    private var binding: FragBlocklistBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragBlocklistBinding.inflate(inflater, container, false).apply {
        (parentFragment as? AppBarStateListener)?.onAppBarScrollTargetViewChanged(blocklist)
        binding = this
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        val accountId = arguments?.getString(AccountEditionFragment.ACCOUNT_ID_KEY) ?: return
        presenter.setAccountId(accountId)
    }

    override fun onUnblockClicked(contact: Contact) {
        launchUnblockContactAction(
            context = requireContext(),
            accountId = mAccountService.currentAccount!!.accountId,
            contact = contact
        ) { _, _ -> presenter.unblockClicked(contact) }
    }

    override fun updateView(list: Collection<ContactViewModel>) {
        if (binding!!.blocklist.adapter != null) {
            mAdapter!!.replaceAll(list)
        } else {
            mAdapter = BlockListAdapter(list, this@BlockListFragment)
            val layoutManager = LinearLayoutManager(activity)
            binding!!.blocklist.layoutManager = layoutManager
            binding!!.blocklist.adapter = mAdapter
        }
    }

    override fun displayEmptyListMessage(display: Boolean) {
        binding!!.placeholder.visibility = if (display) View.VISIBLE else View.GONE
    }

    fun setAccount(accountId: String) {
        presenter.setAccountId(accountId)
    }

    companion object {
        val TAG: String = BlockListFragment::class.simpleName!!
    }

}