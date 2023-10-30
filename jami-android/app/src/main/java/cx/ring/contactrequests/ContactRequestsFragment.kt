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
package cx.ring.contactrequests

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragPendingContactRequestsBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.TextUtils
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.contactrequests.ContactRequestsPresenter
import net.jami.contactrequests.ContactRequestsView
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.ConversationFacade

@AndroidEntryPoint
class ContactRequestsFragment :
    BaseSupportFragment<ContactRequestsPresenter, ContactRequestsView>(), ContactRequestsView,
    SmartListListeners {
    private var mAdapter: SmartListAdapter? = null
    private var binding: FragPendingContactRequestsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragPendingContactRequestsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAdapter = null
        binding = null
    }

    override fun updateView(list: List<Conversation>, conversationFacade: ConversationFacade, disposable: CompositeDisposable) {
        val binding = binding ?: return
        binding.placeholder.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        val adapter = mAdapter
        if (adapter != null) {
            adapter.update(list)
        } else {
            binding.requestsList.layoutManager = LinearLayoutManager(activity)
            mAdapter = SmartListAdapter(ConversationFacade.ConversationList(list), this@ContactRequestsFragment, conversationFacade, disposable).apply {
                binding.requestsList.adapter = this
            }
        }
        binding.requestsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                (activity as HomeActivity?)?.setToolbarElevation(recyclerView.canScrollVertically(SCROLL_DIRECTION_UP))
            }
        })
    }

    override fun updateItem(item: Conversation) {
        //mAdapter?.update(item)
    }

    override fun goToConversation(accountId: String, contactId: Uri) {
        (requireActivity() as HomeActivity).startConversation(accountId, contactId)
    }

    override fun copyNumber(uri: Uri) {
        val number = uri.toString()
        TextUtils.copyToClipboard(requireContext(), number)
        val snackbarText = getString(
            R.string.conversation_action_copied_peer_number_clipboard,
            TextUtils.getShortenedNumber(number)
        )
        Snackbar.make(binding!!.root, snackbarText, Snackbar.LENGTH_LONG).show()
    }

    override fun onItemClick(item: Conversation) {
        presenter.contactRequestClicked(item.accountId, item.uri)
    }

    override fun onItemLongClick(item: Conversation) {
        MaterialAlertDialogBuilder(requireContext())
            .setItems(R.array.swarm_actions) { dialog, which ->
                when (which) {
                    0 -> presenter.copyNumber(item)
                    1 -> presenter.removeConversation(item)
                    2 -> presenter.banContact(item)
                }
            }
            .show()
    }

    companion object {
        private val TAG = ContactRequestsFragment::class.simpleName!!
        private const val SCROLL_DIRECTION_UP = -1
    }
}