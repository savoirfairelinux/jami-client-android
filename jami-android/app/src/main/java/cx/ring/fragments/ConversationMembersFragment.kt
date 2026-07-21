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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.client.ConversationDetailsActivity
import cx.ring.databinding.FragConversationMembersBinding
import cx.ring.databinding.ItemContactHorizontalBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.MemberRole
import net.jami.model.Uri
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationMembersFragment : Fragment() {

    @Inject
    @Singleton
    lateinit var mConversationFacade: ConversationFacade

    @Inject
    @Singleton
    lateinit var contactService: ContactService

    private var binding: FragConversationMembersBinding? = null
    private val mDisposableBag = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationMembersBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val path = ConversationPath.fromBundle(arguments)!!
        mDisposableBag.add(mConversationFacade
            .startConversation(path.accountId, path.conversationUri)
            .flatMapObservable { conversation -> // Keep reference on conversation for roles.
                conversation.contactUpdates.map { contacts ->
                    // Non-admins should never see banned members in the member list,
                    val isAdmin = conversation.isUserGroupAdmin()
                    val visibleContacts = if (isAdmin) contacts
                        else contacts.filterNot { conversation.roles[it.uri.uri] == MemberRole.BLOCKED }
                    Pair(conversation, visibleContacts.sortedBy { !it.isUser })
                }
            }
            .flatMap { (conversation, contacts) ->
                contactService.observeContact(path.accountId, contacts, false, withUser = true)
                    .map { contactViewModels -> Pair(conversation, contactViewModels) }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { (conversation, contactViewModels) ->
                val adapter = binding!!.contactList.adapter
                if (adapter == null) {
                    binding!!.contactList.adapter =
                        ContactViewAdapter(mDisposableBag, contactViewModels, conversation.roles)
                        { contact ->
                            val actionBottomSheet = MembersBottomSheetFragment
                                .newInstance(path.accountId, contact.uri, path.conversationUri)
                            actionBottomSheet
                                .show(parentFragmentManager, MembersBottomSheetFragment.TAG)
                        }
                } else {
                    (adapter as ContactViewAdapter).update(contactViewModels)
                }
            })
    }

    override fun onDestroy() {
        mDisposableBag.dispose()
        super.onDestroy()
        binding = null
    }

    private class ContactView(
        val binding: ItemContactHorizontalBinding,
        parentDisposable: CompositeDisposable
    ) : RecyclerView.ViewHolder(binding.root) {

        var callback: (() -> Unit)? = null
        val disposable = CompositeDisposable()

        init {
            parentDisposable.add(disposable)
            itemView.setOnClickListener {
                try {
                    callback?.invoke()
                } catch (e: Exception) {
                    android.util.Log.w(ConversationDetailsActivity.TAG, "Error performing action", e)
                }
            }
        }
    }

    private class ContactViewAdapter(
        private val disposable: CompositeDisposable,
        private var contacts: List<ContactViewModel>,
        private val roles: Map<String, MemberRole>,
        private val callback: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemContactHorizontalBinding.inflate(layoutInflater, parent, false)
            return ContactView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactView, position: Int) {
            val contact = contacts[position]
            holder.disposable.clear()
            holder.disposable.add(
                AvatarFactory.getAvatar(holder.itemView.context, contact, false)
                .subscribe { drawable: Drawable ->
                    holder.binding.photo.setImageDrawable(drawable)
                })
            val role = roles[contact.contact.uri.uri]
            holder.binding.moderator.apply {
                when (role) {
                    MemberRole.ADMIN -> {
                        isVisible = true
                        text = context.getText(R.string.group_admin)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_moderateur, 0)
                    }
                    MemberRole.BLOCKED -> {
                        isVisible = true
                        text = context.getText(R.string.group_member_blocked)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_block_24, 0)
                    }
                    MemberRole.INVITED -> {
                        isVisible = true
                        text = context.getText(R.string.group_member_invited)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_mail_24, 0)
                    }
                    else -> isVisible = false
                }
            }
            holder.itemView.alpha = if (role == MemberRole.BLOCKED || role == MemberRole.INVITED) 0.5f else 1f
            holder.binding.displayName.text =
                if (contact.contact.isUser) holder.itemView.context.getText(R.string.conversation_info_contact_you) else contact.displayName
            holder.itemView.setOnClickListener { callback.invoke(contact.contact) }
        }

        fun update(contacts: List<ContactViewModel>) {
            this.contacts = contacts
            notifyDataSetChanged()
        }

        override fun onViewRecycled(holder: ContactView) {
            holder.disposable.clear()
            holder.binding.photo.setImageDrawable(null)
        }

        override fun getItemCount(): Int {
            return contacts.size
        }
    }

    companion object {
        val TAG = ConversationMembersFragment::class.simpleName!!
        fun newInstance(accountId: String, conversationId: Uri) = ConversationMembersFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
        }
    }
}
