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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.ColorChooserBottomSheet
import cx.ring.client.ConversationDetailsActivity.Companion.EXIT_REASON
import cx.ring.client.ConversationDetailsActivity.Companion.ExitReason
import cx.ring.client.EmojiChooserBottomSheet
import cx.ring.databinding.DialogSwarmTitleBinding
import cx.ring.databinding.FragConversationActionsBinding
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.utils.ActionHelper
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.utils.TextUtils.copyAndShow
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.utils.Log
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
        conversationIdPanel.setOnClickListener {
            copyAndShow(requireContext(), getString(R.string.swarm_id), path.conversationId)
        }
        userNamePanel.setOnClickListener {
            copyAndShow(
                requireContext(),
                getString(R.string.clip_contact_uri), binding?.userName?.text.toString()
            )
        }
        identifierPanel.setOnClickListener {
            copyAndShow(
                requireContext(),
                getString(R.string.clip_contact_uri), binding?.identifier?.text.toString()
            )
        }

        conversationId.text = conversationUri
        conversationPath = path

        descriptionPanel.setOnClickListener {
            if (!conversation.isUserGroupAdmin()) {
                Toast.makeText(
                    requireContext(),
                    R.string.not_admin_toast,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
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

        muteSwitch.setOnClickListener{
            val isMuted = muteSwitch.isChecked
            mConversationFacade.setConversationPreferences(
                path.accountId,
                path.conversationUri,
                mapOf(Conversation.KEY_PREFERENCE_CONVERSATION_NOTIFICATION to (!isMuted).toString())
            )
            if (!path.conversationUri.isSwarm) conversation.setNotification(!isMuted)
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

        // Update mute switch on RX signal.
        mDisposableBag.add(conversation.isNotificationEnabledObservable
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { muteSwitch.isChecked = !it })

        // Setup card with
        //  - conversation type (such as "Private swarm")
        //  - conversation id (such as swarm:1234)"
        // The real conversation mode is hidden in TrustRequest when it's a request.
        val conversationMode =
            if (conversation.mode.blockingFirst() == Conversation.Mode.Request)
                conversation.request!!.mode
            else conversation.mode.blockingFirst()

        if (conversationMode == Conversation.Mode.OneToOne || conversation.isLegacy()) {
            mDisposableBag.add(
                conversation.contactUpdates
                    // Filter out the user.
                    .map { contacts -> contacts.filterNot { it.isUser } }
                    .filter(List<Contact>::isNotEmpty)
                    .map { it.first() }
                    .flatMapSingle { contact ->
                        contact.username?.map { username -> Pair(username, contact.uri) }
                            ?: Single.just(Pair("", contact.uri))
                    }.observeOn(DeviceUtils.uiScheduler)
                    .subscribe { (registeredName, identifier) ->
                        userNamePanel.isVisible = registeredName.isNotEmpty()
                        userName.text = registeredName
                        this.identifier.text = identifier.uri
                        shareButton.setOnClickListener {
                            shareContact(registeredName.ifEmpty { identifier.uri })
                        }
                        qrCode.setOnClickListener { showContactQRCode(identifier) }
                    }
            )
            conversationDelete.setOnClickListener {
                if (conversation.isLegacy())
                    ActionHelper.launchAddContactAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        contact = conversation.contact!!
                    ) { accountId: String, contactUri: Uri ->
                        mAccountService.addContact(accountId, contactUri.uri)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.CONTACT_ADDED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
                else if (conversation.mode.blockingFirst() == Conversation.Mode.Request)
                    ActionHelper.launchAcceptInvitation(
                        context = requireContext(),
                        conversation = conversation
                    ) {
                        mConversationFacade.acceptRequest(it)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.INVITATION_ACCEPTED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
                else
                    ActionHelper.launchDeleteSwarmOneToOneAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        uri = conversation.uri,
                        callback = { accountId: String, conversationUri: Uri ->
                            mConversationFacade.removeConversation(accountId, conversationUri)
                                .subscribe().apply { mDisposableBag.add(this) }
                            val resultIntent = Intent()
                                .putExtra(EXIT_REASON, ExitReason.CONTACT_DELETED.toString())
                            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                            requireActivity().finish()
                        })
            }

            descriptionPanel.isVisible = false  // Disable description edit for 1-to-1 conversation
            blockContact.setOnClickListener {
                if(conversation.mode.blockingFirst()==Conversation.Mode.Request)
                    ActionHelper.launchBlockContactAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        contact = conversation.contact!!
                    ) { accountId: String, _: Uri ->
                        mConversationFacade.blockConversation(accountId, conversation.uri)
                        mConversationFacade.discardRequest(accountId, conversation.uri)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.CONTACT_BLOCKED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
                else if (conversation.contact!!.isBlocked)
                    ActionHelper.launchUnblockContactAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        contact = conversation.contact!!,
                    ) { accountId: String, contactUri: Uri ->
                        mAccountService.addContact(accountId, contactUri.uri)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.CONTACT_UNBLOCKED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
                else
                    ActionHelper.launchBlockContactAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        contact = conversation.contact!!,
                    ) { accountId: String, contactUri: Uri ->
                        mAccountService.removeContact(accountId, contactUri.uri, true)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.CONTACT_BLOCKED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
            }

            // Hide details not useful for blocked contact
            if (conversation.contact!!.isBlocked) {
                conversationDelete.isVisible = false
                blockContact.text = resources.getString(R.string.conversation_action_unblock_this)
                conversationDetailsPanel.visibility = View.GONE
                conversationActionsPanel.visibility = View.GONE
            }

            // Also going there for SIP account
            if (conversation.isLegacy()) {
                blockContact.isVisible = false
                conversationActionsPanel.visibility = View.GONE
                conversationDetailsPanel.visibility = View.GONE
                conversationDelete.isVisible = !mAccountService.currentAccount!!.isSip
                conversationDelete.text = resources.getString(R.string.ab_action_contact_add)
            } else if (conversation.mode.blockingFirst() == Conversation.Mode.Request) {
                conversationDelete.text = resources.getString(R.string.accept_invitation)
                conversationActionsPanel.visibility = View.GONE
            } else {
                conversationDelete.text = resources.getString(R.string.delete_contact)
            }
        } else {    // If conversation mode is not one to one
            privateConversationPanel.isVisible = false
            userNamePanel.isVisible = false
            conversationDelete.text = resources.getString(R.string.leave_conversation)
            conversationDelete.setOnClickListener {
                if (conversation.mode.blockingFirst() == Conversation.Mode.Request)
                    ActionHelper.launchAcceptInvitation(
                        context = requireContext(),
                        conversation = conversation
                    ) {
                        mConversationFacade.acceptRequest(it)
                        val resultIntent = Intent()
                            .putExtra(EXIT_REASON, ExitReason.INVITATION_ACCEPTED.toString())
                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                        requireActivity().finish()
                    }
                else
                    ActionHelper.launchDeleteSwarmGroupAction(
                        context = requireContext(),
                        accountId = mAccountService.currentAccount!!.accountId,
                        uri = conversation.uri,
                        callback = { accountId: String, conversationUri: Uri ->
                            mConversationFacade.removeConversation(accountId, conversationUri)
                                .subscribe().apply { mDisposableBag.add(this) }
                            val resultIntent = Intent()
                                .putExtra(EXIT_REASON, ExitReason.CONVERSATION_LEFT.toString())
                            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                            requireActivity().finish()
                        })
            }

            if (conversation.mode.blockingFirst() == Conversation.Mode.Request) {
                conversationDelete.text = resources.getString(R.string.accept_invitation)
                conversationActionsPanel.visibility = View.GONE
            }

            blockContact.isVisible = false
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

    private fun shareContact(displayName: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_contact_intent_title))
        sharingIntent.putExtra(
            Intent.EXTRA_TEXT,
            getString(
                R.string.share_contact_intent_body,
                displayName,
                getText(R.string.app_website)
            )
        )
        startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)))
    }

    private fun showContactQRCode(contactUri: Uri) {
        QRCodeFragment.newInstance(
            QRCodeFragment.MODE_SHARE,
            contactUri = contactUri
        ).show(parentFragmentManager, QRCodeFragment::class.java.simpleName)
    }

    companion object {
        val TAG = ConversationActionsFragment::class.simpleName!!
        fun newInstance(accountId: String, conversationId: Uri) =
            ConversationActionsFragment().apply {
                arguments = ConversationPath.toBundle(accountId, conversationId)
            }
    }
}
