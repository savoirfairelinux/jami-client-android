/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.contactrequests

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragPendingContactRequestsBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.contactrequests.ContactRequestsPresenter
import net.jami.contactrequests.ContactRequestsView
import net.jami.model.Uri
import net.jami.smartlist.SmartListViewModel

@AndroidEntryPoint
class ContactRequestsFragment :
    BaseSupportFragment<ContactRequestsPresenter, ContactRequestsView>(), ContactRequestsView,
    SmartListListeners {
    private var mAdapter: SmartListAdapter? = null
    private var binding: FragPendingContactRequestsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragPendingContactRequestsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAdapter = null
        binding = null
    }

    fun presentForAccount(accountId: String?) {
        val arguments = arguments
        arguments?.putString(ACCOUNT_ID, accountId)
        presenter.updateAccount(accountId)
    }

    override fun onStart() {
        super.onStart()
        val arguments = arguments
        val accountId = arguments?.getString(ACCOUNT_ID)
        presenter.updateAccount(accountId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun updateView(list: MutableList<SmartListViewModel>, disposable: CompositeDisposable) {
        if (binding == null) {
            return
        }
        if (list.isNotEmpty()) {
            binding!!.paneRingID.visibility = View.GONE
        }
        binding!!.placeholder.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        if (binding!!.requestsList.adapter != null) {
            mAdapter!!.update(list)
        } else {
            mAdapter = SmartListAdapter(list, this@ContactRequestsFragment, disposable)
            binding!!.requestsList.adapter = mAdapter
            val mLayoutManager = LinearLayoutManager(activity)
            binding!!.requestsList.layoutManager = mLayoutManager
        }
        binding!!.requestsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                (requireActivity() as HomeActivity).setToolbarElevation(
                    recyclerView.canScrollVertically(
                        SCROLL_DIRECTION_UP
                    )
                )
            }
        })
        updateBadge()
    }

    override fun updateItem(item: SmartListViewModel) {
        if (mAdapter != null) {
            mAdapter!!.update(item)
        }
    }

    override fun goToConversation(accountId: String, contactId: Uri) {
        (requireActivity() as HomeActivity).startConversation(accountId, contactId)
    }

    override fun onItemClick(viewModel: SmartListViewModel) {
        presenter.contactRequestClicked(viewModel.accountId, viewModel.uri)
    }

    override fun onItemLongClick(smartListViewModel: SmartListViewModel) {}
    private fun updateBadge() {
        (requireActivity() as HomeActivity).setBadge(R.id.navigation_requests, mAdapter!!.itemCount)
    }

    companion object {
        private val TAG = ContactRequestsFragment::class.java.simpleName
        @JvmField
        val ACCOUNT_ID = TAG + "accountID"
        private const val SCROLL_DIRECTION_UP = -1
    }
}