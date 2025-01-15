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
package cx.ring.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.text.format.Formatter
import android.util.JsonWriter
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
import cx.ring.application.JamiApplication
import cx.ring.client.CallActivity
import cx.ring.client.ConversationActivity
import cx.ring.client.HomeActivity
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.service.CallNotificationService
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.tv.call.TVCallActivity
import cx.ring.utils.ContentUri
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import cx.ring.views.AvatarFactory.toAdaptiveIcon
import cx.ring.views.AvatarFactory.toBitmap
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.call.CallPresenter
import net.jami.model.*
import net.jami.model.interaction.Interaction.TransferStatus
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.TextMessage
import net.jami.services.*
import net.jami.smartlist.ConversationItemViewModel
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NotificationServiceImpl(
    private val mContext: Context,
    private val mAccountService: AccountService,
    private val mContactService: ContactService,
    private val mPreferencesService: PreferencesService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mCallService: CallService
    ) : NotificationService {
    private val mNotificationBuilders = SparseArray<NotificationCompat.Builder>()
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(mContext)
    private val random = Random()
    private val avatarSize = (mContext.resources.displayMetrics.density * AvatarFactory.SIZE_NOTIF).toInt()
    private val currentCalls = LinkedHashMap<String, Conference>()

    private val callNotifications = ConcurrentHashMap<Int, Notification>()
    private val dataTransferNotifications = ConcurrentHashMap<Int, Notification>()
    private var pendingNotificationActions = ArrayList<() -> Unit>()
    private var pendingScreenshareCallbacks = HashMap<String, () -> Unit>()

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
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION))
    }

    private fun buildCallNotification(conference: Conference): Notification? {
        val ongoingConference = currentCalls.values.firstOrNull { it !== conference && it.state == Call.CallStatus.CURRENT }
        val call = conference.firstCall!!
        val accountId = call.account
        val callClass = if (DeviceUtils.isTv(mContext)) TVCallActivity::class.java else CallActivity::class.java
        val viewIntent = PendingIntent.getActivity(mContext, random.nextInt(), Intent(Intent.ACTION_VIEW)
            .setClass(mContext, callClass)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(NotificationService.KEY_CALL_ID, call.id), ContentUri.immutable())

        val contact = getProfile(accountId, call.contact!!)
        val caller = Person.Builder()
            .setName(contact.displayName)
            .setKey(ConversationPath.toKey(accountId, contact.contact.uri.uri))
            .setIcon(getAdaptiveContactPicture(contact))
            .setImportant(true)
            .build()

        val hasVideo = conference.hasVideo()

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
                .setStyle(
                    NotificationCompat.CallStyle.forOngoingCall(caller, PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_CALL_END)
                            .setClass(mContext, DRingService::class.java)
                            .putExtra(NotificationService.KEY_CALL_ID, call.id)
                            .putExtra(ConversationPath.KEY_ACCOUNT_ID, accountId),
                        ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                    .setIsVideo(hasVideo))
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
                    .setStyle(
                        NotificationCompat.CallStyle.forIncomingCall(caller,
                            PendingIntent.getService(mContext, random.nextInt(), Intent(DRingService.ACTION_CALL_REFUSE)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, accountId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.id), ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)),
                            PendingIntent.getActivity(mContext, random.nextInt(), Intent(DRingService.ACTION_CALL_ACCEPT)
                                .setClass(mContext, callClass)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, accountId)
                                .putExtra(NotificationService.KEY_CALL_ID, call.id)
                                .putExtra(CallPresenter.KEY_ACCEPT_OPTION, CallPresenter.ACCEPT_HOLD)
                                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo), ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                            .setIsVideo(hasVideo))
            } else {
                messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS)
                    .setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.displayName))
                    .setContentText(mContext.getText(R.string.notif_outgoing_call))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(viewIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setColorized(true)
                    .setStyle(
                        NotificationCompat.CallStyle.forOngoingCall(caller, PendingIntent.getService(mContext, random.nextInt(),
                            Intent(DRingService.ACTION_CALL_END)
                                .setClass(mContext, DRingService::class.java)
                                .putExtra(NotificationService.KEY_CALL_ID, call.id)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, accountId),
                            ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                        .setIsVideo(hasVideo))
                    .setColor(ContextCompat.getColor(mContext, R.color.color_primary_light))
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

    override fun showLocationNotification(first: Account, contact: Contact, conversation: Conversation) {
        // Ignore new notification if conversation is muted.
        if (!conversation.isNotificationEnabled) return

        val profile = getProfile(conversation)
        val path = ConversationPath.toUri(conversation)

        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true)
        val messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setLargeIcon(profile?.first)
            .setContentText(mContext.getString(R.string.location_share_contact, profile?.second))
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUri.immutable()))
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
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
        }
    }

    /**
     * Handles the creation and destruction of services associated with calls as well as displaying notifications.
     *
     * @param conference the conference object for the notification
     * @param remove     true if it should be removed from current calls
     */
    override fun handleCallNotification(conference: Conference, remove: Boolean, startScreenshare: Boolean) {
        val contact = conference.call?.contact
        val conversationUri = conference.call?.conversationUri
        val account = mAccountService.getAccount(conference.accountId)!!
        val conversation =
            if (conversationUri == null)
                if (contact == null) {
                    Log.e(TAG, "Unable to show notification. contact and conversationId are null")
                    return
                } else
                    account.getByUri(contact.conversationUri.blockingFirst())
                        ?: account.getByUri(contact.uri)
            else
                account.getByUri(conversationUri)

        // Ignore new notification if conversation is muted.
        // Always try to remove notification (case where conversation is muted during a call).
        if (!remove && !conversation!!.isNotificationEnabled) return

        if (!remove && conference.isIncoming && conference.state == Call.CallStatus.RINGING) {
            // Filter case where state is ringing but we haven't receive the media list yet
            val call = conference.call ?: return
            if (call.mediaList == null)
                return
            mCallService.requestIncomingCall(call).subscribe { result ->
                Log.w(TAG, "Telecom API: requestIncomingCall result ${result.allowed}")
                if (result.allowed) {
                    result.setCall(call)
                    manageCallNotification(conference, remove, startScreenshare)
                }
            }
        } else {
            manageCallNotification(conference, remove, startScreenshare)
        }
    }
    private fun manageCallNotification(conference: Conference, remove: Boolean, startScreenshare: Boolean) {
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
        Log.w(TAG, "showCallNotification $notification")
        if (notification != null) {
            val nid = random.nextInt()
            callNotifications[nid] = notification
            val start = {
                ContextCompat.startForegroundService(mContext,
                    Intent(CallNotificationService.ACTION_START, null, mContext, CallNotificationService::class.java)
                        .putExtra(NotificationService.KEY_NOTIFICATION_ID, nid)
                        .putExtra(NotificationService.KEY_SCREENSHARE, startScreenshare)
                        .putExtra(NotificationService.KEY_CALL_ID, id)
                )
            }
            try {
                start()
            }
            catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (e is ForegroundServiceStartNotAllowedException) {
                        pingPush(conference.accountId, start)
                    } else {
                        Log.w(TAG, "Can't show call notification", e)
                    }
                } else {
                    Log.w(TAG, "Can't show call notification", e)
                }
            }
        } else {
            removeCallNotification()
        }
    }

    override fun preparePendingScreenshare(conference: Conference, callback: () -> Unit) {
        pendingScreenshareCallbacks[conference.id] = callback
        handleCallNotification(conference, false, true)
    }

    /**
     * Starts a pending screenshare after the foreground service has received the required
     * screen capture permission using a previously provided callback
     * @param confId The call to start the screenshare on
     */
    override fun startPendingScreenshare(confId: String) {
        val callback = pendingScreenshareCallbacks[confId] ?: return
        callback()
        pendingScreenshareCallbacks.remove(confId)
    }

    override fun processPush() {
        val actions: List<() -> Unit>
        synchronized(this) {
            actions = pendingNotificationActions
            pendingNotificationActions = ArrayList()
        }
        for (action in actions)
            try {
                action()
            } catch (e: Exception) {
                Log.w(TAG, "Error running push action", e)
            }
    }

    private fun pingPush(accountId: String, start: () -> Unit) {
        if (mPreferencesService.isPushAllowed) {
            val account = mAccountService.getAccount(accountId) ?: return
            if (account.isDhtProxyEnabled) {
                val server = account.dhtProxyUsed
                if (server.isEmpty()) return
                synchronized(this) {
                    pendingNotificationActions.add(start)
                }
                Schedulers.io().scheduleDirect {
                    try {
                        val url = URL("$server/node/pingPush")
                        Log.w(TAG, "pingPush $url")
                        val urlConnection: HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
                            doOutput = true
                            instanceFollowRedirects = false
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json")
                            setRequestProperty("charset", "utf-8")
                        }
                        AutoCloseable { urlConnection.disconnect() }.use {
                            JsonWriter(OutputStreamWriter(urlConnection.outputStream, "UTF-8")).apply {
                                beginObject()
                                name("key").value(JamiApplication.instance!!.pushToken)
                                name("client_id").value(accountId)
                                name("platform").value("android")
                                endObject()
                                flush()
                            }
                            val i = InputStreamReader(BufferedInputStream(urlConnection.inputStream)).use { it.readText() }
                            Log.w(TAG, "pingPush Got code ${urlConnection.responseCode} $i")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending push ping", e)
                    }
                }
            }
        }
    }

    override fun removeCallNotification() {
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

    override fun showTextNotification(conversation: Conversation) {
        val texts = conversation.unreadTextMessages
        if (!conversation.isNotificationEnabled) {
            cancelTextNotification(conversation.accountId, conversation.uri)
            return
        }
        if (!conversation.isBubble && (texts.isEmpty() || conversation.isVisible)) {
            cancelTextNotification(conversation.accountId, conversation.uri)
            return
        }
        if ((conversation.isBubble && texts.isEmpty()) || texts.lastEntry()!!.value.isNotified) {
            return
        }
        Log.w(TAG, "showTextNotification " + conversation.accountId + " " + conversation.uri)
        mContactService.getLoadedConversation(conversation)
            .subscribe({ cvm -> textNotification(texts, cvm) })
        { e: Throwable -> Log.w(TAG, "Can't load contact", e) }
    }

    /**
     * Function to show a group call notification.
     */
    override fun showGroupCallNotification(conversation: Conversation, remove: Boolean) {
        // Ignore new notification if conversation is muted.
        // Always try to remove notification (case where conversation is muted during a call).
        if (!remove && !conversation.isNotificationEnabled) return

        mContactService.getLoadedConversation(conversation)
            .subscribe({ cvm -> showGroupCallNotification(cvm, remove) })
            { e: Throwable -> Log.w(TAG, "Can't load contact", e) }
    }

    /**
     * Function to show a group call notification.
     */
    private fun showGroupCallNotification(cvm: ConversationItemViewModel, remove: Boolean) {
        // Obtain the conversation path and key
        val cpath = ConversationPath(cvm.accountId, cvm.uri)
        val path = cpath.toUri()
        val key = cpath.toKey()

        // Generate a unique notification ID
        val notificationId = getTextNotificationId(cpath.accountId, cvm.uri)

        if (remove) { // Remove the notification if the call is no longer in progress.
            CarNotificationManager.from(mContext).cancel(notificationId)
            return
        }

        // Get the conversation profile
        val conversationProfile = getProfile(cvm)

        // Determine the notification visibility based on the user's preference
        var notificationVisibility = mPreferencesService.settings.notificationVisibility
        notificationVisibility = when (notificationVisibility) {
            SettingsFragment.NOTIFICATION_PUBLIC -> Notification.VISIBILITY_PUBLIC
            SettingsFragment.NOTIFICATION_SECRET -> Notification.VISIBILITY_SECRET
            SettingsFragment.NOTIFICATION_PRIVATE -> Notification.VISIBILITY_PRIVATE
            else -> Notification.VISIBILITY_PRIVATE
        }

        // Create an intent to open the conversation in the app
        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Build the notification
        val text = mContext.getString(R.string.notif_inprogress_group_call)
        val messageNotificationBuilder = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(notificationVisibility)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setContentTitle(conversationProfile.second)
            .setContentText(text)
            .setLocusId(LocusIdCompat(key))
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUri.immutable()))
            .setAutoCancel(true)
            .setColor(ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null))
            .setLargeIcon(conversationProfile.first)

        // Notify the notification
        CarNotificationManager.from(mContext).notify(notificationId, messageNotificationBuilder)
        // Save the notification builder for future reference
        mNotificationBuilders.put(notificationId, messageNotificationBuilder)
    }

    private fun textNotification(
        texts: TreeMap<Long, TextMessage>,
        cvm: ConversationItemViewModel
    ) {
        val cpath = ConversationPath(cvm.accountId, cvm.uri)
        val path = cpath.toUri()
        val key = cpath.toKey()
        val conversationProfile = getProfile(cvm)
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
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUri.immutable()))
            .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, ContentUri.immutable()))
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
                PendingIntent.getActivity(mContext, 0, intentBubble, ContentUri.mutable(PendingIntent.FLAG_UPDATE_CURRENT)),
                AvatarDrawable.Builder()
                    .withViewModel(cvm)
                    .withCircleCrop(false)
                    .withPresence(false)
                    .build(mContext)
                    .toAdaptiveIcon(avatarSize))
                .setDesiredHeight(600)
                .build())
            //.addPerson(conversationPerson)
            .setShortcutId(key)
        val account = mAccountService.getAccount(cpath.accountId)
        val profile = if (account == null) null else VCardServiceImpl.loadProfile(mContext, account).blockingFirst()
        val userPerson = Person.Builder()
            .setKey(cpath.accountId)
            .setName(if (profile == null || TextUtils.isEmpty(profile.displayName)) "You" else profile.displayName)
            .setIcon(account?.let { getContactPicture(it) })
            .build()
        val history = NotificationCompat.MessagingStyle(userPerson)
        // Even if it's a group conversation, if there is only two people in it,
        // we don't want to display notification as a group (not necessary to surcharge).
        history.isGroupConversation = cvm.isSwarm && cvm.contacts.size > 2
        history.conversationTitle = conversationProfile.second
        val persons = HashMap<String, Person>()
        for (contact in cvm.contacts) {
            if (contact.contact.isUser) continue
            val contactPerson = Person.Builder()
                .setKey(ConversationPath.toKey(cpath.accountId, contact.contact.uri.uri))
                .setName(contact.displayName)
                .setIcon(getAdaptiveContactPicture(contact))
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
        val notificationId = getTextNotificationId(cpath.accountId, cvm.uri)
        val replyId = notificationId + 1
        val markAsReadId = notificationId + 2
        val replyLabel = mContext.getText(R.string.notif_reply)
        val remoteInput = RemoteInput.Builder(DRingService.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        val replyPendingIntent = PendingIntent.getService(mContext, replyId,
            Intent(DRingService.ACTION_CONV_REPLY_INLINE, path, mContext, DRingService::class.java),
            ContentUri.mutable(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val readPendingIntent = PendingIntent.getService(mContext, markAsReadId,
            Intent(DRingService.ACTION_CONV_READ, path, mContext, DRingService::class.java), ContentUri.immutable())
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

    private fun getRequestNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(mContext, NOTIF_CHANNEL_REQUEST)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentTitle(mContext.getString(R.string.new_invitation_request_title))
            .setColor(ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null))
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
            mContactService.getLoadedConversation(request)
                .observeOn(Schedulers.computation())
                .subscribe({ vm ->
                    mPreferencesService.saveRequestPreferences(account.accountId, contactKey)
                    val info = ConversationPath.toUri(account.accountId, request.uri)
                    val builder = getRequestNotificationBuilder()

                    builder.setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(),
                        Intent(Intent.ACTION_VIEW, info, mContext, HomeActivity::class.java),
                        ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT))
                    )

                    builder.setContentText(vm.uriTitle)
                        .addAction(R.drawable.baseline_person_add_24, mContext.getText(R.string.accept), PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT, info, mContext, DRingService::class.java),
                            ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                        .addAction(R.drawable.baseline_delete_24, mContext.getText(R.string.decline), PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_TRUST_REQUEST_REFUSE, info, mContext, DRingService::class.java),
                            ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                        .addAction(R.drawable.baseline_block_24, mContext.getText(R.string.block), PendingIntent.getService(
                            mContext, random.nextInt(),
                            Intent(DRingService.ACTION_TRUST_REQUEST_BLOCK, info, mContext, DRingService::class.java),
                            ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                    getContactPicture(request)?.let { pic -> builder.setLargeIcon(pic) }
                    notificationManager.notify(notificationId, builder.build())
                }) { e: Throwable -> Log.w(TAG, "error showing notification", e) }
        } else {
            val builder = getRequestNotificationBuilder()
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

            builder.setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(),
                    Intent(mContext, HomeActivity::class.java)
                            .setAction(NotificationService.NOTIF_TRUST_REQUEST_MULTIPLE)
                            .putExtra(NotificationService.NOTIF_TRUST_REQUEST_ACCOUNT_ID, account.accountId),
                    ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT))
            )

            builder.setContentText(String.format(mContext.getString(R.string.invitation_message), requests.size))
            builder.setLargeIcon(null as Icon?)
            notificationManager.notify(notificationId, builder.build())
        }
    }

    @SuppressLint("RestrictedApi")
    override fun showFileTransferNotification(conversation: Conversation, info: DataTransfer) {
        val event = info.transferStatus
        if (event == TransferStatus.FILE_AVAILABLE)
            return
        val path = ConversationPath.toUri(conversation)
        Log.d(TAG, "showFileTransferNotification $path")
        val dataTransferId = info.fileId ?: info.id.toString()
        val notificationId = getFileTransferNotificationId(path, dataTransferId)
        val intentViewConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
        //val profile = getProfile(conversation)
        val author = mContactService.getLoadedContact(info.account!!, info.author!!).blockingGet()

        if (event.isOver) {
            removeTransferNotification(path, dataTransferId)
            if (info.isOutgoing || info.isError) {
                return
            }
        }

        // Ignore new notification if conversation is muted.
        if (!conversation.isNotificationEnabled) return

        if(event.isOver){
            val notif = NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentViewConversation, ContentUri.immutable()))
                .setAutoCancel(true)
            if (info.showPicture()) {
                val filePath = mDeviceRuntimeService.getConversationPath(conversation.accountId, conversation.uri.rawRingId, info.storagePath)
                val img: Bitmap
                try {
                    val d = Glide.with(mContext)
                        .load(filePath)
                        .submit()
                        .get() as BitmapDrawable
                    img = d.bitmap
                    notif.setContentTitle(mContext.getString(R.string.notif_incoming_picture, author.displayName))
                    notif.setStyle(NotificationCompat.BigPictureStyle().bigPicture(img))
                } catch (e: Exception) {
                    Log.w(TAG, "Can't load image for notification", e)
                    return
                }
            } else {
                notif.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title, author.displayName))
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
        val ongoing = event == TransferStatus.TRANSFER_ONGOING || event == TransferStatus.TRANSFER_ACCEPTED
        messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title, author.displayName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(ongoing)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setContentText(
                if (event == TransferStatus.TRANSFER_ONGOING)
                    Formatter.formatFileSize(mContext, info.bytesProgress) + " / " + Formatter.formatFileSize(mContext, info.totalSize)
                else
                    info.displayName + ": " + cx.ring.utils.TextUtils.getReadableFileTransferStatus(mContext, event)
            )
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentViewConversation, ContentUri.immutable()))
            .color = ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null)
        val picture = getContactPicture(conversation)
        if (picture != null) messageNotificationBuilder.setLargeIcon(picture)
        when {
            event.isOver -> messageNotificationBuilder.setProgress(0, 0, false)
            ongoing -> messageNotificationBuilder.setProgress(info.totalSize.toInt(), info.bytesProgress.toInt(), false)
            else -> messageNotificationBuilder.setProgress(0, 0, true)
        }
        if (event == TransferStatus.TRANSFER_CREATED) {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            mNotificationBuilders.put(notificationId, messageNotificationBuilder)
            // updateNotification(messageNotificationBuilder.build(), notificationId);
            return
        } else {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }
        messageNotificationBuilder.mActions.clear()
        if (event == TransferStatus.TRANSFER_AWAITING_HOST) {
            messageNotificationBuilder
                .addAction(R.drawable.baseline_call_received_24, mContext.getText(R.string.accept),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_ACCEPT, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
                .addAction(R.drawable.baseline_cancel_24, mContext.getText(R.string.decline),
                    PendingIntent.getService(mContext, random.nextInt(),
                        Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
            updateNotification(messageNotificationBuilder.build(), notificationId)
            return
        } else if (!event.isOver) {
            messageNotificationBuilder.addAction(R.drawable.baseline_cancel_24, mContext.getText(android.R.string.cancel),
                PendingIntent.getService(mContext, random.nextInt(),
                    Intent(DRingService.ACTION_FILE_CANCEL, path, mContext, DRingService::class.java)
                        .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId), ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
        }
        dataTransferNotifications[notificationId] = messageNotificationBuilder.build()

        val start = {
            ContextCompat.startForegroundService(
                mContext,
                Intent(
                    DataTransferService.ACTION_START, path, mContext,
                    DataTransferService::class.java
                ).putExtra(NotificationService.KEY_NOTIFICATION_ID, notificationId))
        }

        try {
            start()
        }
        catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (e is ForegroundServiceStartNotAllowedException) {
                    pingPush(conversation.accountId, start)
                } else {
                    Log.w(TAG, "Error starting file transfer service " + e.message)
                }
            } else {
                Log.w(TAG, "Error starting file transfer service " + e.message)
            }
        }
    }

    override fun showMissedCallNotification(call: Call) {
        val conversationUri = call.conversationUri
        val contact = call.contact
        val account = mAccountService.getAccount(call.account)!!
        val conversation =
            if (conversationUri == null)
                if (contact == null) {
                    Log.e(TAG, "Unable to show missed call notification. contact and conversationId are null")
                    return
                } else
                    account.getByUri(contact.conversationUri.blockingFirst())
                        ?: account.getByUri(contact.uri)
            else
                account.getByUri(conversationUri)

        // Ignore new notification if conversation is muted.
        if (!conversation!!.isNotificationEnabled) return

        val notificationId = call.id.hashCode()
        val messageNotificationBuilder = mNotificationBuilders[notificationId]
            ?: NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MISSED_CALL)
        val path = ConversationPath.toUri(call.account, call.conversationUri ?: call.contact?.conversationUri?.blockingFirst()!!)
        val intentConversation = Intent(Intent.ACTION_VIEW, path, mContext, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contactViewModel = getProfile(call.account, call.contact!!)

        messageNotificationBuilder.setContentTitle(mContext.getText(R.string.notif_missed_incoming_call))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.baseline_call_missed_24)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentText(contactViewModel.displayName)
            .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
            .color = ResourcesCompat.getColor(mContext.resources, R.color.color_primary_dark, null)
        setContactPicture(contactViewModel, messageNotificationBuilder)
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
                .setContentIntent(PendingIntent.getActivity(mContext, 0, intentHome, ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)))
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
        notificationManager.cancel(getIncomingTrustNotificationId(accountID))
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

    private fun getContactPicture(contact: ContactViewModel): Bitmap? =
        try {
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .withPresence(false)
                .build(mContext)
                .toBitmap(avatarSize)
        } catch (e: Exception) {
            null
        }

    private fun getAdaptiveContactPicture(contact: ContactViewModel): IconCompat? =
        try {
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(false)
                .withPresence(false)
                .build(mContext)
                .toAdaptiveIcon(avatarSize)
        } catch (e: Exception) {
            null
        }

    private fun getContactPicture(conversation: Conversation): Bitmap? =
        try {
            mContactService.getLoadedConversation(conversation).flatMap { vm ->
                AvatarFactory.getAvatar(mContext, vm)
                    .map { it.toBitmap(avatarSize) }
            }.blockingGet()
        } catch (e: Exception) {
            null
        }

    private fun getContactPicture(account: Account): IconCompat =
        AvatarFactory.getAccountAdaptiveIcon(mContext, account, avatarSize).blockingGet()

    private fun getProfile(conversation: Conversation): Pair<Bitmap, String>? =
        try {
            mContactService.getLoadedConversation(conversation)
                .map { getProfile(it) }
                .blockingGet()
        } catch (e: Exception) {
            null
        }

    private fun getProfile(vm: ConversationItemViewModel): Pair<Bitmap, String> =
        Pair(AvatarDrawable.Builder()
            .withViewModel(vm)
            .withCircleCrop(true)
            .build(mContext)
            .toBitmap(avatarSize), vm.title)

    private fun getProfile(accountId:String, contact: Contact): ContactViewModel =
        mContactService.getLoadedContact(accountId, contact).blockingGet()

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