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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.CertificateViewerActivity
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
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.model.ConversationActionsState
import net.jami.model.ConversationActionsState.BlockAction
import net.jami.model.ConversationActionsState.DeleteAction
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
        binding = this
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

        var actionsState = ConversationActionsState.from(
            conversation, mAccountService.getAccount(path.accountId)
        )
        mDisposableBag.add(ConversationActionsState.observe(
            conversation, mAccountService.observableAccountList
        )
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ state ->
                actionsState = state
                renderActions(state)
            }, { error -> Log.e(TAG, "Unable to update conversation actions", error) }))

        secureP2pConnection.setOnClickListener {
            val identifier = actionsState.contactUri ?: return@setOnClickListener
            startActivity(Intent(Intent.ACTION_VIEW, ConversationPath.toUri(
                conversation.accountId,
                identifier.rawRingId
            )).setClass(requireContext(), CertificateViewerActivity::class.java))
        }
        shareButton.setOnClickListener {
            val identifier = actionsState.contactUri ?: return@setOnClickListener
            shareContact(actionsState.registeredName.ifEmpty { identifier.uri })
        }
        qrCode.setOnClickListener {
            actionsState.contactUri?.let { showContactQRCode(it) }
        }

        conversationDelete.setOnClickListener {
            if (!actionsState.showDelete) return@setOnClickListener
            when (actionsState.deleteAction) {
                DeleteAction.ADD_CONTACT -> {
                    val contact = conversation.contact ?: return@setOnClickListener
                    ActionHelper.launchAddContactAction(
                        context = requireContext(),
                        accountId = path.accountId,
                        contact = contact
                    ) { accountId: String, contactUri: Uri ->
                        mAccountService.addContact(accountId, contactUri.uri)
                        finishWithResult(ExitReason.CONTACT_ADDED)
                    }
                }
                DeleteAction.ACCEPT_INVITATION ->
                    mDisposableBag.add(ActionHelper.launchAcceptInvitation(
                        context = requireContext(),
                        conversation = conversation
                    ) {
                        mConversationFacade.acceptRequest(it)
                        finishWithResult(ExitReason.INVITATION_ACCEPTED)
                    })
                DeleteAction.DELETE_CONTACT ->
                    ActionHelper.launchDeleteSwarmOneToOneAction(
                        context = requireContext(),
                        accountId = path.accountId,
                        uri = conversation.uri,
                        callback = { accountId: String, conversationUri: Uri ->
                            mConversationFacade.removeConversation(accountId, conversationUri)
                                .subscribe().apply { mDisposableBag.add(this) }
                            finishWithResult(ExitReason.CONTACT_DELETED)
                        })
                DeleteAction.LEAVE_CONVERSATION ->
                    ActionHelper.launchDeleteSwarmGroupAction(
                        context = requireContext(),
                        accountId = path.accountId,
                        uri = conversation.uri,
                        callback = { accountId: String, conversationUri: Uri ->
                            mConversationFacade.removeConversation(accountId, conversationUri)
                                .subscribe().apply { mDisposableBag.add(this) }
                            finishWithResult(ExitReason.CONVERSATION_LEFT)
                        })
            }
        }

        blockContact.setOnClickListener {
            val contact = mAccountService.getBlockableContact(conversation)
            if (contact == null || actionsState.blockAction == BlockAction.NONE) {
                ActionHelper.showBlockContactRefused(requireContext())
                return@setOnClickListener
            }
            val accountId = conversation.accountId
            if (actionsState.blockAction == BlockAction.UNBLOCK) {
                mDisposableBag.add(ActionHelper.launchUnblockContactAction(
                    context = requireContext(),
                    accountId = accountId,
                    contact = contact
                ) { accountId: String, contactUri: Uri ->
                    mAccountService.addContact(accountId, contactUri.uri)
                    finishWithResult(ExitReason.CONTACT_UNBLOCKED)
                })
            } else {
                val request = actionsState.deleteAction == DeleteAction.ACCEPT_INVITATION
                mDisposableBag.add(ActionHelper.launchBlockContactAction(
                    context = requireContext(),
                    accountId = accountId,
                    contact = contact,
                    accountService = mAccountService
                ) { accountId: String, contactUri: Uri ->
                    val operation = if (request)
                        mConversationFacade.blockAndDiscardRequest(accountId, conversation.uri, contactUri)
                    else mAccountService.blockContact(accountId, contactUri)
                    mDisposableBag.add(operation
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe({
                            if (binding === this)
                                finishWithResult(ExitReason.CONTACT_BLOCKED)
                        }, { error ->
                            Log.e(TAG, "Unable to block contact", error)
                            if (binding === this)
                                Toast.makeText(requireContext(), R.string.generic_error, Toast.LENGTH_LONG).show()
                        }))
                })
            }
        }

        conversationRemove.setOnClickListener {
            if (!actionsState.showRemove) return@setOnClickListener
            ActionHelper.launchClearAction(
                context = requireContext(),
                accountId = path.accountId,
                uri = conversation.uri,
                callback = { accountId: String, conversationUri: Uri ->
                    mConversationFacade.removeConversation(accountId, conversationUri, true)
                        .subscribe().apply { mDisposableBag.add(this) }
                    finishWithResult(ExitReason.CONVERSATION_LEFT)
                })
        }
        renderActions(actionsState)
    }.root

    private fun FragConversationActionsBinding.renderActions(state: ConversationActionsState) {
        privateConversationPanel.isVisible = state.showPrivate
        userNamePanel.isVisible = state.showUsername
        userName.text = state.registeredName
        identifier.text = state.contactUri?.uri.orEmpty()
        secureP2pConnection.isClickable = state.contactUri != null
        descriptionPanel.isVisible = state.showDescription
        conversationDetailsPanel.isVisible = state.showDetails
        conversationActionsPanel.isVisible = state.showActions
        conversationDelete.isVisible = state.showDelete
        conversationRemove.isVisible = state.showRemove
        blockContact.isVisible = state.blockAction != BlockAction.NONE
        blockContact.setText(if (state.blockAction == BlockAction.UNBLOCK)
            R.string.conversation_action_unblock_this else R.string.conversation_action_block_this)
        conversationDelete.setText(when (state.deleteAction) {
            DeleteAction.ADD_CONTACT -> R.string.ab_action_contact_add
            DeleteAction.ACCEPT_INVITATION -> R.string.accept_invitation
            DeleteAction.DELETE_CONTACT -> R.string.delete_contact
            DeleteAction.LEAVE_CONVERSATION -> R.string.leave_conversation
        })
        conversationType.setText(when (state.type) {
            ConversationActionsState.Type.CONTACT -> R.string.conversation_type_contact
            ConversationActionsState.Type.PRIVATE -> R.string.conversation_type_private
            ConversationActionsState.Type.GROUP -> R.string.conversation_type_group
        })
    }

    private fun finishWithResult(reason: ExitReason) {
        requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(EXIT_REASON, reason.toString()))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        binding = null
        mDisposableBag.clear()
        super.onDestroyView()
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
