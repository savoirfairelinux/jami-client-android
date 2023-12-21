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
import cx.ring.interfaces.Colorable
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
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
class ConversationActionsFragment : Fragment(), Colorable {

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
    private var colorActionPosition = 0
    private var symbolActionPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?
    ): View =
        // Inflate the layout for this fragment
        FragConversationActionsBinding.inflate(inflater, container, false).apply {
            // Init some variables useful for the fragment
            val path = ConversationPath.fromBundle(arguments)!!
            // Get the current conversation
            val conversation = mConversationFacade
                    .startConversation(path.accountId, path.conversationUri)
                    .blockingGet()

            colorActionPosition = 0
            symbolActionPosition = 1

            // Setup an action to allow the user to select a color for the conversation.
            colorAction = ContactAction(
                R.drawable.item_color_background, 0,
                getText(R.string.conversation_preference_color)
            ) {
                ColorChooserBottomSheet { color -> // Color chosen by the user (onclick method).
                    setConversationPreferences(
                        path.accountId,
                        path.conversationUri,
                        color = color
                    )
                    // Need to manually update the color of the conversation as will not get the
                    // update signal from daemon.
                    if (!path.conversationUri.isSwarm) conversation.setColor(color)
                }.show(parentFragmentManager, "colorChooser")
            }
            // Add the action to the list of actions.
            adapter.actions.add(colorAction!!)

            // Setup an action to allow the user to select an Emoji for the conversation.
            symbolAction = ContactAction(0, getText(R.string.conversation_preference_emoji)) {
                EmojiChooserBottomSheet { emoji -> // Emoji chosen by the user (onclick method).
                    if (emoji == null) return@EmojiChooserBottomSheet
                    setConversationPreferences(
                        path.accountId,
                        path.conversationUri,
                        emoji = emoji
                    )
                    // Need to manually update the symbol of the conversation as will not get the
                    // update signal from daemon.
                    if(!path.conversationUri.isSwarm) conversation.setSymbol(emoji.toString())
                }.show(parentFragmentManager, "colorChooser")
            }
            adapter.actions.add(symbolAction!!)

            // Update color on RX color signal.
            mDisposableBag.add(conversation.getColor()
                .startWith(Single.just(getConversationColor(requireContext(), 0)))
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { setColor(getConversationColor(requireContext(), it)) })

            // Update symbol on RX color signal.
            mDisposableBag.add(conversation.getSymbol()
                .startWith(Single.just(getConversationSymbol(requireContext(), null)))
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { setSymbol(getConversationSymbol(requireContext(), it)) })

            // Setup card with
            //  - conversation type (such as "Private swarm")
            //  - conversation id (such as swarm:1234)"
            // The real conversation mode is hidden in TrustRequest when it's a request.
            val conversationMode =
                if (conversation.mode.blockingFirst() == Conversation.Mode.Request)
                    conversation.request!!.mode
                else conversation.mode.blockingFirst()

            @StringRes val infoString =
                if (conversation.isSwarm) {
                    if (conversationMode == Conversation.Mode.OneToOne)
                        R.string.conversation_type_private
                    else {
                        R.string.conversation_type_group
                    }
                }
                else R.string.conversation_type_contact
            conversationType.setText(infoString)
            val conversationUri = conversation.uri.toString()
            conversationId.text = conversationUri
            // Onclick on the card, copy the conversation id to the clipboard and show a snack-bar
            infoCard.setOnClickListener { copyAndShow(path.conversationId) }

            // Setup other actions depending on context
            val callUri: Uri?
            if (conversationMode == Conversation.Mode.OneToOne) {
                callUri = conversation.contact!!.uri
                if (!conversation.contact!!.isBanned) {
                    // Setup block contact action
                    adapter.actions.add(
                        ContactAction(
                            R.drawable.baseline_block_24,
                            getText(R.string.conversation_action_block_this)
                        ) {
                            // Callback when the block action is clicked
                            // Display a dialog to confirm the action
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.block_contact_dialog_title, conversationUri))
                                .setMessage(getString(R.string.block_contact_dialog_message, conversationUri))
                                .setPositiveButton(R.string.conversation_action_block_this){ _: DialogInterface?, _: Int ->
                                    // Ban conversation and display a toast to display success.
                                    mConversationFacade.banConversation(conversation.accountId, conversation.uri)
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.block_contact_completed, conversationUri),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    requireActivity().finish()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()
                        }
                    )
                } else {
                    // Setup unblock contact action
                    adapter.actions.add(
                        ContactAction(
                            R.drawable.baseline_person_add_24,
                            getText(R.string.conversation_action_unblock_this)
                        ) {
                            // Callback when the unblock action is clicked
                            // Display a dialog to confirm the action
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.unblock_contact_dialog_title, conversationUri))
                                .setMessage(getString(R.string.unblock_contact_dialog_message, conversationUri))
                                .setPositiveButton(R.string.conversation_action_unblock_this) { _: DialogInterface?, _: Int ->
                                    // Add contact and display a toast to display success.
                                    mAccountService.addContact(conversation.accountId, callUri.rawRingId)
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.unblock_contact_completed, conversationUri),
                                        Toast.LENGTH_LONG).show()
                                    requireActivity().finish()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()
                        }
                    )
                }
            } else { // If conversation mode is not one to one
                callUri = conversation.uri
            }

            if (!conversation.isSwarm) {
                // Setup clear history action
                adapter.actions.add(
                    ContactAction(
                        R.drawable.baseline_clear_all_24,
                        getText(R.string.conversation_action_history_clear)
                    ) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.clear_history_dialog_title)
                            .setMessage(R.string.clear_history_dialog_message)
                            .setPositiveButton(R.string.conversation_action_history_clear) { _: DialogInterface?, _: Int ->
                                // Clear history and display a snack-bar to display success.
                                mConversationFacade.clearHistory(conversation.accountId, callUri).subscribe()
                                Snackbar.make(
                                    root, R.string.clear_history_completed, Snackbar.LENGTH_LONG
                                ).show()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()
                    }
                )
            }

            // Setup call actions (audio only)
            adapter.actions.add(
                ContactAction(R.drawable.baseline_call_24, getText(R.string.ab_action_audio_call)) {
                    (activity as ContactDetailsActivity)
                        .goToCallActivity(conversation, callUri, false)
                }
            )

            // Setup video call action
            adapter.actions.add(
                ContactAction(
                    R.drawable.baseline_videocam_24, getText(R.string.ab_action_video_call)
                ) {
                    (activity as ContactDetailsActivity)
                        .goToCallActivity(conversation, callUri, true)
                }
            )

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

    /**
     * Set the color of the color action button.
     */
    override fun setColor(color: Int) {
        colorAction?.iconTint = color
        adapter.notifyItemChanged(colorActionPosition)
    }

    /**
     * Set the symbol of the symbol action button.
     */
    private fun setSymbol(symbol: CharSequence) {
        symbolAction?.setSymbol(symbol) // Update emoji action icon
        adapter.notifyItemChanged(symbolActionPosition)
    }

    /**
     * Set the conversation preferences.
     * Always resend the color and emoji, even if they are not changed (to not reset).
     */
    private fun setConversationPreferences(
        accountId: String,
        conversationUri: Uri,
        color: Int? = colorAction?.iconTint,
        emoji: String = symbolAction?.iconSymbol.toString(),
    ) {
        mConversationFacade.setConversationPreferences(
            accountId, conversationUri, mapOf(
                Conversation.KEY_PREFERENCE_CONVERSATION_SYMBOL to emoji,
                Conversation.KEY_PREFERENCE_CONVERSATION_COLOR to if (color != null)
                    String.format("#%06X", 0xFFFFFF and color) else ""
            )
        )
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
