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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.channels.ChannelRepository
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Conversation.ConversationActionCallback
import net.jami.model.Uri
import net.jami.services.ConversationFacade
import net.jami.services.ContactService
import net.jami.services.AccountService
import net.jami.smartlist.SmartListPresenter
import net.jami.smartlist.SmartListView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import javax.inject.Inject

@AndroidEntryPoint
class SmartListFragment : BaseSupportFragment<SmartListPresenter, SmartListView>(),
    SmartListListeners, ConversationActionCallback, SmartListView {
    private val groupsOnly by lazy { arguments?.getBoolean(ARG_GROUPS_ONLY) == true }
    private var lastList: Triple<ConversationFacade.ConversationList, ConversationFacade, CompositeDisposable>? = null
    private var mSmartListAdapter: SmartListAdapter? = null
    private var binding: FragSmartlistBinding? = null
    @Inject
    lateinit var contactService: ContactService
    @Inject
    lateinit var accountService: AccountService

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
        binding?.loadingIndicator?.visibility = if (loading) View.VISIBLE else View.GONE
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
                    .setData(selected.toString().toUri())
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
            }.show()
    }

    override fun displayNoConversationMessage() {
        binding?.placeholderText?.setText(R.string.conversation_placeholder)
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
        presenter.addDisposable(ActionHelper.launchBlockContactAction(
            context = requireContext(),
            accountId = accountId,
            contact = contact
        ) { _, _ -> presenter.blockContact(accountId, contact) })

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
        lastList = Triple(conversations, conversationFacade, parentDisposable)
        val activeChannel = ChannelRepository(
            requireContext(), accountService.currentAccount?.accountId
        ).activeChannel()
        binding?.apply {
            // Only show conversations of the active channel; the Groups view shows its groups only.
            // Groups are gathered at the top, each part keeping its order of last interaction.
            val filteredConversations = conversations.conversations
                .filter { activeChannel.contains(it) }
                .let { list -> if (groupsOnly) list.filter { it.isSwarmGroup() } else list }
            val visibleList = sectionedList(filteredConversations)
            val visibleConversations = visibleList.conversations
            // The presenter only knows about the full list: show our own placeholder when the
            // channel filter leaves nothing to display.
            val filteredOut = visibleConversations.isEmpty() && conversations.searchResult.result.isEmpty()
            placeholderText.setText(if (groupsOnly) R.string.channels_no_groups else R.string.channels_no_conversations)
            placeholder.isVisible = filteredOut
            if (confsList.adapter == null) {
                confsList.adapter = SmartListAdapter(
                        visibleList, this@SmartListFragment, conversationFacade, parentDisposable
                ).apply { mSmartListAdapter = this }

                confsList.setHasFixedSize(true)
            } else {
                mSmartListAdapter?.update(visibleList)
            }
            confsList.visibility = View.VISIBLE
        }
    }

    private fun sectionedList(conversations: List<Conversation>): ConversationFacade.ConversationList {
        val (groups, direct) = conversations.partition { it.isSwarmGroup() }
        val ordered = groups + direct
        val headers = buildList {
            if (groups.isNotEmpty())
                add(
                    ConversationFacade.ConversationList.SectionHeader(
                        0, net.jami.smartlist.ConversationItemViewModel.Title.Groups
                    )
                )
            if (direct.isNotEmpty())
                add(
                    ConversationFacade.ConversationList.SectionHeader(
                        groups.size + if (groups.isNotEmpty()) 1 else 0,
                        net.jami.smartlist.ConversationItemViewModel.Title.Conversations
                    )
                )
        }
        return ConversationFacade.ConversationList(
            conversations = ordered,
            sectionHeaders = headers
        )
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
            val currentMode = item.mode.blockingFirst()
            val effectiveMode = if (currentMode == Conversation.Mode.Syncing) {
                item.requestMode ?: Conversation.Mode.OneToOne
            } else {
                currentMode
            }

            val isGroup = effectiveMode == Conversation.Mode.AdminInvitesOnly ||
                    effectiveMode == Conversation.Mode.InvitesOnly ||
                    effectiveMode == Conversation.Mode.Public

            if (isGroup) {
                ActionListBottomSheet(
                    R.array.swarm_group_actions,
                    R.array.swarm_group_action_icons
                ) { which ->
                    when (which) {
                        0 -> addToChannel(item)
                        1 -> presenter.removeConversation(item)
                    }
                }.show(childFragmentManager, "SmartListFragment")
            } else {
                ActionListBottomSheet(
                    R.array.swarm_one_to_one_actions,
                    R.array.swarm_one_to_one_action_icons
                ) { which ->
                    when (which) {
                        0 -> addToChannel(item)
                        1 -> presenter.copyNumber(item)
                        2 -> presenter.clearConversation(item)
                        3 -> presenter.removeConversation(item)
                        4 -> presenter.blockContact(item)
                    }
                }.show(childFragmentManager, "SmartListFragment")
            }
        } else {
            ActionListBottomSheet(
                R.array.conversation_actions,
                R.array.conversation_action_icons
            ) { which ->
                when (which) {
                    ActionHelper.ACTION_COPY -> presenter.copyNumber(item)
                    ActionHelper.ACTION_CLEAR -> presenter.clearConversation(item)
                    ActionHelper.ACTION_DELETE -> presenter.removeConversation(item)
                    ActionHelper.ACTION_BLOCK -> presenter.blockContact(item)
                    ActionHelper.ACTION_ADD_TO_CHANNEL -> addToChannel(item)
                }
            }.show(childFragmentManager, "SmartListFragment")
        }
    }

    private fun addToChannel(conversation: Conversation) {
        // A group is placed in channels as itself; a one-to-one conversation follows its contact.
        if (conversation.isSwarmGroup()) {
            chooseChannels(conversation.uri.rawUriString, group = true)
            return
        }
        contactService.getLoadedConversation(conversation)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ viewModel ->
                val contact = viewModel.getContact()?.contact ?: return@subscribe
                chooseChannels(contact.uri.rawUriString, group = false)
            }, { error ->
                Log.e(TAG, "Unable to load contact for channel assignment", error)
                Snackbar.make(requireView(), error.message ?: "Unable to add to a Channel", Snackbar.LENGTH_LONG).show()
            })
    }

    /**
     * Membership is a choice among the channels, and a conversation may be in several: ticking
     * a channel puts it there, unticking takes it out. "All" holds everything and is not a choice.
     */
    private fun chooseChannels(id: String, group: Boolean) {
        val context = requireContext()
        val repository = ChannelRepository(context, accountService.currentAccount?.accountId)
        val channels = repository.load().filterNot { it.isAllContacts }
        val checked = BooleanArray(channels.size) { index ->
            id in if (group) channels[index].groups else channels[index].members
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.channels_membership)
            .setMultiChoiceItems(channels.map { it.name }.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selected = channels.filterIndexed { index, _ -> checked[index] }.map { it.name }.toSet()
                repository.save(repository.load().map { channel ->
                    if (channel.isAllContacts) channel
                    else {
                        val holding = (if (group) channel.groups else channel.members).toMutableSet().apply {
                            if (channel.name in selected) add(id) else remove(id)
                        }
                        if (group) channel.copy(groups = holding) else channel.copy(members = holding)
                    }
                })
                Snackbar.make(requireView(), R.string.channels_updated, Snackbar.LENGTH_SHORT).show()
                lastList?.let { (list, facade, disposable) -> updateList(list, facade, disposable) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        val TAG = SmartListFragment::class.simpleName!!
        private const val ARG_GROUPS_ONLY = "groups_only"

        fun newInstance(groupsOnly: Boolean) = SmartListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_GROUPS_ONLY, groupsOnly) }
        }
    }

}