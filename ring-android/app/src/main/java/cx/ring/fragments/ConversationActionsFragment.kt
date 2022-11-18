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
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.ColorChooserBottomSheet
import cx.ring.client.ContactDetailsActivity
import cx.ring.client.EmojiChooserBottomSheet
import cx.ring.databinding.FragConversationActionsBinding
import cx.ring.databinding.ItemContactActionBinding
import cx.ring.services.SharedPreferencesServiceImpl
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationActionsFragment : Fragment() {

    @Inject
    @Singleton
    lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    @Singleton lateinit
    var mAccountService: AccountService

    private var binding: FragConversationActionsBinding? = null
    private val mDisposableBag = CompositeDisposable()
    private val adapter = ContactActionAdapter(mDisposableBag)
    private var colorAction: ContactAction? = null
    private var symbolAction: ContactAction? = null
    private var conversation: Conversation? = null
    private var colorActionPosition = 0
    private var symbolActionPosition = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationActionsBinding.inflate(inflater, container, false).apply {

            val path = ConversationPath.fromBundle(arguments)!!

            conversation = mConversationFacade
                    .startConversation(path.accountId, path.conversationUri)
                    .blockingGet()


            val preferences = SharedPreferencesServiceImpl.getConversationPreferences(
                requireActivity(),
                path.accountId,
                path.conversationUri
            )

            colorActionPosition = 0
            symbolActionPosition = 1

            colorAction = ContactAction(R.drawable.item_color_background, 0, getText(R.string.conversation_preference_color)) {
                ColorChooserBottomSheet { color ->
                    colorAction!!.iconTint = color
                    (activity as ContactDetailsActivity).updateColor(color)
                    adapter.notifyItemChanged(colorActionPosition)
                    preferences.edit()
                        .putInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, color)
                        .apply()
                }.show(parentFragmentManager, "colorChooser")
            }
            val color = preferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, resources.getColor(R.color.color_primary_light))
            colorAction!!.iconTint = color
            adapter.actions.add(colorAction!!)
            symbolAction = ContactAction(0, getText(R.string.conversation_preference_emoji)) {
                EmojiChooserBottomSheet{ emoji ->
                    symbolAction?.setSymbol(emoji)
                    adapter.notifyItemChanged(symbolActionPosition)
                    preferences.edit()
                        .putString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, emoji)
                        .apply()
                }.show(parentFragmentManager, "colorChooser")
            }.apply {
                setSymbol(preferences.getString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, resources.getString(R.string.conversation_default_emoji)))
                adapter.actions.add(this)
            }

            @StringRes val infoString = if (conversation!!.isSwarm)
                if (conversation!!.mode.blockingFirst() == Conversation.Mode.OneToOne)
                    R.string.conversation_type_private
                else
                    R.string.conversation_type_group
            else R.string.conversation_type_contact

            conversationType.setText(infoString)
            val conversationUri = conversation?.uri.toString()//if (conversation.isSwarm) conversation.uri.toString() else conversation.uriTitle
            conversationId.text = conversationUri
            infoCard.setOnClickListener { copyAndShow(path.conversationId) }

            if (conversation?.contacts!!.size <= 2 && conversation?.contacts!!.isNotEmpty()) {
                val contact = conversation?.contact!!
                adapter.actions.add(ContactAction(R.drawable.baseline_call_24, getText(R.string.ab_action_audio_call)) {
                    (activity as ContactDetailsActivity).goToCallActivity(conversation!!, contact.uri, true)
                })
                adapter.actions.add(ContactAction(R.drawable.baseline_videocam_24, getText(R.string.ab_action_video_call)) {
                    (activity as ContactDetailsActivity).goToCallActivity(conversation!!, contact.uri, false)
                })
                if (!conversation?.isSwarm!!) {
                    adapter.actions.add(ContactAction(R.drawable.baseline_clear_all_24, getText(R.string.conversation_action_history_clear)) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.clear_history_dialog_title)
                            .setMessage(R.string.clear_history_dialog_message)
                            .setPositiveButton(R.string.conversation_action_history_clear) { _: DialogInterface?, _: Int ->
                                mConversationFacade.clearHistory(conversation?.accountId!!, contact.uri).subscribe()
                                Snackbar.make(root, R.string.clear_history_completed, Snackbar.LENGTH_LONG).show()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()
                    })
                }

                adapter.actions.add(ContactAction(R.drawable.baseline_block_24, getText(R.string.conversation_action_block_this)) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.block_contact_dialog_title, conversationUri))
                        .setMessage(getString(R.string.block_contact_dialog_message, conversationUri))
                        .setPositiveButton(R.string.conversation_action_block_this) { _: DialogInterface?, _: Int ->
                            mAccountService.removeContact(conversation?.accountId!!, contact.uri.rawRingId,true)
                            Toast.makeText(requireContext(), getString(R.string.block_contact_completed, conversationUri), Toast.LENGTH_LONG).show()
                            requireActivity().finish()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show()
                })
            }


            contactActionList.adapter = adapter
            binding = this
        }.root

    override fun onDestroy() {
        adapter.actions.clear()
        mDisposableBag.dispose()
        super.onDestroy()
        colorAction = null
        binding = null
    }

    private fun copyAndShow(toCopy: String) {
        val clipboard = requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), toCopy))
        Snackbar.make(binding!!.root, getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy), Snackbar.LENGTH_LONG).show()
    }

    internal class ContactAction {
        @DrawableRes
        val icon: Int
        val drawable: Single<Drawable>?
        val title: CharSequence
        val callback: () -> Unit
        var iconTint: Int
        var iconSymbol: CharSequence? = null

        constructor(@DrawableRes i: Int, tint: Int, t: CharSequence, cb: () -> Unit) {
            icon = i
            iconTint = tint
            title = t
            callback = cb
            drawable = null
        }

        constructor(@DrawableRes i: Int, t: CharSequence, cb: () -> Unit) {
            icon = i
            iconTint = Color.BLACK
            title = t
            callback = cb
            drawable = null
        }

        constructor(d: Single<Drawable>?, t: CharSequence, cb: () -> Unit) {
            drawable = d
            icon = 0
            iconTint = Color.BLACK
            title = t
            callback = cb
        }

        fun setSymbol(t: CharSequence?) {
            iconSymbol = t
        }
    }

    internal class ContactActionView(
        val binding: ItemContactActionBinding,
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

    private class ContactActionAdapter(private val disposable: CompositeDisposable) :
        RecyclerView.Adapter<ContactActionView>() {
        val actions = ArrayList<ContactAction>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactActionView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemContactActionBinding.inflate(layoutInflater, parent, false)
            return ContactActionView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactActionView, position: Int) {
            val action = actions[position]
            holder.disposable.clear()
            if (action.drawable != null) {
                holder.disposable.add(action.drawable.subscribe { background: Drawable ->
                    holder.binding.actionIcon.background = background
                })
            } else {
                holder.binding.actionIcon.setBackgroundResource(action.icon)
                holder.binding.actionIcon.text = action.iconSymbol
                if (action.iconTint != Color.BLACK) ViewCompat.setBackgroundTintList(
                    holder.binding.actionIcon,
                    ColorStateList.valueOf(action.iconTint)
                )
            }
            holder.binding.actionTitle.text = action.title
            holder.callback = action.callback
        }

        override fun onViewRecycled(holder: ContactActionView) {
            holder.disposable.clear()
            holder.binding.actionIcon.background = null
        }

        override fun getItemCount(): Int {
            return actions.size
        }
    }

    companion object {
        private val TAG = ConversationActionsFragment::class.simpleName!!
        fun newInstance(accountId: String, conversationId: Uri) = ConversationActionsFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
        }
    }
}
