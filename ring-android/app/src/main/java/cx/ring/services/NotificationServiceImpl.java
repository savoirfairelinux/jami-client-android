/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.CarExtender.UnreadConversation;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.contacts.AvatarFactory;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.Interaction;
import cx.ring.model.Interaction.InteractionStatus;
import cx.ring.model.DataTransfer;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.service.CallNotificationService;
import cx.ring.service.DRingService;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.FileUtils;
import cx.ring.utils.ResourceMapper;
import cx.ring.utils.Tuple;

public class NotificationServiceImpl implements NotificationService {

    private static final String TAG = NotificationServiceImpl.class.getSimpleName();

    private static final String NOTIF_MSG = "MESSAGE";
    private static final String NOTIF_TRUST_REQUEST = "TRUST REQUEST";
    private static final String NOTIF_FILE_TRANSFER = "FILE_TRANSFER";
    private static final String NOTIF_MISSED_CALL = "MISSED_CALL";

    private static final String NOTIF_CHANNEL_CALL_IN_PROGRESS = "current_call";
    private static final String NOTIF_CHANNEL_MISSED_CALL = "missed_calls";
    private static final String NOTIF_CHANNEL_INCOMING_CALL = "incoming_call";

    private static final String NOTIF_CHANNEL_MESSAGE = "messages";
    private static final String NOTIF_CHANNEL_REQUEST = "requests";
    private static final String NOTIF_CHANNEL_FILE_TRANSFER = "file_transfer";
    private static final String NOTIF_CHANNEL_SERVICE = "service";

    // old channel codes that were replaced or split
    private static final String NOTIF_CHANNEL_CALL_LEGACY = "call";
    private static final String NOTIF_CHANNEL_MISSED_CALL_LEGACY = "missed_call";

    private static final String NOTIF_CALL_GROUP = "calls";

    private static final int NOTIF_CALL_ID = 1001;


    private final SparseArray<NotificationCompat.Builder> mNotificationBuilders = new SparseArray<>();
    @Inject
    protected Context mContext;
    @Inject
    protected AccountService mAccountService;
    @Inject
    protected ContactService mContactService;
    @Inject
    protected PreferencesService mPreferencesService;
    @Inject
    protected HistoryService mHistoryService;
    @Inject
    protected DeviceRuntimeService mDeviceRuntimeService;
    private NotificationManagerCompat notificationManager;
    private final Random random = new Random();
    private int avatarSize;
    private LinkedHashMap<Integer, Conference> currentCalls = new LinkedHashMap<>();
    private ConcurrentHashMap<Integer, Notification> dataTransferNotifications = new ConcurrentHashMap<>();

    @SuppressLint("CheckResult")
    public void initHelper() {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(mContext);
        }
        avatarSize = (int) (mContext.getResources().getDisplayMetrics().density * AvatarFactory.SIZE_NOTIF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;


        notificationManager.deleteNotificationChannel(NOTIF_CHANNEL_MISSED_CALL_LEGACY);
        notificationManager.deleteNotificationChannel(NOTIF_CHANNEL_CALL_LEGACY);

        // Setting up groups

        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIF_CALL_GROUP, mContext.getString(R.string.notif_group_calls)));

        // Missed calls channel
        NotificationChannel missedCallsChannel = new NotificationChannel(NOTIF_CHANNEL_MISSED_CALL, mContext.getString(R.string.notif_channel_missed_calls), NotificationManager.IMPORTANCE_LOW);
        missedCallsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        missedCallsChannel.setSound(null, null);
        missedCallsChannel.setGroup(NOTIF_CALL_GROUP);
        notificationManager.createNotificationChannel(missedCallsChannel);

        // Incoming call channel
        NotificationChannel incomingCallChannel = new NotificationChannel(NOTIF_CHANNEL_INCOMING_CALL, mContext.getString(R.string.notif_channel_incoming_calls), NotificationManager.IMPORTANCE_HIGH);
        incomingCallChannel.enableVibration(true);
        incomingCallChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        incomingCallChannel.setGroup(NOTIF_CALL_GROUP);
        incomingCallChannel.setSound(null, null);
        notificationManager.createNotificationChannel(incomingCallChannel);

        // Call in progress channel
        NotificationChannel callInProgressChannel = new NotificationChannel(NOTIF_CHANNEL_CALL_IN_PROGRESS, mContext.getString(R.string.notif_channel_call_in_progress), NotificationManager.IMPORTANCE_LOW);
        callInProgressChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        callInProgressChannel.setSound(null, null);
        callInProgressChannel.setGroup(NOTIF_CALL_GROUP);
        notificationManager.createNotificationChannel(callInProgressChannel);

        // Text messages channel
        AudioAttributes soundAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build();

        NotificationChannel messageChannel = new NotificationChannel(NOTIF_CHANNEL_MESSAGE, mContext.getString(R.string.notif_channel_messages), NotificationManager.IMPORTANCE_HIGH);
        messageChannel.enableVibration(true);
        messageChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        messageChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(messageChannel);

        // Contact requests
        NotificationChannel requestsChannel = new NotificationChannel(NOTIF_CHANNEL_REQUEST, mContext.getString(R.string.notif_channel_requests), NotificationManager.IMPORTANCE_DEFAULT);
        requestsChannel.enableVibration(true);
        requestsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        requestsChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(requestsChannel);

        // File transfer requests
        NotificationChannel fileTransferChannel = new NotificationChannel(NOTIF_CHANNEL_FILE_TRANSFER, mContext.getString(R.string.notif_channel_file_transfer), NotificationManager.IMPORTANCE_DEFAULT);
        fileTransferChannel.enableVibration(true);
        fileTransferChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        fileTransferChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(fileTransferChannel);

        // Background service channel
        NotificationChannel backgroundChannel = new NotificationChannel(NOTIF_CHANNEL_SERVICE, mContext.getString(R.string.notif_channel_background_service), NotificationManager.IMPORTANCE_LOW);
        backgroundChannel.setDescription(mContext.getString(R.string.notif_channel_background_service_descr));
        backgroundChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        backgroundChannel.enableLights(false);
        backgroundChannel.enableVibration(false);
        backgroundChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(backgroundChannel);
    }

    @Override
    public Object showCallNotification(int callId) {
        Conference mConference = currentCalls.get(callId);
        if (mConference == null || mConference.getParticipants().isEmpty()) {
            return null;
        }

        SipCall call = mConference.getParticipants().get(0);
        notificationManager.cancel(NOTIF_CALL_ID);

        PendingIntent gotoIntent = PendingIntent.getService(mContext,
                random.nextInt(),
                new Intent(DRingService.ACTION_CALL_VIEW)
                        .setClass(mContext, DRingService.class)
                        .putExtra(KEY_CALL_ID, call.getDaemonIdString()), 0);

        if (DeviceUtils.isTv(mContext)) {
            try {
                gotoIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Error launching incoming call on Android TV", e);
            }
            return null;
        }

        CallContact contact = call.getContact();
        NotificationCompat.Builder messageNotificationBuilder;
        if (mConference.isOnGoing()) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS);
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getDisplayName()))
                    .setContentText(mContext.getText(R.string.notif_current_call))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(gotoIntent)
                    .setSound(null)
                    .setColorized(true)
                    .setColor(mContext.getResources().getColor(R.color.color_primary_light))
                    .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_hangup),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_CALL_END)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (mConference.isRinging()) {
            if (mConference.isIncoming()) {
                messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_INCOMING_CALL);
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getDisplayName()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(mContext.getText(R.string.notif_incoming_call))
                        .setContentIntent(gotoIntent)
                        .setFullScreenIntent(gotoIntent, true)
                        .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_decline),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_REFUSE)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.action_call_accept),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_ACCEPT)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                        PendingIntent.FLAG_ONE_SHOT));
            } else {
                messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS);
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.getDisplayName()))
                        .setContentText(mContext.getText(R.string.notif_outgoing_call))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(gotoIntent)
                        .setSound(null)
                        .setColorized(true)
                        .setColor(mContext.getResources().getColor(R.color.color_primary_light))
                        .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_hangup),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_END)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                        PendingIntent.FLAG_ONE_SHOT));
            }
        } else {
            return null;
        }

        messageNotificationBuilder.setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white);

        setContactPicture(contact, messageNotificationBuilder);

        return messageNotificationBuilder.build();
    }

    /**
     * Updates a notification
     *
     * @param notification   a built notification object
     * @param notificationId the notification's id
     */
    @Override
    public void updateNotification(Object notification, int notificationId) {
        if (notification != null)
            notificationManager.notify(notificationId, (Notification) notification);
    }

    /**
     * Starts a service (data transfer or call)
     *
     * @param id            the notification id
     * @param isCallService true if the intended service is a call service
     */
    @Override
    public void startForegroundService(int id, boolean isCallService) {
        Intent serviceIntent;
        if (isCallService)
            serviceIntent = new Intent(mContext, CallNotificationService.class);
        else
            serviceIntent = new Intent(mContext, DataTransferService.class);


        serviceIntent.putExtra(KEY_NOTIFICATION_ID, id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mContext.startForegroundService(serviceIntent);
        else
            mContext.startService(serviceIntent);
    }

    /**
     * Kills a foreground service.
     *
     * @param isCallService true if the service in question is for calls, false if it is for data transfers
     */
    @Override
    public void stopForegroundService(boolean isCallService) {
        Intent serviceIntent;
        if (isCallService)
            serviceIntent = new Intent(mContext, CallNotificationService.class);
        else
            serviceIntent = new Intent(mContext, DataTransferService.class);
        mContext.stopService(serviceIntent);
    }

    /**
     * Handles the creation and destruction of services associated with calls as well as displaying notifications.
     *
     * @param conference the conference object for the notification
     * @param remove     true if it should be removed from current calls
     */
    @Override
    public void handleCallNotification(Conference conference, boolean remove) {
        Log.d(TAG, "handleCallNotification, call status has been updated");
        int id = conference.getId().hashCode();
        if (DeviceUtils.isTv(mContext)) {
            if (remove)
                showCallNotification(id);
            else
                cancelCallNotification();
            return;
        }
        currentCalls.remove(id);
        if (!remove) {
            currentCalls.put(id, conference);
            startForegroundService(id, true);
        } else if (currentCalls.isEmpty())
            stopForegroundService(true);
            // this is a temporary solution until we have direct support for concurrent calls and the call state will exclusively update notifications
        else {
            int key = -1;
            Iterator<Integer> iterator = (currentCalls.keySet().iterator());
            while (iterator.hasNext()) {
                key = iterator.next();
            }
            updateNotification(showCallNotification(key), NOTIF_CALL_ID);
        }
    }

    /**
     * Handles the creation and destruction of services associated with transfers as well as displaying notifications.
     *
     * @param transfer the data transfer object
     * @param contact  the contact to whom the data transfer is being sent
     * @param remove   true if it should be removed from current calls
     */
    @Override
    public void handleDataTransferNotification(DataTransfer transfer, CallContact contact, boolean remove) {
        Log.d(TAG, "handleDataTransferNotification, a data transfer event is in progress");
        if (DeviceUtils.isTv(mContext)) {
            return;
        }
        if (!remove) {
            showFileTransferNotification(transfer, contact);
        } else {
            removeTransferNotification(transfer.getDaemonId());
        }
    }

    /**
     * Cancels a data transfer notification and removes it from the list of notifications
     *
     * @param transferId the transfer id which is required to generate the notification id
     */
    @Override
    public void removeTransferNotification(long transferId) {
        int id = getFileTransferNotificationId(transferId);
        dataTransferNotifications.remove(id);
        cancelFileNotification(id, false);
        if (dataTransferNotifications.isEmpty())
            stopForegroundService(false);
        else {
            startForegroundService(dataTransferNotifications.keySet().iterator().next(), false);
        }
    }

    /**
     * @param notificationId the notification id
     * @return the notification object for a data transfer notification
     */
    @Override
    public Notification getDataTransferNotification(int notificationId) {
        return dataTransferNotifications.get(notificationId);
    }

    @Override
    public void showTextNotification(String accountId, Conversation conversation) {
        TreeMap<Long, TextMessage> texts = conversation.getUnreadTextMessages();

        CallContact contact = conversation.getContact();
        if (texts.isEmpty() || conversation.isVisible()) {
            cancelTextNotification(contact.getPrimaryUri());
            return;
        }
        if (texts.lastEntry().getValue().isNotified()) {
            return;
        }

        Log.w(TAG, "showTextNotification " + accountId + " " + contact.getPrimaryNumber());
        mContactService.getLoadedContact(accountId, contact)
                .subscribe(c -> textNotification(accountId, texts, c),
                        e -> Log.w(TAG, "Can't load contact", e));
    }

    private void textNotification(String accountId, TreeMap<Long, TextMessage> texts, CallContact contact) {
        Uri contactUri = contact.getPrimaryUri();
        String contactId = contactUri.getRawUriString();

        String contactName = contact.getDisplayName();
        if (TextUtils.isEmpty(contactName))
            return;

        TextMessage last = texts.lastEntry().getValue();
        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);

        Intent intentDelete = new Intent(DRingService.ACTION_CONV_DISMISS)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);

        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentTitle(contactName)
                .setContentText(last.getBody())
                .setWhen(last.getTimestamp())
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, 0))
                .setAutoCancel(true)
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));

        Bitmap contactPicture = getContactPicture(contact);
        if (contactPicture != null)
            messageNotificationBuilder.setLargeIcon(contactPicture);

        UnreadConversation.Builder unreadConvBuilder = new UnreadConversation.Builder(contactName)
                .setLatestTimestamp(last.getTimestamp());

        if (texts.size() == 1) {
            last.setNotified(true);
            messageNotificationBuilder.setStyle(null);
            unreadConvBuilder.addMessage(last.getBody());
        } else {
            Account account = mAccountService.getAccount(accountId);
            Tuple<String, Object> profile = account == null ? null : VCardServiceImpl.loadProfile(account).blockingGet();
            Bitmap myPic = account == null ? null : getContactPicture(account);
            Person userPerson = new Person.Builder()
                    .setKey(accountId)
                    .setName(profile == null || TextUtils.isEmpty(profile.first) ? "You" : profile.first)
                    .setIcon(myPic == null ? null : IconCompat.createWithBitmap(myPic))
                    .build();

            Person contactPerson = new Person.Builder()
                    .setKey(contactId)
                    .setName(contactName)
                    .setIcon(contactPicture == null ? null : IconCompat.createWithBitmap(contactPicture))
                    .build();

            NotificationCompat.MessagingStyle history = new NotificationCompat.MessagingStyle(userPerson);
            for (TextMessage textMessage : texts.values()) {
                history.addMessage(new NotificationCompat.MessagingStyle.Message(
                        textMessage.getBody(),
                        textMessage.getTimestamp(),
                        textMessage.isIncoming() ? contactPerson : null));
                unreadConvBuilder.addMessage(textMessage.getBody());
            }
            messageNotificationBuilder.setStyle(history);
        }

        int notificationId = getTextNotificationId(contactUri);
        int replyId = notificationId + 1;
        int markAsReadId = notificationId + 2;

        CharSequence replyLabel = mContext.getText(R.string.notif_reply);
        RemoteInput remoteInput = new RemoteInput.Builder(DRingService.KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        Intent intentReply = new Intent(DRingService.ACTION_CONV_REPLY_INLINE)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);

        PendingIntent replyPendingIntent = PendingIntent.getService(mContext, replyId,
                intentReply,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentRead = new Intent(DRingService.ACTION_CONV_READ)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);

        PendingIntent readPendingIntent = PendingIntent.getService(mContext, markAsReadId, intentRead, 0);

        messageNotificationBuilder
                .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_reply_24, replyLabel, replyPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .addRemoteInput(remoteInput)
                        .build())
                .addAction(new NotificationCompat.Action.Builder(0,
                        mContext.getString(R.string.notif_mark_as_read),
                        readPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                        .setShowsUserInterface(false)
                        .build())
                .extend(new NotificationCompat.CarExtender()
                        .setUnreadConversation(unreadConvBuilder
                                .setReadPendingIntent(readPendingIntent)
                                .setReplyAction(replyPendingIntent, remoteInput)
                                .build()));

        notificationManager.notify(notificationId, messageNotificationBuilder.build());
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
    }

    private NotificationCompat.Builder getRequestNotificationBuilder(String accountId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_REQUEST)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setContentTitle(mContext.getString(R.string.contact_request_title));
        Intent intentOpenTrustRequestFragment = new Intent(HomeActivity.ACTION_PRESENT_TRUST_REQUEST_FRAGMENT)
                .setClass(mContext, HomeActivity.class)
                .putExtra(ContactRequestsFragment.ACCOUNT_ID, accountId);
        builder.setContentIntent(PendingIntent.getActivity(mContext,
                random.nextInt(), intentOpenTrustRequestFragment, PendingIntent.FLAG_ONE_SHOT));
        builder.setColor(ResourcesCompat.getColor(mContext.getResources(),
                R.color.color_primary_dark, null));
        return builder;
    }

    @Override
    public void showIncomingTrustRequestNotification(final Account account) {
        int notificationId = getIncomingTrustNotificationId(account.getAccountID());
        Set<String> notifiedRequests = mPreferencesService.loadRequestsPreferences(account.getAccountID());

        Collection<Conversation> requests = account.getPending();
        if (requests.isEmpty())
            return;
        if (requests.size() == 1) {
            Conversation request = requests.iterator().next();
            CallContact contact = request.getContact();
            String contactKey = contact.getPrimaryUri().getRawRingId();
            if (notifiedRequests.contains(contactKey)) {
                return;
            }
            mContactService.getLoadedContact(account.getAccountID(), contact).subscribe(c -> {
                NotificationCompat.Builder builder = getRequestNotificationBuilder(account.getAccountID());
                mPreferencesService.saveRequestPreferences(account.getAccountID(), contactKey);
                Bundle info = new Bundle();
                info.putString(TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID, account.getAccountID());
                info.putString(TRUST_REQUEST_NOTIFICATION_FROM, c.getPrimaryNumber());
                builder.setContentText(c.getRingUsername())
                        .addAction(R.drawable.baseline_person_add_24, mContext.getText(R.string.accept),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT)
                                                .setClass(mContext, DRingService.class)
                                                .putExtras(info),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.baseline_delete_24, mContext.getText(R.string.refuse),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_REFUSE)
                                                .setClass(mContext, DRingService.class)
                                                .putExtras(info),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_close_white, mContext.getText(R.string.block),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_BLOCK)
                                                .setClass(mContext, DRingService.class)
                                                .putExtras(info),
                                        PendingIntent.FLAG_ONE_SHOT));

                setContactPicture(c, builder);
                notificationManager.notify(notificationId, builder.build());
            }, e -> Log.w(TAG, "error showing notification", e));
        } else {
            NotificationCompat.Builder builder = getRequestNotificationBuilder(account.getAccountID());
            boolean newRequest = false;
            for (Conversation request : requests) {
                CallContact contact = request.getContact();
                String contactKey = contact.getPrimaryUri().getRawRingId();
                if (!notifiedRequests.contains(contactKey)) {
                    newRequest = true;
                    mPreferencesService.saveRequestPreferences(account.getAccountID(), contactKey);
                }
            }
            if (!newRequest)
                return;
            builder.setContentText(String.format(mContext.getString(R.string.contact_request_msg), Integer.toString(requests.size())));
            builder.setLargeIcon(null);
            notificationManager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void showFileTransferNotification(DataTransfer info, CallContact contact) {
        if (info == null) {
            return;
        }
        InteractionStatus event = info.getStatus();
        if (event == null) {
            return;
        }
        long dataTransferId = info.getDaemonId();
        int notificationId = getFileTransferNotificationId(dataTransferId);

        String contactUri = new Uri(info.getPeerId()).getRawUriString();
        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, mAccountService.getCurrentAccount().getAccountID())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactUri);

        if (event.isOver()) {
            removeTransferNotification(dataTransferId);

            if (!info.isOutgoing() && info.showPicture()) {
                File path = mDeviceRuntimeService.getConversationPath(info.getPeerId(), info.getStoragePath());
                Bitmap img;
                try {
                    BitmapDrawable d = (BitmapDrawable) Glide.with(mContext)
                            .load(path)
                            .submit()
                            .get();
                    img = d.getBitmap();
                } catch (Exception e) {
                    Log.w(TAG, "Can't load image for notification", e);
                    return;
                }
                NotificationCompat.Builder notif = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
                        .setContentTitle(mContext.getString(R.string.notif_incoming_picture, contact.getDisplayName()))
                        .setSmallIcon(R.drawable.ic_ring_logo_white)
                        .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(img));
                setContactPicture(contact, notif);
                notificationManager.notify(random.nextInt(), notif.build());
            }
            return;
        }
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER);
        }

        boolean ongoing = event == InteractionStatus.TRANSFER_ONGOING || event == InteractionStatus.TRANSFER_ACCEPTED;
        String titleMessage = mContext.getString(info.isOutgoing() ? R.string.notif_outgoing_file_transfer_title : R.string.notif_incoming_file_transfer_title, contact.getDisplayName());
        messageNotificationBuilder.setContentTitle(titleMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(ongoing)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setContentText(event == Interaction.InteractionStatus.TRANSFER_ONGOING ?
                        FileUtils.readableFileProgress(info.getBytesProgress(), info.getTotalSize()) :
                        info.getDisplayName() + ": " + ResourceMapper.getReadableFileTransferStatus(mContext, event))
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));
        setContactPicture(contact, messageNotificationBuilder);
        if (event.isOver()) {
            messageNotificationBuilder.setProgress(0, 0, false);
        } else if (ongoing) {
            messageNotificationBuilder.setProgress((int) info.getTotalSize(), (int) info.getBytesProgress(), false);
        } else {
            messageNotificationBuilder.setProgress(0, 0, true);
        }
        if (event == Interaction.InteractionStatus.TRANSFER_CREATED) {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
            mNotificationBuilders.put(notificationId, messageNotificationBuilder);
            updateNotification(messageNotificationBuilder.build(), notificationId);
            return;
        } else {
            messageNotificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS);
        }
        messageNotificationBuilder.mActions.clear();
        if (event == Interaction.InteractionStatus.TRANSFER_AWAITING_HOST) {
            messageNotificationBuilder
                    .addAction(R.drawable.baseline_call_received_24, mContext.getText(R.string.accept),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_FILE_ACCEPT)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.baseline_cancel_24, mContext.getText(R.string.refuse),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_FILE_CANCEL)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId),
                                    PendingIntent.FLAG_ONE_SHOT));
            mNotificationBuilders.put(notificationId, messageNotificationBuilder);
            updateNotification(messageNotificationBuilder.build(), notificationId);
            return;
        } else if (!event.isOver()) {
            messageNotificationBuilder
                    .addAction(R.drawable.baseline_cancel_24, mContext.getText(android.R.string.cancel),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_FILE_CANCEL)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId),
                                    PendingIntent.FLAG_ONE_SHOT));
        }
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
        dataTransferNotifications.remove(notificationId);
        dataTransferNotifications.put(notificationId, messageNotificationBuilder.build());
        startForegroundService(notificationId, false);
    }

    @Override
    public void showMissedCallNotification(SipCall call) {
        final int notificationId = call.getDaemonIdString().hashCode();
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MISSED_CALL);
        }

        String contactUri = call.getConversation().getParticipant();
        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, mAccountService.getCurrentAccount().getAccountID())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactUri);

        messageNotificationBuilder.setContentTitle(mContext.getText(R.string.notif_missed_incoming_call))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.baseline_call_missed_24)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentText(call.getContact().getDisplayName())
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));

        setContactPicture(call.getContact(), messageNotificationBuilder);
        notificationManager.notify(notificationId, messageNotificationBuilder.build());
    }

    @Override
    public Object getServiceNotification() {
        Intent intentHome = new Intent(Intent.ACTION_VIEW)
                .setClass(mContext, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendIntent = PendingIntent.getActivity(mContext, 0, intentHome, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NotificationServiceImpl.NOTIF_CHANNEL_SERVICE);
        messageNotificationBuilder
                .setContentTitle(mContext.getText(R.string.app_name))
                .setContentText(mContext.getText(R.string.notif_background_service))
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentIntent(pendIntent)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);
        return messageNotificationBuilder.build();
    }

    @Override
    public void cancelTextNotification(Uri contact) {
        if (contact == null) {
            return;
        }
        int notificationId = getTextNotificationId(contact);
        notificationManager.cancel(notificationId);
        mNotificationBuilders.remove(notificationId);
    }

    public void cancelTextNotification(String ringId) {
        int notificationId = (NOTIF_MSG + ringId).hashCode();
        notificationManager.cancel(notificationId);
        mNotificationBuilders.remove(notificationId);
    }

    @Override
    public void cancelTrustRequestNotification(String accountID) {
        if (accountID == null) {
            return;
        }
        int notificationId = getIncomingTrustNotificationId(accountID);
        notificationManager.cancel(notificationId);
    }

    @Override
    public void cancelCallNotification() {
        notificationManager.cancel(NOTIF_CALL_ID);
        mNotificationBuilders.remove(NOTIF_CALL_ID);
    }

    /**\
     * Cancels a notification
     * @param notificationId the notification ID
     * @param isMigratingToService true if the notification is being updated to be a part of the foreground service
     */
    @Override
    public void cancelFileNotification(int notificationId, boolean isMigratingToService) {
        notificationManager.cancel(notificationId);
        if(!isMigratingToService)
            mNotificationBuilders.remove(notificationId);
    }

    @Override
    public void cancelAll() {
        notificationManager.cancelAll();
        mNotificationBuilders.clear();
    }

    private int getIncomingTrustNotificationId(String accountId) {
        return (NOTIF_TRUST_REQUEST + accountId).hashCode();
    }

    private int getTextNotificationId(Uri contact) {
        return (NOTIF_MSG + contact.toString()).hashCode();
    }

    private int getFileTransferNotificationId(long dataTransferId) {
        return (NOTIF_FILE_TRANSFER + dataTransferId).hashCode();
    }

    private Bitmap getContactPicture(CallContact contact) {
        try {
            return AvatarFactory.getBitmapAvatar(mContext, contact, avatarSize).blockingGet();
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap getContactPicture(Account account) {
        return AvatarFactory.getBitmapAvatar(mContext, account, avatarSize).blockingGet();
    }

    private void setContactPicture(CallContact contact, NotificationCompat.Builder messageNotificationBuilder) {
        Bitmap pic = getContactPicture(contact);
        if (pic != null)
            messageNotificationBuilder.setLargeIcon(pic);
    }
}
