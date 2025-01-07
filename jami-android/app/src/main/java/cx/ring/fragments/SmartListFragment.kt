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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.CallActivity
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragSmartlistBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.ActionHelper
import cx.ring.utils.ConversationPath
import cx.ring.utils.TextUtils.copyAndShow
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Conversation.ConversationActionCallback
import net.jami.model.Uri
import net.jami.services.ConversationFacade
import net.jami.smartlist.SmartListPresenter
import net.jami.smartlist.SmartListView

@AndroidEntryPoint
class SmartListFragment : BaseSupportFragment<SmartListPresenter, SmartListView>(),
    SmartListListeners, ConversationActionCallback, SmartListView {
    private var mSmartListAdapter: SmartListAdapter? = null
    private var binding: FragSmartlistBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragSmartlistBinding.inflate(inflater, container, false).apply {
            (confsList.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
            binding = this
        }.root

    fun getRecyclerView(): RecyclerView? = binding?.confsList

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun setLoading(loading: Boolean) {
        binding!!.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun removeConversation(accountId: String, conversationUri: Uri) {
        presenter.removeConversation(accountId, conversationUri)
    }

    override fun clearConversation(accountId: String, conversationUri: Uri) {
        presenter.clearConversation(accountId, conversationUri)
    }

    override fun copyContactNumberToClipboard(contactNumber: String) {
        copyAndShow(requireContext(), getString(R.string.clip_contact_uri), contactNumber)
    }

    override fun displayChooseNumberDialog(numbers: Array<CharSequence>) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.choose_number)
            .setItems(numbers) { _: DialogInterface?, which: Int ->
                val selected = numbers[which]
                val intent = Intent(Intent.ACTION_CALL)
                    .setClass(context, CallActivity::class.java)
                    .setData(android.net.Uri.parse(selected.toString()))
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
            }.show()
    }

    override fun displayNoConversationMessage() {
        binding?.placeholder?.visibility = View.VISIBLE
    }

    override fun hideNoConversationMessage() {
        binding?.placeholder?.visibility = View.GONE
    }

    override fun displayClearDialog(accountId: String, conversationUri: Uri) {
        ActionHelper.launchClearAction(requireContext(), accountId, conversationUri, this@SmartListFragment)
    }

    override fun displayDeleteDialog(accountId: String, conversationUri: Uri, isGroup: Boolean) {
        if (isGroup)
            ActionHelper.launchDeleteSwarmGroupAction(
                context = requireContext(),
                accountId = accountId,
                uri = conversationUri,
                callback = this@SmartListFragment
            )
        else
            ActionHelper.launchDeleteSwarmOneToOneAction(
                context = requireContext(),
                accountId = accountId,
                uri = conversationUri,
                callback = this@SmartListFragment
            )
    }

    override fun displayBlockDialog(accountId: String, contact: Contact) =
        ActionHelper.launchBlockContactAction(
            context = requireContext(),
            accountId = accountId,
            contact = contact
        ) { _, _ -> presenter.blockContact(accountId, contact) }

    override fun copyNumber(uri: Uri) {
        copyContactNumberToClipboard(uri.toString())
    }

    override fun hideList() {
        binding!!.confsList.visibility = View.GONE
        mSmartListAdapter?.update(ConversationFacade.ConversationList())
    }

    override fun updateList(
        conversations: ConversationFacade.ConversationList,
        conversationFacade: ConversationFacade,
        parentDisposable: CompositeDisposable
    ) {
        binding?.apply {
            if (confsList.adapter == null) {
                confsList.adapter = SmartListAdapter(
                        conversations, this@SmartListFragment, conversationFacade, parentDisposable
                ).apply { mSmartListAdapter = this }

                confsList.setHasFixedSize(true)
            } else {
                mSmartListAdapter?.update(conversations)
            }
            confsList.visibility = View.VISIBLE
        }
    }

    private fun goToConversation(accountId: String, conversationUri: Uri) {
        Log.w(TAG, "goToConversation $accountId $conversationUri")
        (parentFragment as? HomeFragment)?.collapseSearchActionView()
        (activity as? HomeActivity)?.startConversation(accountId, conversationUri)
    }

    override fun goToCallActivity(accountId: String, conversationUri: Uri, contactId: String) {
        val intent = Intent(Intent.ACTION_CALL)
            .setClass(requireContext(), CallActivity::class.java)
            .putExtras(ConversationPath.toBundle(accountId, conversationUri))
            .putExtra(CallFragment.KEY_HAS_VIDEO, true)
            .putExtra(Intent.EXTRA_PHONE_NUMBER, contactId)
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
    }

    override fun scrollToTop() {
        binding?.confsList?.scrollToPosition(0)
    }

    override fun onItemClick(item: Conversation) {
        goToConversation(item.accountId, item.uri)
    }

    override fun onItemLongClick(item: Conversation) {

        if (item.isSwarm) {
            // Don't display same menu item if swarm group or if swarm one to one.
            if (item.isSwarmGroup()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setItems(R.array.swarm_group_actions) { _, which ->
                        when (which) {
                            0 -> presenter.removeConversation(item)
                        }
                    }
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setItems(R.array.swarm_one_to_one_actions) { _, which ->
                        when (which) {
                            0 -> presenter.copyNumber(item)
                            1 -> presenter.removeConversation(item)
                            2 -> presenter.blockContact(item)
                        }
                    }
                    .show()
            }
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.conversation_actions) { _, which ->
                    when (which) {
                        ActionHelper.ACTION_COPY -> presenter.copyNumber(item)
                        ActionHelper.ACTION_CLEAR -> presenter.clearConversation(item)
                        ActionHelper.ACTION_DELETE -> presenter.removeConversation(item)
                        ActionHelper.ACTION_BLOCK -> presenter.blockContact(item)
                    }
                }
                .show()
        }
    }

    companion object {
        val TAG = SmartListFragment::class.simpleName!!
    }

}