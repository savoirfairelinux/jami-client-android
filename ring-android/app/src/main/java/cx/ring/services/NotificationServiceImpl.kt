/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import cx.ring.R
import cx.ring.client.ConversationActivity
import cx.ring.client.HomeActivity
import cx.ring.contactrequests.ContactRequestsFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.service.CallNotificationService
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.tv.call.TVCallActivity
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.utils.ResourceMapper
import cx.ring.views.AvatarFactory
import net.jami.model.*
import net.jami.model.Interaction.InteractionStatus
import net.jami.services.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NotificationServiceImpl(
    val mContext: Context,
    var mAccountService: AccountService,
    var mContactService: ContactService,
    var mPreferencesService: PreferencesService,
    var mDeviceRuntimeService: DeviceRuntimeService) : NotificationService {
    private val mNotificationBuilders = SparseArray<NotificationCompat.Builder>()

    private var notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(mContext)
    private val random = Random()
    private var avatarSize = (mContext.resources.displayMetrics.density * AvatarFactory.SIZE_NOTIF).toInt()
    private val currentCalls = LinkedHashMap<String, Conference>()
    private val callNotifications = ConcurrentHashMap<Int, Notification>()
    private val dataTransferNotifications = ConcurrentHashMap<Int, Notification>()
    @SuppressLint("CheckResult")
    fun initHelper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannels(mContext)
        }
    }

    /**
     * Starts the call activity directly for Android TV
     *
     * @param callId the call ID
     */
    private fun startCallActivity(callId: String) {
        mContext.startActivity(
            Intent(Intent.ACTION_VIEW)
                .putExtra(NotificationService.KEY_CALL_ID, callId)
                .setClass(mContext.applicationContext, TVCallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun buildCallNotification(conference: Conference): Notification? {
        var ongoingCallId: String? = null
        for (conf in currentCalls.values) {
            if (conf !== conference && conf.state == Call.CallStatus.CURRENT) ongoingCallId =
                conf.participants[0].daemonIdString
        }
        val call = conference.participants[0]
        val gotoIntent = PendingIntent.getService(mContext, random.nextInt(),
            Intent(DRingService.ACTION_CALL_VIEW)
                .setClass(mContext, DRingService::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString), 0)
        val contact = call.contact!!
        val messageNotificationBuilder: NotificationCompat.Builder
        if (conference.isOnGoing) {
            messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS)
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.displayName))
                .setContentText(mContext.getText(R.string.notif_current_call))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(gotoIntent)
                .setSound(null)
                .setVibrate(null)
                .setColorized(true)
                .setUsesChronometer(true)
                .setWhen(conference.timestampStart)
                .setColor(ContextCompat.getColor(mContext, R.color.color_primary_light))
                .addAction(
                    R.drawable.baseline_call_end_24,
                    mContext.getText(R.string.action_call_hangup),
                    PendingIntent.getService(
                        mContext, random.nextInt(),
                        Intent(DRingService.ACTION_CALL_END)
                            .setClass(mContext, DRingService::class.java)
                            .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                        PendingIntent.FLAG_ONE_SHOT
                    )
                )
        } else if (conference.isRinging) {
            if (conference.isIncoming) {
                messageNotificationBuilder =
                    NotificationCompat.Builder(mContext, NOTIF_CHANNEL_INCOMING_CALL)
                messageNotificationBuilder.setContentTitle(
                    mContext.getString(
                        R.string.notif_incoming_call_title,
                        contact.displayName
                    )
                )
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentText(mContext.getText(R.string.notif_incoming_call))
                    .setContentIntent(gotoIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setFullScreenIntent(gotoIntent, true)
                    .addAction(
                        R.drawable.baseline_call_end_24,
                        mContext.getText(R.string.action_call_decline),
                        PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_REFUSE)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                            PendingIntent.FLAG_ONE_SHOT
                        )
                    )
                    .addAction(
                        R.drawable.baseline_call_24,
                        if (ongoingCallId == null)
                            mContext.getText(R.string.action_call_accept)
                        else
                            mContext.getText(R.string.action_call_end_accept),
                        PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(if (ongoingCallId == null) DRingService.ACTION_CALL_ACCEPT else DRingService.ACTION_CALL_END_ACCEPT)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_END_ID, ongoingCallId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                            PendingIntent.FLAG_ONE_SHOT
                        )
                    )
                if (ongoingCallId != null) {
                    messageNotificationBuilder.addAction(
                        R.drawable.baseline_call_24,
                        mContext.getText(R.string.action_call_hold_accept),
                        PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_HOLD_ACCEPT)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_HOLD_ID, ongoingCallId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                            PendingIntent.FLAG_ONE_SHOT
                        )
                    )
                }
            } else {
                messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS)
                    .setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.displayName))
                    .setContentText(mContext.getText(R.string.notif_outgoing_call))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(gotoIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setColorized(true)
                    .setColor(ContextCompat.getColor(mContext, R.color.color_primary_light))
                    .addAction(
                        R.drawable.baseline_call_end_24,
                        mContext.getText(R.string.action_call_hangup),
                        PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_END)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString),
                            PendingIntent.FLAG_ONE_SHOT
                        )
                    )
            }
        } else {
            return null
        }
        messageNotificationBuilder.setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
        setContactPicture(contact, messageNotificationBuilder)
        return messageNotificationBuilder.build()
    }

    override fun showCallNotification(notifId: Int): Any {
        return callNotifications.remove(notifId)!!
    }

    override fun showLocationNotification(first: Account, contact: Contact) {
        val path = ConversationPath.toUri(first.accountID, contact.uri)
        val intentConversation =
            Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity::class.java)
                .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true)
        val messageNotificationBuilder = NotificationCompat.Builder(
            mContext, NOTIF_CHANNEL_MESSAGE
        )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setLargeIcon(getContactPicture(contact))
            .setContentText(
                mContext.getString(
                    R.string.location_share_contact,
                    contact.displayName
                )
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext,
                    random.nextInt(),
                    intentConversation,
                    0
                )
            )
            .setAutoCancel(false)
            .setColor(ResourcesCompat.getColor(
                    mContext.resources,
                    R.color.color_primary_dark,
                    null
                ))
        notificationManager.notify(
            Objects.hash("Location", path),
            messageNotificationBuilder.build()
        )
    }

    override fun cancelLocationNotification(first: Account, contact: Contact) {
        notificationManager.cancel(
            Objects.hash(
                "Location",
                ConversationPath.toUri(first.accountID, contact.uri)
            )
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
     * Starts a service (data transfer or call)
     *
     * @param id            the notification id
     */
    private fun startForegroundService(id: Int, serviceClass: Class<*>) {
        ContextCompat.startForegroundService(
            mContext, Intent(mContext, serviceClass)
                .putExtra(NotificationService.KEY_NOTIFICATION_ID, id)
        )
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
            ContextCompat.startForegroundService(mContext,
                Intent(CallNotificationService.ACTION_START, null, mContext, CallNotificationService::class.java)
                    .putExtra(NotificationService.KEY_NOTIFICATION_ID, nid))
        } else {
            try {
                mContext.startService(Intent(CallNotificationService.ACTION_STOP, null, mContext, CallNotificationService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping service", e)
            }
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
        if (DeviceUtils.isTv(mContext)) {
            return
        }
        if (!remove) {
            showFileTransferNotification(conversation, transfer)
        } else {
            removeTransferNotification(ConversationPath.toUri(conversation), transfer.fileId ?: transfer.id.toString())
        }
    }

    override fun removeTransferNotification(accountId: String, conversationUri: net.jami.model.Uri, transferId: String) {
        removeTransferNotification(ConversationPath.toUri(accountId, conversationUri), transferId)
    }

    /**
     * Cancels a data transfer notification and removes it from the list of notifications
     *
     * @param transferId the transfer id which is required to generate the notification id
     */
    fun removeTransferNotification(path: Uri, transferId: String) {
        val id = getFileTransferNotificationId(path, transferId)
        dataTransferNotifications.remove(id)
        cancelFileNotification(id, false)
        if (dataTransferNotifications.isEmpty()) {
            mContext.startService(
                Intent(
                    DataTransferService.ACTION_STOP,
                    path,
                    mContext,
                    DataTransferService::class.java
                )
                    .putExtra(NotificationService.KEY_NOTIFICATION_ID, id)
            )
        } else {
            ContextCompat.startForegroundService(
                mContext,
                Intent(
                    DataTransferService.ACTION_STOP,
                    path,
                    mContext,
                    DataTransferService::class.java
                )
                    .putExtra(NotificationService.KEY_NOTIFICATION_ID, id)
            )
        }
    }

    /**
     * @param notificationId the notification id
     * @return the notification object for a data transfer notification
     */
    override fun getDataTransferNotification(notificationId: Int): Notification {
        return dataTransferNotifications[notificationId]!!
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
            .subscribe({ textNotification(accountId, texts, conversation) })
            { e: Throwable -> Log.w(TAG, "Can't load contact", e) }
    }

    private fun textNotification(
        accountId: String,
        texts: TreeMap<Long, TextMessage>,
        conversation: Conversation
    ) {
        val cpath = ConversationPath(conversation)
        val path = cpath.toUri()
        val conversationProfile = getProfile(conversation)
        var notificationVisibility = mPreferencesService.settings.notificationVisibility
        notificationVisibility = when (notificationVisibility) {
            SettingsFragment.NOTIFICATION_PUBLIC -> Notification.VISIBILITY_PUBLIC
            SettingsFragment.NOTIFICATION_SECRET -> Notification.VISIBILITY_SECRET
            SettingsFragment.NOTIFICATION_PRIVATE -> Notification.VISIBILITY_PRIVATE
            else -> Notification.VISIBILITY_PRIVATE
        }
        val last = texts.lastEntry()?.value
        val intentConversation = Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService::class.java)
        val intentDelete = Intent(DRingService.ACTION_CONV_DISMISS, path, mContext, DRingService::class.java)
        val messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(notificationVisibility)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setContentTitle(conversationProfile.second)
            .setContentText(last?.body)
            .setWhen(last?.timestamp ?: 0)
            .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
            .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, 0))
            .setAutoCancel(true)
            .setColor(
                ResourcesCompat.getColor(
                    mContext.resources,
                    R.color.color_primary_dark,
                    null
                )
            )
        val key = cpath.toKey()
        val conversationPerson = Person.Builder()
            .setKey(key)
            .setName(conversationProfile.second)
            .setIcon(
                if (conversationProfile.first == null) null else IconCompat.createWithBitmap(conversationProfile.first)
            )
            .build()
        if (conversationProfile.first != null) {
            messageNotificationBuilder.setLargeIcon(conversationProfile.first)
            val intentBubble = Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity::class.java)
            intentBubble.putExtra(EXTRA_BUBBLE, true)
            messageNotificationBuilder
                .setBubbleMetadata(NotificationCompat.BubbleMetadata.Builder(PendingIntent.getActivity(
                    mContext, 0, intentBubble,
                    PendingIntent.FLAG_UPDATE_CURRENT
                ), IconCompat.createWithAdaptiveBitmap(conversationProfile.first))
                    .setDesiredHeight(600)
                    .build())
                .addPerson(conversationPerson)
                .setShortcutId(key)
        }
        if (texts.size == 1) {
            last!!.isNotified = true
            messageNotificationBuilder.setStyle(null)
        } else {
            val account = mAccountService.getAccount(accountId)
            val profile = if (account == null) null else VCardServiceImpl.loadProfile(
                mContext, account
            ).blockingFirst()
            val myPic = account?.let { getContactPicture(it) }
            val userPerson = Person.Builder()
                .setKey(accountId)
                .setName(if (profile == null || TextUtils.isEmpty(profile.displayName)) "You" else profile.displayName)
                .setIcon(if (myPic == null) null else IconCompat.createWithBitmap(myPic))
                .build()
            val history = NotificationCompat.MessagingStyle(userPerson)
            for (textMessage in texts.values) {
                val contact = textMessage.contact!!
                val contactPicture = getContactPicture(contact)
                val contactPerson = Person.Builder()
                    .setKey(ConversationPath.toKey(cpath.accountId, contact.uri.uri))
                    .setName(contact.displayName)
                    .setIcon(if (contactPicture == null) null else IconCompat.createWithBitmap(contactPicture))
                    .build()
                history.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        textMessage.body,
                        textMessage.timestamp,
                        if (textMessage.isIncoming) contactPerson else null
                    )
                )
            }
            messageNotificationBuilder.setStyle(history)
        }
        val notificationId = getTextNotificationId(conversation.accountId, conversation.uri)
        val replyId = notificationId + 1
        val markAsReadId = notificationId + 2
        val replyLabel = mContext.getText(R.string.notif_reply)
        val remoteInput = RemoteInput.Builder(DRingService.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        val replyPendingIntent = PendingIntent.getService(
            mContext, replyId,
            Intent(DRingService.ACTION_CONV_REPLY_INLINE, path, mContext, DRingService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val readPendingIntent = PendingIntent.getService(
            mContext, markAsReadId,
            Intent(DRingService.ACTION_CONV_READ, path, mContext, DRingService::class.java), 0
        )
        messageNotificationBuilder
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.baseline_reply_24,
                    replyLabel,
                    replyPendingIntent
                )
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .addRemoteInput(remoteInput)
                    .extend(
                        NotificationCompat.Action.WearableExtender()
                            .setHintDisplayActionInline(true)
                    )
                    .build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    mContext.getString(R.string.notif_mark_as_read),
                    readPendingIntent
                )
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .setShowsUserInterface(false)
                    .build()
            )
        notificationManager.notify(notificationId, messageNotificationBuilder.build())
        mNotificationBuilders.put(notificationId, messageNotificationBuilder)
    }

    private fun getRequestNotificationBuilder(accountId: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(
            mContext, NOTIF_CHANNEL_REQUEST
        )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentTitle(mContext.getString(R.string.contact_request_title))
        val intentOpenTrustRequestFragment =
            Intent(HomeActivity.ACTION_PRESENT_TRUST_REQUEST_FRAGMENT)
                .setClass(mContext, HomeActivity::class.java)
                .putExtra(ContactRequestsFragment.ACCOUNT_ID, accountId)
        builder.setContentIntent(
            PendingIntent.getActivity(
                mContext,
                random.nextInt(), intentOpenTrustRequestFragment, PendingIntent.FLAG_ONE_SHOT
            )
        )
        builder.color = ResourcesCompat.getColor(
            mContext.resources,
            R.color.color_primary_dark, null
        )
        return builder
    }

    override fun showIncomingTrustRequestNotification(account: Account) {
        val notificationId = getIncomingTrustNotificationId(account.accountID)
        val notifiedRequests = mPreferencesService.loadRequestsPreferences(account.accountID)
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
            mContactService.getLoadedContact(account.accountID, request.contacts, false)
                .subscribe(
                    {
                        val builder = getRequestNotificationBuilder(account.accountID)
                        mPreferencesService.saveRequestPreferences(account.accountID, contactKey)
                        val info = ConversationPath.toUri(account.accountID, request.uri)
                        builder.setContentText(request.uriTitle)
                            .addAction(
                                R.drawable.baseline_person_add_24,
                                mContext.getText(R.string.accept),
                                PendingIntent.getService(
                                    mContext, random.nextInt(),
                                    Intent(
                                        DRingService.ACTION_TRUST_REQUEST_ACCEPT,
                                        info,
                                        mContext,
                                        DRingService::class.java
                                    ),
                                    PendingIntent.FLAG_ONE_SHOT
                                )
                            )
                            .addAction(
                                R.drawable.baseline_delete_24, mContext.getText(R.string.refuse),
                                PendingIntent.getService(
                                    mContext, random.nextInt(),
                                    Intent(
                                        DRingService.ACTION_TRUST_REQUEST_REFUSE,
                                        info,
                                        mContext,
                                        DRingService::class.java
                                    ),
                                    PendingIntent.FLAG_ONE_SHOT
                                )
                            )
                            .addAction(
                                R.drawable.baseline_block_24, mContext.getText(R.string.block),
                                PendingIntent.getService(
                                    mContext, random.nextInt(),
                                    Intent(
                                        DRingService.ACTION_TRUST_REQUEST_BLOCK,
                                        info,
                                        mContext,
                                        DRingService::class.java
                                    ),
                                    PendingIntent.FLAG_ONE_SHOT
                                )
                            )
                        val pic = getContactPicture(request)
                        if (pic != null) builder.setLargeIcon(pic)
                        notificationManager.notify(notificationId, builder.build())
                    }) { e: Throwable? -> Log.w(TAG, "error showing notification", e) }
        } else {
            val builder = getRequestNotificationBuilder(account.accountID)
            var newRequest = false
            for (request in requests) {
                val contact = request.contact
                if (contact != null) {
                    val contactKey = contact.uri.rawRingId
                    if (!notifiedRequests.contains(contactKey)) {
                        newRequest = true
                        mPreferencesService.saveRequestPreferences(account.accountID, contactKey)
                    }
                }
            }
            if (!newRequest) return
            builder.setContentText(
                String.format(
                    mContext.getString(R.string.contact_request_msg),
                    requests.size
                )
            )
            builder.setLargeIcon(null)
            notificationManager.notify(notificationId, builder.build())
        }
    }

    override fun showFileTransferNotification(conversation: Conversation, info: DataTransfer) {
        val event = info.status ?: return
        if (event == InteractionStatus.FILE_AVAILABLE)
            return
        val path = ConversationPath.toUri(conversation)
        Log.d(TAG, "showFileTransferNotification $path")
        val dataTransferId = info.fileId ?: info.id.toString()
        val notificationId = getFileTransferNotificationId(path, dataTransferId)
        val intentConversation =
            Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService::class.java)
        if (event.isOver) {
            removeTransferNotification(path, dataTransferId)
            if (info.isOutgoing || info.isError) {
                return
            }
            val notif = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setAutoCancel(true)
            if (info.showPicture()) {
                val filePath = mDeviceRuntimeService.getConversationPath(
                    conversation.uri.rawRingId,
                    info.storagePath
                )
                val img: Bitmap
                try {
                    val d = Glide.with(mContext)
                        .load(filePath)
                        .submit()
                        .get() as BitmapDrawable
                    img = d.bitmap
                    notif.setContentTitle(mContext.getString(R.string.notif_incoming_picture,conversation.title))
                    notif.setStyle(NotificationCompat.BigPictureStyle().bigPicture(img))
                } catch (e: Exception) {
                    Log.w(TAG, "Can't load image for notification", e)
                    return
                }
            } else {
                notif.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title, conversation.title))
                notif.setStyle(null)
            }
            val picture = getContactPicture(conversation)
            if (picture != null) notif.setLargeIcon(picture)
            notificationManager.notify(random.nextInt(), notif.build())
            return
        }
        var messageNotificationBuilder = mNotificationBuilders[notificationId]
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
        }
        val ongoing = event == InteractionStatus.TRANSFER_ONGOING || event == InteractionStatus.TRANSFER_ACCEPTED
        val titleMessage = mContext.getString(
            if (info.isOutgoing) R.string.notif_outgoing_file_transfer_title else R.string.notif_incoming_file_transfer_title,
            conversation.title
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
                    info.displayName + ": " + ResourceMapper.getReadableFileTransferStatus(mContext, event)
            )
            .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
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
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), PendingIntent.FLAG_ONE_SHOT))
                .addAction(R.drawable.baseline_cancel_24, mContext.getText(R.string.refuse),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), PendingIntent.FLAG_ONE_SHOT))
            mNotificationBuilders.put(notificationId, messageNotificationBuilder)
            updateNotification(messageNotificationBuilder.build(), notificationId)
            return
        } else if (!event.isOver) {
            messageNotificationBuilder
                .addAction(R.drawable.baseline_cancel_24, mContext.getText(android.R.string.cancel),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), PendingIntent.FLAG_ONE_SHOT))
        }
        mNotificationBuilders.put(notificationId, messageNotificationBuilder)
        dataTransferNotifications[notificationId] = messageNotificationBuilder.build()
        ContextCompat.startForegroundService(mContext, Intent(DataTransferService.ACTION_START, path, mContext, DataTransferService::class.java)
                .putExtra(NotificationService.KEY_NOTIFICATION_ID, notificationId))
        //startForegroundService(notificationId, DataTransferService.class);
    }

    override fun showMissedCallNotification(call: Call) {
        val notificationId = call.daemonIdString.hashCode()
        var messageNotificationBuilder = mNotificationBuilders[notificationId]
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MISSED_CALL)
        }
        val path = ConversationPath.toUri(call)
        val intentConversation = Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService::class.java)
        val contact = call.contact!!
        messageNotificationBuilder.setContentTitle(mContext.getText(R.string.notif_missed_incoming_call))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.baseline_call_missed_24)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentText(contact.displayName)
            .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
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
                .setContentIntent(PendingIntent.getActivity(mContext, 0, intentHome, PendingIntent.FLAG_UPDATE_CURRENT))
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
     * @param isMigratingToService true if the notification is being updated to be a part of the foreground service
     */
    override fun cancelFileNotification(notificationId: Int, isMigratingToService: Boolean) {
        notificationManager.cancel(notificationId)
        if (!isMigratingToService) mNotificationBuilders.remove(notificationId)
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

    private fun getContactPicture(contact: Contact): Bitmap? {
        return try {
            AvatarFactory.getBitmapAvatar(mContext, contact, avatarSize, false).blockingGet()
        } catch (e: Exception) {
            null
        }
    }

    private fun getContactPicture(conversation: Conversation): Bitmap? {
        return try {
            AvatarFactory.getBitmapAvatar(mContext, conversation, avatarSize, false).blockingGet()
        } catch (e: Exception) {
            null
        }
    }

    private fun getContactPicture(account: Account): Bitmap {
        return AvatarFactory.getBitmapAvatar(mContext, account, avatarSize).blockingGet()
    }

    private fun getProfile(conversation: Conversation): Pair<Bitmap?, String> {
        return Pair.create(getContactPicture(conversation), conversation.title)
    }

    private fun setContactPicture(
        contact: Contact,
        messageNotificationBuilder: NotificationCompat.Builder
    ) {
        val pic = getContactPicture(contact)
        if (pic != null) messageNotificationBuilder.setLargeIcon(pic)
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
        private const val NOTIF_CHANNEL_INCOMING_CALL = "incoming_call"
        private const val NOTIF_CHANNEL_MESSAGE = "messages"
        private const val NOTIF_CHANNEL_REQUEST = "requests"
        private const val NOTIF_CHANNEL_FILE_TRANSFER = "file_transfer"
        const val NOTIF_CHANNEL_SYNC = "sync"
        private const val NOTIF_CHANNEL_SERVICE = "service"
        private const val NOTIF_CALL_GROUP = "calls"
        const val NOTIF_CALL_ID = 1001

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun registerNotificationChannels(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Setting up groups
            notificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(
                    NOTIF_CALL_GROUP, context.getString(R.string.notif_group_calls)
                )
            )

            // Missed calls channel
            val missedCallsChannel = NotificationChannel(
                NOTIF_CHANNEL_MISSED_CALL,
                context.getString(R.string.notif_channel_missed_calls),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            missedCallsChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            missedCallsChannel.setSound(null, null)
            missedCallsChannel.enableVibration(false)
            missedCallsChannel.group = NOTIF_CALL_GROUP
            notificationManager.createNotificationChannel(missedCallsChannel)

            // Incoming call channel
            val incomingCallChannel = NotificationChannel(
                NOTIF_CHANNEL_INCOMING_CALL,
                context.getString(R.string.notif_channel_incoming_calls),
                NotificationManager.IMPORTANCE_HIGH
            )
            incomingCallChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            incomingCallChannel.group = NOTIF_CALL_GROUP
            incomingCallChannel.setSound(null, null)
            incomingCallChannel.enableVibration(false)
            notificationManager.createNotificationChannel(incomingCallChannel)

            // Call in progress channel
            val callInProgressChannel = NotificationChannel(
                NOTIF_CHANNEL_CALL_IN_PROGRESS,
                context.getString(R.string.notif_channel_call_in_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            callInProgressChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            callInProgressChannel.setSound(null, null)
            callInProgressChannel.enableVibration(false)
            callInProgressChannel.group = NOTIF_CALL_GROUP
            notificationManager.createNotificationChannel(callInProgressChannel)

            // Text messages channel
            val soundAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()
            val messageChannel = NotificationChannel(
                NOTIF_CHANNEL_MESSAGE,
                context.getString(R.string.notif_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            )
            messageChannel.enableVibration(true)
            messageChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            messageChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                soundAttributes
            )
            notificationManager.createNotificationChannel(messageChannel)

            // Contact requests
            val requestsChannel = NotificationChannel(
                NOTIF_CHANNEL_REQUEST,
                context.getString(R.string.notif_channel_requests),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            requestsChannel.enableVibration(true)
            requestsChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            requestsChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                soundAttributes
            )
            notificationManager.createNotificationChannel(requestsChannel)

            // File transfer requests
            val fileTransferChannel = NotificationChannel(
                NOTIF_CHANNEL_FILE_TRANSFER,
                context.getString(R.string.notif_channel_file_transfer),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            fileTransferChannel.enableVibration(true)
            fileTransferChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            fileTransferChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                soundAttributes
            )
            notificationManager.createNotificationChannel(fileTransferChannel)

            // File transfer requests
            val syncChannel = NotificationChannel(
                NOTIF_CHANNEL_SYNC,
                context.getString(R.string.notif_channel_sync),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            syncChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            syncChannel.enableLights(false)
            syncChannel.enableVibration(false)
            syncChannel.setShowBadge(false)
            syncChannel.setSound(null, null)
            notificationManager.createNotificationChannel(syncChannel)

            // Background service channel
            val backgroundChannel = NotificationChannel(
                NOTIF_CHANNEL_SERVICE,
                context.getString(R.string.notif_channel_background_service),
                NotificationManager.IMPORTANCE_LOW
            )
            backgroundChannel.description =
                context.getString(R.string.notif_channel_background_service_descr)
            backgroundChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            backgroundChannel.enableLights(false)
            backgroundChannel.enableVibration(false)
            backgroundChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(backgroundChannel)
        }
    }
}