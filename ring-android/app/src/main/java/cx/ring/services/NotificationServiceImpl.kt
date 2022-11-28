/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RequiresApi
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.notification.CarNotificationManager
import androidx.core.app.*
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.client.CallActivity
import cx.ring.client.ConversationActivity
import cx.ring.client.HomeActivity
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.service.CallNotificationService
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.tv.call.TVCallActivity
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarFactory
import net.jami.model.*
import net.jami.model.Interaction.InteractionStatus
import net.jami.services.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class NotificationServiceImpl(
    private val mContext: Context,
    private val mAccountService: AccountService,
    private val mContactService: ContactService,
    private val mPreferencesService: PreferencesService,
    private val mDeviceRuntimeService: DeviceRuntimeService
) : NotificationService {
    private val mNotificationBuilders = SparseArray<NotificationCompat.Builder>()
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(mContext)
    private val random = Random()
    private val avatarSize = (mContext.resources.displayMetrics.density * AvatarFactory.SIZE_NOTIF).toInt()
    private val currentCalls = LinkedHashMap<String, Conference>()
    private val callNotifications = ConcurrentHashMap<Int, Notification>()
    private val dataTransferNotifications = ConcurrentHashMap<Int, Notification>()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannels(mContext, notificationManager)
        }
    }

    /**
     * Starts the call activity directly for Android TV
     *
     * @param callId the call ID
     */
    private fun startCallActivity(callId: String) {
        mContext.startActivity(Intent(Intent.ACTION_VIEW)
                .putExtra(NotificationService.KEY_CALL_ID, callId)
                .setClass(mContext.applicationContext, TVCallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun buildCallNotification(conference: Conference): Notification? {
        var ongoingCallId: String? = null
        for (conf in currentCalls.values) {
            if (conf !== conference && conf.state == Call.CallStatus.CURRENT)
                ongoingCallId = conf.participants[0].daemonIdString
        }
        val call = conference.firstCall!!
        val viewIntent = PendingIntent.getActivity(mContext, random.nextInt(), Intent(Intent.ACTION_VIEW)
            .setClass(mContext, if (DeviceUtils.isTv(mContext)) TVCallActivity::class.java else CallActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString), ContentUriHandler.immutable())

        val contact = getProfile(call.account!!, call.contact!!)

        val messageNotificationBuilder: NotificationCompat.Builder
        if (conference.isOnGoing) {
            messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS)
                .setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.displayName))
                .setContentText(mContext.getText(R.string.notif_current_call))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(viewIntent)
                .setSound(null)
                .setVibrate(null)
                .setColorized(true)
                .setUsesChronometer(true)
                .setWhen(conference.timestampStart)
                .setColor(ContextCompat.getColor(mContext, R.color.color_primary_light))
                .addAction(R.drawable.baseline_call_end_24,
                    mContext.getText(R.string.action_call_hangup),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_CALL_END)
                            .setClass(mContext, DRingService::class.java)
                            .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString)
                            .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account),

            ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
        } else if (conference.isRinging) {
            if (conference.isIncoming) {
                messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_INCOMING_CALL)
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.displayName))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentText(mContext.getText(R.string.notif_incoming_call))
                    .setContentIntent(viewIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setFullScreenIntent(viewIntent, true)
                    .addAction(
                        R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_decline),
                        PendingIntent.getService(mContext, random.nextInt(), Intent(DRingService.ACTION_CALL_REFUSE)
                            .setClass(mContext, DRingService::class.java)
                            .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account)
                            .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))

                if (conference.hasVideo()){
                        messageNotificationBuilder
                            .addAction(R.drawable.baseline_videocam_24, if (ongoingCallId == null) mContext.getText(R.string.action_call_accept_video) else mContext.getText(R.string.action_call_hold_accept_video),
                                PendingIntent.getService(mContext, random.nextInt(), Intent(if (ongoingCallId == null) DRingService.ACTION_CALL_ACCEPT else DRingService.ACTION_CALL_HOLD_ACCEPT)
                                    .setClass(mContext, DRingService::class.java)
                                    .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account)
                                    .putExtra(NotificationService.KEY_HOLD_ID, ongoingCallId)
                                    .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString)
                                    .putExtra(CallFragment.KEY_HAS_VIDEO, true), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))

                    } else {
                        messageNotificationBuilder.addAction(
                            R.drawable.baseline_call_24, if (ongoingCallId == null) mContext.getText(R.string.action_call_accept_audio) else mContext.getText(R.string.action_call_end_accept),
                            PendingIntent.getService(mContext, random.nextInt(), Intent(if (ongoingCallId == null) DRingService.ACTION_CALL_ACCEPT else DRingService.ACTION_CALL_END_ACCEPT)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account)
                                .putExtra(NotificationService.KEY_END_ID, ongoingCallId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString)
                                .putExtra(CallFragment.KEY_HAS_VIDEO, false), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT))
                        )
                    }
                if (ongoingCallId != null) {
                    messageNotificationBuilder.addAction(R.drawable.baseline_call_24,
                        mContext.getText(R.string.action_call_hold_accept),
                        PendingIntent.getService(mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_HOLD_ACCEPT)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account)
                                .putExtra(NotificationService.KEY_HOLD_ID, ongoingCallId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                            ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                }
            } else {
                messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS)
                    .setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.displayName))
                    .setContentText(mContext.getText(R.string.notif_outgoing_call))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(viewIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setColorized(true)
                    .setColor(ContextCompat.getColor(mContext, R.color.color_primary_light))
                    .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_hangup),
                        PendingIntent.getService(mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_END)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account),
                            ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
            }
        } else {
            return null
        }
        messageNotificationBuilder.setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
        setContactPicture(contact, messageNotificationBuilder)
        return messageNotificationBuilder.build().apply {
            if (conference.isRinging)
                flags = flags or NotificationCompat.FLAG_INSISTENT
        }
    }

    override fun showCallNotification(notifId: Int): Any? = callNotifications.remove(notifId)

    override fun showLocationNotification(first: Account, contact: Contact) {
        val path = ConversationPath.toUri(first.accountId, contact.uri)
        val profile = getProfile(first.accountId, contact)

        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true)
        val messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setLargeIcon(getContactPicture(profile))
            .setContentText(mContext.getString(R.string.location_share_contact, profile.displayName))
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUriHandler.immutable()))
            .setAutoCancel(false)
            .setColor(ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null))
        notificationManager.notify(
            Objects.hash("Location", path),
            messageNotificationBuilder.build()
        )
    }

    override fun cancelLocationNotification(first: Account, contact: Contact) {
        notificationManager.cancel(
            Objects.hash("Location", ConversationPath.toUri(first.accountId, contact.uri))
        )
    }

    /**
     * Updates a notification
     *
     * @param notification   a built notification object
     * @param notificationId the notification's id
     */
    private fun updateNotification(notification: Notification, notificationId: Int) {
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Handles the creation and destruction of services associated with calls as well as displaying notifications.
     *
     * @param conference the conference object for the notification
     * @param remove     true if it should be removed from current calls
     */
    override fun handleCallNotification(conference: Conference, remove: Boolean) {
        if (DeviceUtils.isTv(mContext)) {
            if (!remove) startCallActivity(conference.id)
            return
        }
        var notification: Notification? = null

        // Build notification
        val id = conference.id
        currentCalls.remove(id)
        if (!remove) {
            currentCalls[id] = conference
            notification = buildCallNotification(conference)
        }
        if (notification == null && currentCalls.isNotEmpty()) {
            // Build notification for other calls if any remains
            //for (c in currentCalls.values) conference = c
            notification = buildCallNotification(currentCalls.values.last())
        }

        // Send notification to the  Service
        if (notification != null) {
            val nid = random.nextInt()
            callNotifications[nid] = notification
            try {
                ContextCompat.startForegroundService(mContext,
                    Intent(CallNotificationService.ACTION_START, null, mContext, CallNotificationService::class.java)
                        .putExtra(NotificationService.KEY_NOTIFICATION_ID, nid))
            } catch (e: Exception) {
                Log.w(TAG, "Can't show call notification", e)
            }
        } else {
            removeCallNotification(0)
        }
    }

    override fun removeCallNotification(notifId: Int) {
        try {
            mContext.startService(Intent(CallNotificationService.ACTION_STOP, null, mContext, CallNotificationService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping service", e)
        }
    }

    override fun onConnectionUpdate(b: Boolean) {
        /*Log.i(TAG, "onConnectionUpdate " + b);
        if (b) {
            Intent serviceIntent = new Intent(SyncService.ACTION_START).setClass(mContext, SyncService.class);
            try {
                ContextCompat.startForegroundService(mContext, serviceIntent);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error starting service", e);
            }
        } else {
            try {
                mContext.startService(new Intent(SyncService.ACTION_STOP).setClass(mContext, SyncService.class));
            } catch (IllegalStateException ignored) {
            }
        }*/
    }

    /**
     * Handles the creation and destruction of services associated with transfers as well as displaying notifications.
     *
     * @param transfer the data transfer object
     * @param conversation  the contact to whom the data transfer is being sent
     * @param remove   true if it should be removed from current calls
     */
    override fun handleDataTransferNotification(transfer: DataTransfer, conversation: Conversation, remove: Boolean) {
        Log.d(TAG, "handleDataTransferNotification, a data transfer event is in progress $remove")
        if (DeviceUtils.isTv(mContext))
            return
        if (!remove) {
            showFileTransferNotification(conversation, transfer)
        } else {
            removeTransferNotification(ConversationPath.toUri(conversation), transfer.fileId ?: transfer.id.toString())
        }
    }

    override fun removeTransferNotification(accountId: String, conversationUri: net.jami.model.Uri, fileId: String) {
        removeTransferNotification(ConversationPath.toUri(accountId, conversationUri), fileId)
    }

    /**
     * Cancels a data transfer notification and removes it from the list of notifications
     *
     * @param fileId the transfer id which is required to generate the notification id
     */
    private fun removeTransferNotification(path: Uri, fileId: String) {
        val id = getFileTransferNotificationId(path, fileId)
        try {
            mContext.startService(Intent(DataTransferService.ACTION_STOP, path, mContext, DataTransferService::class.java)
                    .putExtra(NotificationService.KEY_NOTIFICATION_ID, id))
        } catch (e: Exception) {
            Log.d(TAG, "Error stopping transfer service ${e.message}")
        }
    }

    /**
     * @param notificationId the notification id
     * @return the notification object for a data transfer notification
     */
    override fun getDataTransferNotification(notificationId: Int): Notification? {
        return dataTransferNotifications[notificationId]
    }

    override fun showTextNotification(accountId: String, conversation: Conversation) {
        val texts = conversation.unreadTextMessages

        //Log.w(TAG, "showTextNotification start " + accountId + " " + conversation.getUri() + " " + texts.size());

        //TODO handle groups
        if (texts.isEmpty() || conversation.isVisible) {
            cancelTextNotification(conversation.accountId, conversation.uri)
            return
        }
        if (texts.lastEntry().value.isNotified) {
            return
        }
        Log.w(TAG, "showTextNotification " + accountId + " " + conversation.uri)
        mContactService.getLoadedContact(accountId, conversation.contacts, false)
            .subscribe({contacts -> textNotification(accountId, texts, conversation, contacts) })
            { e: Throwable -> Log.w(TAG, "Can't load contact", e) }
    }

    private fun textNotification(
        accountId: String,
        texts: TreeMap<Long, TextMessage>,
        conversation: Conversation,
        contacts: List<ContactViewModel>
    ) {
        val cpath = ConversationPath(conversation)
        val path = cpath.toUri()
        val key = cpath.toKey()
        val conversationProfile = getProfile(conversation) ?: return
        var notificationVisibility = mPreferencesService.settings.notificationVisibility
        notificationVisibility = when (notificationVisibility) {
            SettingsFragment.NOTIFICATION_PUBLIC -> Notification.VISIBILITY_PUBLIC
            SettingsFragment.NOTIFICATION_SECRET -> Notification.VISIBILITY_SECRET
            SettingsFragment.NOTIFICATION_PRIVATE -> Notification.VISIBILITY_PRIVATE
            else -> Notification.VISIBILITY_PRIVATE
        }
        val last = texts.lastEntry()?.value
        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val intentDelete = Intent(DRingService.ACTION_CONV_DISMISS, path, mContext, DRingService::class.java)
            .putExtra(DRingService.KEY_MESSAGE_ID, last?.messageId ?: last?.daemonIdString)
        val messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(notificationVisibility)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setContentTitle(conversationProfile.second)
            .setContentText(last?.body)
            .setLocusId(LocusIdCompat(key))
            .setWhen(last?.timestamp ?: 0)
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUriHandler.immutable()))
            .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, ContentUriHandler.immutable()))
            .setAutoCancel(true)
            .setColor(ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null))
        /*val conversationPerson = Person.Builder()
            .setKey(key)
            .setName(conversationProfile.second)
            .setIcon(IconCompat.createWithBitmap(conversationProfile.first))
            .build()*/
        messageNotificationBuilder.setLargeIcon(conversationProfile.first)
        val intentBubble = Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity::class.java)
        intentBubble.putExtra(EXTRA_BUBBLE, true)
        messageNotificationBuilder
            .setBubbleMetadata(NotificationCompat.BubbleMetadata.Builder(
                PendingIntent.getActivity(mContext, 0, intentBubble, ContentUriHandler.mutable(PendingIntent.FLAG_UPDATE_CURRENT)),
                IconCompat.createWithAdaptiveBitmap(conversationProfile.first))
                .setDesiredHeight(600)
                .build())
            //.addPerson(conversationPerson)
            .setShortcutId(key)
        val account = mAccountService.getAccount(accountId)
        val profile = if (account == null) null else VCardServiceImpl.loadProfile(mContext, account).blockingFirst()
        val myPic = account?.let { getContactPicture(it) }
        val userPerson = Person.Builder()
            .setKey(accountId)
            .setName(if (profile == null || TextUtils.isEmpty(profile.displayName)) "You" else profile.displayName)
            .setIcon(if (myPic == null) null else IconCompat.createWithBitmap(myPic))
            .build()
        val history = NotificationCompat.MessagingStyle(userPerson)
        history.isGroupConversation = conversation.isGroup()
        history.conversationTitle = conversationProfile.second
        val persons = HashMap<String, Person>()
        for (contact in contacts) {
            if (contact.contact.isUser) continue
            val contactPicture = getContactPicture(contact)
            val contactPerson = Person.Builder()
                .setKey(ConversationPath.toKey(cpath.accountId, contact.contact.uri.uri))
                .setName(contact.profile.displayName)
                .setIcon(if (contactPicture == null) null else IconCompat.createWithBitmap(contactPicture))
                .build()
            messageNotificationBuilder.addPerson(contactPerson)
            persons[contact.contact.uri.uri] = contactPerson
        }
        for (textMessage in texts.values) {
            val contact = textMessage.contact!!
            val contactPerson = if (contact.isUser) null else persons[contact.uri.uri]
            history.addMessage(NotificationCompat.MessagingStyle.Message(
                textMessage.body,
                textMessage.timestamp,
                contactPerson
            ))
        }
        messageNotificationBuilder.setStyle(history)
        val notificationId = getTextNotificationId(conversation.accountId, conversation.uri)
        val replyId = notificationId + 1
        val markAsReadId = notificationId + 2
        val replyLabel = mContext.getText(R.string.notif_reply)
        val remoteInput = RemoteInput.Builder(DRingService.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        val replyPendingIntent = PendingIntent.getService(mContext, replyId,
            Intent(DRingService.ACTION_CONV_REPLY_INLINE, path, mContext, DRingService::class.java),
            ContentUriHandler.mutable(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val readPendingIntent = PendingIntent.getService(mContext, markAsReadId,
            Intent(DRingService.ACTION_CONV_READ, path, mContext, DRingService::class.java), ContentUriHandler.immutable())
        messageNotificationBuilder
            .extend(CarAppExtender.Builder().build())
            .addAction(NotificationCompat.Action.Builder(R.drawable.baseline_reply_24, replyLabel, replyPendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .addRemoteInput(remoteInput)
                .extend(NotificationCompat.Action.WearableExtender()
                    .setHintDisplayActionInline(true))
                .build())
            .addAction(NotificationCompat.Action.Builder(0, mContext.getString(R.string.notif_mark_as_read), readPendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build())
        CarNotificationManager.from(mContext).notify(notificationId, messageNotificationBuilder)
        mNotificationBuilders.put(notificationId, messageNotificationBuilder)
    }

    private fun getRequestNotificationBuilder(accountId: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_REQUEST)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentTitle(mContext.getString(R.string.contact_request_title))
        val intentOpenTrustRequestFragment = Intent(HomeActivity.ACTION_PRESENT_TRUST_REQUEST_FRAGMENT)
            .setClass(mContext, HomeActivity::class.java)
            .putExtra(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
        builder.setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentOpenTrustRequestFragment,
            ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
        builder.color = ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null)
        return builder
    }

    override fun showIncomingTrustRequestNotification(account: Account) {
        val notificationId = getIncomingTrustNotificationId(account.accountId)
        val notifiedRequests = mPreferencesService.loadRequestsPreferences(account.accountId)
        val requests = account.getPending()
        if (requests.isEmpty()) {
            notificationManager.cancel(notificationId)
            return
        }
        if (requests.size == 1) {
            val request = requests.iterator().next()
            val contactKey = request.uri.rawUriString
            if (notifiedRequests.contains(contactKey)) {
                return
            }
            mContactService.getLoadedConversation(request).subscribe({ vm ->
                val builder = getRequestNotificationBuilder(account.accountId)
                mPreferencesService.saveRequestPreferences(account.accountId, contactKey)
                val info = ConversationPath.toUri(account.accountId, request.uri)
                builder.setContentText(vm.uriTitle)
                    .addAction(R.drawable.baseline_person_add_24, mContext.getText(R.string.accept), PendingIntent.getService(
                        mContext, random.nextInt(),
                        Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT, info, mContext, DRingService::class.java),
                        ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                    .addAction(R.drawable.baseline_delete_24, mContext.getText(R.string.refuse), PendingIntent.getService(
                        mContext, random.nextInt(),
                        Intent(DRingService.ACTION_TRUST_REQUEST_REFUSE, info, mContext, DRingService::class.java),
                        ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                    .addAction(R.drawable.baseline_block_24, mContext.getText(R.string.block), PendingIntent.getService(
                        mContext, random.nextInt(),
                        Intent(DRingService.ACTION_TRUST_REQUEST_BLOCK, info, mContext, DRingService::class.java),
                        ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                getContactPicture(request)?.let { pic -> builder.setLargeIcon(pic) }
                notificationManager.notify(notificationId, builder.build())
            }) { e: Throwable -> Log.w(TAG, "error showing notification", e) }
        } else {
            val builder = getRequestNotificationBuilder(account.accountId)
            var newRequest = false
            for (request in requests) {
                val contact = request.contact
                if (contact != null) {
                    val contactKey = contact.uri.rawRingId
                    if (!notifiedRequests.contains(contactKey)) {
                        newRequest = true
                        mPreferencesService.saveRequestPreferences(account.accountId, contactKey)
                    }
                }
            }
            if (!newRequest) return
            builder.setContentText(String.format(mContext.getString(R.string.contact_request_msg), requests.size))
            builder.setLargeIcon(null)
            notificationManager.notify(notificationId, builder.build())
        }
    }

    @SuppressLint("RestrictedApi")
    override fun showFileTransferNotification(conversation: Conversation, info: DataTransfer) {
        val event = info.status ?: return
        if (event == InteractionStatus.FILE_AVAILABLE)
            return
        val path = ConversationPath.toUri(conversation)
        Log.d(TAG, "showFileTransferNotification $path")
        val dataTransferId = info.fileId ?: info.id.toString()
        val notificationId = getFileTransferNotificationId(path, dataTransferId)
        val intentViewConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
        val profile = getProfile(conversation) ?: return

        if (event.isOver) {
            removeTransferNotification(path, dataTransferId)
            if (info.isOutgoing || info.isError) {
                return
            }
            val notif = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentViewConversation, ContentUriHandler.immutable()))
                .setAutoCancel(true)
            if (info.showPicture()) {
                val filePath = mDeviceRuntimeService.getConversationPath(conversation.uri.rawRingId, info.storagePath)
                val img: Bitmap
                try {
                    val d = Glide.with(mContext)
                        .load(filePath)
                        .submit()
                        .get() as BitmapDrawable
                    img = d.bitmap
                    notif.setContentTitle(mContext.getString(R.string.notif_incoming_picture, profile.second))
                    notif.setStyle(NotificationCompat.BigPictureStyle().bigPicture(img))
                } catch (e: Exception) {
                    Log.w(TAG, "Can't load image for notification", e)
                    return
                }
            } else {
                notif.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title, profile.second))
                notif.setStyle(null)
            }
            val picture = getContactPicture(conversation)
            if (picture != null) notif.setLargeIcon(picture)
            notificationManager.notify(random.nextInt(), notif.build())
            return
        }
        val messageNotificationBuilder = mNotificationBuilders[notificationId] ?: NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER).apply {
            mNotificationBuilders.put(notificationId, this)
        }
        val ongoing = event == InteractionStatus.TRANSFER_ONGOING || event == InteractionStatus.TRANSFER_ACCEPTED
        val titleMessage = mContext.getString(
            if (info.isOutgoing) R.string.notif_outgoing_file_transfer_title else R.string.notif_incoming_file_transfer_title,
            profile.second
        )
        messageNotificationBuilder.setContentTitle(titleMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(ongoing)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setContentText(
                if (event == InteractionStatus.TRANSFER_ONGOING)
                    Formatter.formatFileSize(mContext, info.bytesProgress) + " / " + Formatter.formatFileSize(mContext, info.totalSize)
                else
                    info.displayName + ": " + cx.ring.utils.TextUtils.getReadableFileTransferStatus(mContext, event)
            )
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentViewConversation, ContentUriHandler.immutable()))
            .color = ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null)
        val picture = getContactPicture(conversation)
        if (picture != null) messageNotificationBuilder.setLargeIcon(picture)
        when {
            event.isOver -> messageNotificationBuilder.setProgress(0, 0, false)
            ongoing -> messageNotificationBuilder.setProgress(info.totalSize.toInt(), info.bytesProgress.toInt(), false)
            else -> messageNotificationBuilder.setProgress(0, 0, true)
        }
        if (event == InteractionStatus.TRANSFER_CREATED) {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            mNotificationBuilders.put(notificationId, messageNotificationBuilder)
            // updateNotification(messageNotificationBuilder.build(), notificationId);
            return
        } else {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }
        messageNotificationBuilder.mActions.clear()
        if (event == InteractionStatus.TRANSFER_AWAITING_HOST) {
            messageNotificationBuilder
                .addAction(R.drawable.baseline_call_received_24, mContext.getText(R.string.accept),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_ACCEPT, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                .addAction(R.drawable.baseline_cancel_24, mContext.getText(R.string.refuse),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
            updateNotification(messageNotificationBuilder.build(), notificationId)
            return
        } else if (!event.isOver) {
            messageNotificationBuilder.addAction(R.drawable.baseline_cancel_24, mContext.getText(android.R.string.cancel),
                PendingIntent.getService(mContext, random.nextInt(),
                    Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                        .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
        }
        dataTransferNotifications[notificationId] = messageNotificationBuilder.build()
        try {
            ContextCompat.startForegroundService(mContext,
                Intent(DataTransferService.ACTION_START, path, mContext, DataTransferService::class.java)
                    .putExtra(NotificationService.KEY_NOTIFICATION_ID, notificationId))
        } catch (e: Exception) {
            Log.w(TAG, "Error starting file transfer service " + e.message)
        }
        //startForegroundService(notificationId, DataTransferService.class);
    }

    override fun showMissedCallNotification(call: Call) {
        val notificationId = call.daemonIdString.hashCode()
        val messageNotificationBuilder = mNotificationBuilders[notificationId]
            ?: NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MISSED_CALL)
        val path = ConversationPath.toUri(call)
        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contact = getProfile(call.account!!, call.contact!!)

        messageNotificationBuilder.setContentTitle(mContext.getText(R.string.notif_missed_incoming_call))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.baseline_call_missed_24)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentText(contact.displayName)
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
            .color = ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null)
        setContactPicture(contact, messageNotificationBuilder)
        notificationManager.notify(notificationId, messageNotificationBuilder.build())
    }

    override val serviceNotification: Any
        get() {
            val intentHome = Intent(Intent.ACTION_VIEW)
                .setClass(mContext, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return NotificationCompat.Builder(mContext, NOTIF_CHANNEL_SERVICE)
                .setContentTitle(mContext.getText(R.string.app_name))
                .setContentText(mContext.getText(R.string.notif_background_service))
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, intentHome, ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
        }

    override fun cancelTextNotification(accountId: String, contact: net.jami.model.Uri) {
        val notificationId = getTextNotificationId(accountId, contact)
        notificationManager.cancel(notificationId)
        mNotificationBuilders.remove(notificationId)
    }

    override fun cancelTrustRequestNotification(accountID: String) {
        val notificationId = getIncomingTrustNotificationId(accountID)
        notificationManager.cancel(notificationId)
    }

    override fun cancelCallNotification() {
        notificationManager.cancel(NOTIF_CALL_ID)
        mNotificationBuilders.remove(NOTIF_CALL_ID)
        callNotifications.clear()
    }

    /**\
     * Cancels a notification
     * @param notificationId the notification ID
     */
    override fun cancelFileNotification(notificationId: Int) {
        mNotificationBuilders.remove(notificationId)
    }

    override fun cancelAll() {
        notificationManager.cancelAll()
        mNotificationBuilders.clear()
    }

    private fun getIncomingTrustNotificationId(accountId: String): Int {
        return (NOTIF_TRUST_REQUEST + accountId).hashCode()
    }

    private fun getTextNotificationId(accountId: String, contact: net.jami.model.Uri): Int {
        return (NOTIF_MSG + accountId + contact.toString()).hashCode()
    }

    private fun getFileTransferNotificationId(path: Uri, dataTransferId: String): Int {
        return (NOTIF_FILE_TRANSFER + path.toString() + dataTransferId).hashCode()
    }

    private fun getContactPicture(contact: ContactViewModel): Bitmap? {
        return try {
            AvatarFactory.getBitmapAvatar(mContext, contact, avatarSize, false).blockingGet()
        } catch (e: Exception) {
            null
        }
    }

    private fun getContactPicture(conversation: Conversation): Bitmap? {
        return try {
            mContactService.getLoadedConversation(conversation).flatMap { vm ->
                AvatarFactory.getAvatar(mContext, vm)
                    .map { d -> BitmapUtils.drawableToBitmap(d, avatarSize) }
            }.blockingGet()
        } catch (e: Exception) {
            null
        }
    }

    private fun getContactPicture(account: Account): Bitmap {
        return AvatarFactory.getBitmapAvatar(mContext, account, avatarSize).blockingGet()
    }

    private fun getProfile(conversation: Conversation): Pair<Bitmap, String>? {
        return try {
            mContactService.getLoadedConversation(conversation).map { vm ->
                Pair(AvatarFactory.getAvatar(mContext, vm)
                    .map { d -> BitmapUtils.drawableToBitmap(d, avatarSize) }
                    .blockingGet(), vm.title)
            }.blockingGet()
        } catch (e: Exception) {
            null
        }
    }

    private fun getProfile(accountId:String, contact: Contact): ContactViewModel {
        return mContactService.getLoadedContact(accountId, contact).blockingGet()
    }

    private fun setContactPicture(contact: ContactViewModel, messageNotificationBuilder: NotificationCompat.Builder) {
        getContactPicture(contact)?.let { pic -> messageNotificationBuilder.setLargeIcon(pic) }
    }

    companion object {
        const val EXTRA_BUBBLE = "bubble"
        private val TAG = NotificationServiceImpl::class.java.simpleName
        private const val NOTIF_MSG = "MESSAGE"
        private const val NOTIF_TRUST_REQUEST = "TRUST REQUEST"
        private const val NOTIF_FILE_TRANSFER = "FILE_TRANSFER"
        private const val NOTIF_MISSED_CALL = "MISSED_CALL"
        private const val NOTIF_CHANNEL_CALL_IN_PROGRESS = "current_call"
        private const val NOTIF_CHANNEL_MISSED_CALL = "missed_calls"
        private const val NOTIF_CHANNEL_INCOMING_CALL = "incoming_call2"
        private const val NOTIF_CHANNEL_MESSAGE = "messages"
        private const val NOTIF_CHANNEL_REQUEST = "requests"
        private const val NOTIF_CHANNEL_FILE_TRANSFER = "file_transfer"
        const val NOTIF_CHANNEL_SYNC = "sync"
        private const val NOTIF_CHANNEL_SERVICE = "service"
        private const val NOTIF_CALL_GROUP = "calls"
        const val NOTIF_CALL_ID = 1001

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun registerNotificationChannels(context: Context, notificationManager: NotificationManagerCompat) {
            // Setting up groups
            notificationManager.createNotificationChannelGroup(NotificationChannelGroupCompat.Builder(NOTIF_CALL_GROUP)
                .setName(context.getString(R.string.notif_group_calls))
                .build())

            // Missed calls channel
            val missedCallsChannel = NotificationChannelCompat.Builder(NOTIF_CHANNEL_MISSED_CALL, NotificationManager.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notif_channel_missed_calls))
                .setSound(null, null)
                .setGroup(NOTIF_CALL_GROUP)
                .setVibrationEnabled(false)
                .build()
            // lockscreenVisibility = Notification.VISIBILITY_SECRET
            notificationManager.createNotificationChannel(missedCallsChannel)

            // Incoming call channel
            val incomingCallChannel = NotificationChannel(
                NOTIF_CHANNEL_INCOMING_CALL,
                context.getString(R.string.notif_channel_incoming_calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                group = NOTIF_CALL_GROUP
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000)
            }
            notificationManager.createNotificationChannel(incomingCallChannel)

            // Call in progress channel
            val callInProgressChannel = NotificationChannel(
                NOTIF_CHANNEL_CALL_IN_PROGRESS,
                context.getString(R.string.notif_channel_call_in_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                group = NOTIF_CALL_GROUP
            }
            notificationManager.createNotificationChannel(callInProgressChannel)

            // Text messages channel
            val soundAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()
            val messageChannel = NotificationChannel(NOTIF_CHANNEL_MESSAGE, context.getString(R.string.notif_channel_messages), NotificationManager.IMPORTANCE_HIGH)
            messageChannel.enableVibration(true)
            messageChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            messageChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes)
            notificationManager.createNotificationChannel(messageChannel)

            // Contact requests
            val requestsChannel = NotificationChannel(NOTIF_CHANNEL_REQUEST,context.getString(R.string.notif_channel_requests), NotificationManager.IMPORTANCE_DEFAULT)
            requestsChannel.enableVibration(true)
            requestsChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            requestsChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes)
            notificationManager.createNotificationChannel(requestsChannel)

            // File transfer requests
            val fileTransferChannel = NotificationChannel(NOTIF_CHANNEL_FILE_TRANSFER, context.getString(R.string.notif_channel_file_transfer), NotificationManager.IMPORTANCE_DEFAULT)
            fileTransferChannel.enableVibration(true)
            fileTransferChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            fileTransferChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes)
            notificationManager.createNotificationChannel(fileTransferChannel)

            // File transfer requests
            val syncChannel = NotificationChannel(NOTIF_CHANNEL_SYNC, context.getString(R.string.notif_channel_sync), NotificationManager.IMPORTANCE_DEFAULT)
            syncChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            syncChannel.enableLights(false)
            syncChannel.enableVibration(false)
            syncChannel.setShowBadge(false)
            syncChannel.setSound(null, null)
            notificationManager.createNotificationChannel(syncChannel)

            // Background service channel
            val backgroundChannel = NotificationChannel(NOTIF_CHANNEL_SERVICE, context.getString(R.string.notif_channel_background_service), NotificationManager.IMPORTANCE_LOW)
            backgroundChannel.description = context.getString(R.string.notif_channel_background_service_descr)
            backgroundChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            backgroundChannel.enableLights(false)
            backgroundChannel.enableVibration(false)
            backgroundChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(backgroundChannel)
        }
    }
}