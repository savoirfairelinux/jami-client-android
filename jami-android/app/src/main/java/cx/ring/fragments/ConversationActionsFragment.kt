/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.ColorChooserBottomSheet
import cx.ring.client.EmojiChooserBottomSheet
import cx.ring.databinding.DialogSwarmTitleBinding
import cx.ring.databinding.FragConversationActionsBinding
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
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
    lateinit var mConversationFacade: ConversationFacade

    @Inject
    @Singleton
    lateinit var mAccountService: AccountService

    private var binding: FragConversationActionsBinding? = null
    private val mDisposableBag = CompositeDisposable()
    private lateinit var conversationPath: ConversationPath

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragConversationActionsBinding.inflate(inflater, container, false).apply {

        val path = ConversationPath.fromBundle(arguments)!!
        val conversation = mConversationFacade
            .startConversation(path.accountId, path.conversationUri)
            .blockingGet()

        val conversationUri = conversation.uri.toString()
        conversationIdPanel.setOnClickListener { copyAndShow(path.conversationId) }
        conversationId.text = conversationUri
        conversationPath = path

        descriptionPanel.setOnClickListener {
            val dialogBinding = DialogSwarmTitleBinding.inflate(LayoutInflater.from(requireContext())).apply {
                titleTxt.setText(conversation.profile.blockingFirst().description)
                titleTxtBox.hint = getString(R.string.dialog_hint_description)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setTitle(getString(R.string.dialogtitle_description))
                .setPositiveButton(R.string.rename_btn) { d, _: Int ->
                    val input = dialogBinding.titleTxt.text.toString().trim { it <= ' ' }
                    mAccountService.updateConversationInfo(
                        conversationPath.accountId,
                        conversationPath.conversationUri.host,
                        mapOf("description" to input))
                    d.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        colorPickPanel.setOnClickListener {
            ColorChooserBottomSheet { color -> // Color chosen by the user (onclick method).
                val rgbColor = String.format("#%06X", 0xFFFFFF and color)
                mConversationFacade.setConversationPreferences(
                    path.accountId,
                    path.conversationUri,
                    mapOf(Conversation.KEY_PREFERENCE_CONVERSATION_COLOR to rgbColor)
                )
                // Need to manually update the color of the conversation as will not get the
                // update signal from daemon.
                if (!path.conversationUri.isSwarm) conversation.setColor(color)
            }.show(parentFragmentManager, "colorChooser")
        }

        emojiPickPanel.setOnClickListener {
            EmojiChooserBottomSheet { emoji -> // Emoji chosen by the user (onclick method).
                if (emoji == null) return@EmojiChooserBottomSheet
                mConversationFacade.setConversationPreferences(
                    path.accountId,
                    path.conversationUri,
                    mapOf(Conversation.KEY_PREFERENCE_CONVERSATION_SYMBOL to emoji)
                )
                // Need to manually update the symbol of the conversation as will not get the
                // update signal from daemon.
                if (!path.conversationUri.isSwarm) conversation.setSymbol(emoji.toString())
            }.show(parentFragmentManager, "emojiChooser")
        }

        mDisposableBag.add(conversation.profile
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { profile -> description.text = profile.description })

        // Update color on RX color signal.
        mDisposableBag.add(conversation.getColor()
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { colorPick.setColorFilter(getConversationColor(requireContext(), it)) })

        // Update emoji symbol on RX color signal.
        mDisposableBag.add(conversation.getSymbol()
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { emojiPick.text = getConversationSymbol(requireContext(), it) })

        // Setup card with
        //  - conversation type (such as "Private swarm")
        //  - conversation id (such as swarm:1234)"
        // The real conversation mode is hidden in TrustRequest when it's a request.
        val conversationMode =
            if (conversation.mode.blockingFirst() == Conversation.Mode.Request)
                conversation.request!!.mode
            else conversation.mode.blockingFirst()

        if (conversationMode == Conversation.Mode.OneToOne) {
            conversationDelete.text = resources.getString(R.string.delete_conversation)
            conversationDelete.setOnClickListener {  }

            descriptionPanel.isVisible = false  // disable description edit for 1-to-1 conversation
            //descriptionPanel.setOnClickListener(null)

            blockSwitch.isChecked = conversation.contact!!.isBanned
            blockSwitchPanel.setOnClickListener {
                val ctx = requireContext()
                val builder = MaterialAlertDialogBuilder(ctx)
                if (blockSwitch.isChecked) {    // the contact is already blocked
                    builder.setTitle(getString(R.string.unblock_contact_dialog_title, conversationUri))
                    builder.setMessage(getString(R.string.unblock_contact_dialog_message, conversationUri))
                    builder.setPositiveButton(R.string.conversation_action_unblock_this) { _, _ ->
                        // Unblock the contact and display a toast
                        mAccountService.addContact(conversation.accountId, conversation.contact!!.uri.rawRingId)
                        Snackbar.make(root,
                            getString(R.string.unblock_contact_completed, conversationUri),
                            Snackbar.LENGTH_LONG
                        ).show()
                        blockSwitch.isChecked = false
                    }
                } else {
                    builder.setTitle(getString(R.string.block_contact_dialog_title, conversationUri))
                    builder.setMessage(getString(R.string.block_contact_dialog_message, conversationUri))
                    builder.setPositiveButton(R.string.conversation_action_block_this) { _, _ ->
                        // Block the conversation and display a toast
                        mConversationFacade.banConversation(conversation.accountId, conversation.uri)
                        Snackbar.make(root,
                            getString(R.string.block_contact_completed, conversationUri),
                            Snackbar.LENGTH_LONG
                        ).show()
                        blockSwitch.isChecked = true
                    }
                }
                builder.setNegativeButton(android.R.string.cancel, null).show()
            }
        } else {    // If conversation mode is not one to one
            conversationDelete.text = resources.getString(R.string.leave_swarm)
            blockSwitchPanel.isVisible = false
        }

        @StringRes val infoString =
            if (conversation.isSwarm) {
                if (conversationMode == Conversation.Mode.OneToOne)
                    R.string.conversation_type_private
                else {
                    R.string.conversation_type_group
                }
            } else R.string.conversation_type_contact

        conversationType.setText(infoString)

//        val callUri: Uri
//        if (conversationMode == Conversation.Mode.OneToOne) {
//            callUri = conversation.contact!!.uri
//
//        } else {
//            callUri = conversation.uri
//        }

//            if (!conversation.isSwarm) {
//                // Setup clear history action
//                adapter.actions.add(
//                    ContactAction(
//                        R.drawable.baseline_clear_all_24,
//                        getText(R.string.conversation_action_history_clear)
//                    ) {
//                        MaterialAlertDialogBuilder(requireContext())
//                            .setTitle(R.string.clear_history_dialog_title)
//                            .setMessage(R.string.clear_history_dialog_message)
//                            .setPositiveButton(R.string.conversation_action_history_clear) { _: DialogInterface?, _: Int ->
//                                // Clear history and display a snack-bar to display success.
//                                mConversationFacade.clearHistory(conversation.accountId, callUri).subscribe()
//                                Snackbar.make(
//                                    root, R.string.clear_history_completed, Snackbar.LENGTH_LONG
//                                ).show()
//                            }
//                            .setNegativeButton(android.R.string.cancel, null)
//                            .create()
//                            .show()
//                    }
//                )
//            }

        binding = this
    }.root

    override fun onDestroy() {
        binding = null
        mDisposableBag.dispose()
        super.onDestroy()
    }

    private fun copyAndShow(toCopy: String) {
        val clipboard = requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), toCopy))
        Snackbar.make(binding!!.root, getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy), Snackbar.LENGTH_LONG).show()
    }

    companion object {
        val TAG = ConversationActionsFragment::class.simpleName!!
        fun newInstance(accountId: String, conversationId: Uri) =
            ConversationActionsFragment().apply {
                arguments = ConversationPath.toBundle(accountId, conversationId)
            }
    }
}
