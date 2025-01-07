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

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.client.ConversationDetailsActivity
import cx.ring.databinding.FragMembersBottomsheetBinding
import cx.ring.databinding.ItemMembersBottomsheetBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.TextUtils.copyAndShow
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class MembersBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject
    @Singleton
    lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    @Singleton lateinit
    var mAccountService: AccountService

    private var binding: FragMembersBottomsheetBinding? = null
    private val mDisposableBag = CompositeDisposable()
    private val adapter = ContactActionAdapter(mDisposableBag)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragMembersBottomsheetBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val path = ConversationPath.fromBundle(arguments)!!
        val contactUri = Uri.fromString(requireArguments().getString(KEY_CONTACT_ID)!!)

        val conversation = mConversationFacade
            .startConversation(path.accountId, path.conversationUri)
            .blockingGet()

        adapter.actions.add(ContactAction(null,
            getString(R.string.bottomsheet_contact, contactUri.host)) {
            copyAndShow(requireContext(), getString(R.string.clip_contact_uri), contactUri.host)
        })

        adapter.actions.add(ContactAction(null, getText(R.string.ab_action_audio_call)) {
            (activity as? ConversationDetailsActivity)?.goToCallActivity(conversation, contactUri, false)
        })

        adapter.actions.add(ContactAction(null, getText(R.string.ab_action_video_call)) {
            (activity as? ConversationDetailsActivity)?.goToCallActivity(conversation, contactUri, true)
        })

        adapter.actions.add(ContactAction(null, getString(R.string.send_message)) {
            (activity as? ConversationDetailsActivity)?.goToConversationActivity(path.accountId, contactUri)
        })

        adapter.actions.add(ContactAction(null, getString(R.string.bottomsheet_remove)) {
            mAccountService.removeConversationMember(
                path.accountId,
                conversationId = path.conversationUri.host,
                uri = contactUri
            )
            dismiss()
        })

        binding!!.actions.adapter = adapter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }

    override fun onDestroy() {
        mDisposableBag.dispose()
        super.onDestroy()
        binding = null
    }

    private class ContactActionAdapter(private val disposable: CompositeDisposable) :
        RecyclerView.Adapter<ContactActionView>() {
        val actions = ArrayList<ContactAction>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactActionView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemMembersBottomsheetBinding.inflate(layoutInflater, parent, false)
            return ContactActionView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactActionView, position: Int) {
            val action = actions[position]
            holder.disposable.clear()
            holder.binding.actionTitle.text = action.title
            holder.callback = action.callback
        }

        override fun onViewRecycled(holder: ContactActionView) {
            holder.disposable.clear()
        }

        override fun getItemCount(): Int {
            return actions.size
        }
    }

    private class ContactActionView(
        val binding: ItemMembersBottomsheetBinding,
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
                    Log.w(TAG, "Error performing action", e)
                }
            }
        }
    }

    private class ContactAction(d: Single<Drawable>?, t: CharSequence, cb: () -> Unit) {
        @DrawableRes
        val icon: Int = 0
        val drawable: Single<Drawable>? = d
        val title: CharSequence = t
        val callback: () -> Unit = cb
    }

    companion object {
        val TAG = MembersBottomSheetFragment::class.simpleName!!
        const val KEY_CONTACT_ID = "CONTACT_ID"
        fun newInstance(accountId: String, contactUri: Uri, conversationId: Uri) = MembersBottomSheetFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
            arguments!!.putString(KEY_CONTACT_ID, contactUri.uri)
        }
    }
}
