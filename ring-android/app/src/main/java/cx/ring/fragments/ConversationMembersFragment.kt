/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Amirhossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.ContactDetailsActivity
import cx.ring.databinding.FragConversationMembersBinding
import cx.ring.databinding.ItemContactHorizontalBinding
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationMembersFragment : Fragment() {

    @Inject
    @Singleton
    lateinit
    var mConversationFacade: ConversationFacade

    private var binding: FragConversationMembersBinding? = null
    private val mDisposableBag = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationMembersBinding.inflate(inflater, container, false).apply {

            val path = ConversationPath.fromBundle(arguments)!!

            val conversation = mConversationFacade
                .startConversation(path.accountId, path.conversationUri)
                .blockingGet()

            mDisposableBag.add(mConversationFacade.observeConversation(conversation)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { requireActivity().finish() }
                .subscribe({ vm ->
                    contactList.adapter = ContactViewAdapter(mDisposableBag, vm.contacts)
                    { contact -> copyAndShow(contact.uri.rawUriString) }
                }) { e ->
                    Log.e(TAG, "e", e)
                    requireActivity().finish()
                })

            binding = this
        }.root

    override fun onDestroy() {
        mDisposableBag.dispose()
        super.onDestroy()
        binding = null
    }

    private fun copyAndShow(toCopy: String) {
        val clipboard = requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), toCopy))
        Snackbar.make(binding!!.root, getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy), Snackbar.LENGTH_LONG).show()
    }

    private class ContactViewAdapter(
        private val disposable: CompositeDisposable,
        private val contacts: List<ContactViewModel>,
        private val callback: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactDetailsActivity.ContactView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactDetailsActivity.ContactView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemContactHorizontalBinding.inflate(layoutInflater, parent, false)
            return ContactDetailsActivity.ContactView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactDetailsActivity.ContactView, position: Int) {
            val contact = contacts[position]
            holder.disposable.clear()
            holder.disposable.add(
                AvatarFactory.getAvatar(holder.itemView.context, contact, false)
                .subscribe { drawable: Drawable ->
                    holder.binding.photo.setImageDrawable(drawable)
                })
            holder.binding.displayName.text =
                if (contact.contact.isUser) holder.itemView.context.getText(R.string.conversation_info_contact_you) else contact.displayName
            holder.itemView.setOnClickListener { callback.invoke(contact.contact) }
        }

        override fun onViewRecycled(holder: ContactDetailsActivity.ContactView) {
            holder.disposable.clear()
            holder.binding.photo.setImageDrawable(null)
        }

        override fun getItemCount(): Int {
            return contacts.size
        }
    }

    companion object {
        private val TAG = ConversationMembersFragment::class.simpleName!!
        fun newInstance(accountId: String, conversationId: Uri) = ConversationMembersFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
        }
    }
}
