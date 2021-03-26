/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.CarExtender.UnreadConversation;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.views.AvatarFactory;
import cx.ring.fragments.ConversationFragment;
import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conference;
import net.jami.model.Conversation;
import net.jami.model.Interaction;
import net.jami.model.Interaction.InteractionStatus;
import net.jami.model.DataTransfer;
import net.jami.model.Call;
import net.jami.model.TextMessage;
import net.jami.model.Uri;
import cx.ring.service.CallNotificationService;
import cx.ring.service.DRingService;
import cx.ring.settings.SettingsFragment;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.ResourceMapper;

import net.jami.services.AccountService;
import net.jami.services.ContactService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HistoryService;
import net.jami.services.NotificationService;
import net.jami.services.PreferencesService;
import net.jami.utils.Tuple;

public class NotificationServiceImpl implements NotificationService {

    public static final String EXTRA_BUBBLE = "bubble";

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
    public static final String NOTIF_CHANNEL_SYNC = "sync";
    private static final String NOTIF_CHANNEL_SERVICE = "service";

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
    private final LinkedHashMap<Integer, Conference> currentCalls = new LinkedHashMap<>();
    private final ConcurrentHashMap<Integer, Notification> callNotifications = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Notification> dataTransferNotifications = new ConcurrentHashMap<>();

    @SuppressLint("CheckResult")
    public void initHelper() {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(mContext);
        }
        avatarSize = (int) (mContext.getResources().getDisplayMetrics().density * AvatarFactory.SIZE_NOTIF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannels(mContext);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void registerNotificationChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;

        // Setting up groups
        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIF_CALL_GROUP, context.getString(R.string.notif_group_calls)));

        // Missed calls channel
        NotificationChannel missedCallsChannel = new NotificationChannel(NOTIF_CHANNEL_MISSED_CALL, context.getString(R.string.notif_channel_missed_calls), NotificationManager.IMPORTANCE_DEFAULT);
        missedCallsChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        missedCallsChannel.setSound(null, null);
        missedCallsChannel.enableVibration(false);
        missedCallsChannel.setGroup(NOTIF_CALL_GROUP);
        notificationManager.createNotificationChannel(missedCallsChannel);

        // Incoming call channel
        NotificationChannel incomingCallChannel = new NotificationChannel(NOTIF_CHANNEL_INCOMING_CALL, context.getString(R.string.notif_channel_incoming_calls), NotificationManager.IMPORTANCE_HIGH);
        incomingCallChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        incomingCallChannel.setGroup(NOTIF_CALL_GROUP);
        incomingCallChannel.setSound(null, null);
        incomingCallChannel.enableVibration(false);
        notificationManager.createNotificationChannel(incomingCallChannel);

        // Call in progress channel
        NotificationChannel callInProgressChannel = new NotificationChannel(NOTIF_CHANNEL_CALL_IN_PROGRESS, context.getString(R.string.notif_channel_call_in_progress), NotificationManager.IMPORTANCE_DEFAULT);
        callInProgressChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        callInProgressChannel.setSound(null, null);
        callInProgressChannel.enableVibration(false);
        callInProgressChannel.setGroup(NOTIF_CALL_GROUP);
        notificationManager.createNotificationChannel(callInProgressChannel);

        // Text messages channel
        AudioAttributes soundAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build();

        NotificationChannel messageChannel = new NotificationChannel(NOTIF_CHANNEL_MESSAGE, context.getString(R.string.notif_channel_messages), NotificationManager.IMPORTANCE_HIGH);
        messageChannel.enableVibration(true);
        messageChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        messageChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(messageChannel);

        // Contact requests
        NotificationChannel requestsChannel = new NotificationChannel(NOTIF_CHANNEL_REQUEST, context.getString(R.string.notif_channel_requests), NotificationManager.IMPORTANCE_DEFAULT);
        requestsChannel.enableVibration(true);
        requestsChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        requestsChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(requestsChannel);

        // File transfer requests
        NotificationChannel fileTransferChannel = new NotificationChannel(NOTIF_CHANNEL_FILE_TRANSFER, context.getString(R.string.notif_channel_file_transfer), NotificationManager.IMPORTANCE_DEFAULT);
        fileTransferChannel.enableVibration(true);
        fileTransferChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        fileTransferChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes);
        notificationManager.createNotificationChannel(fileTransferChannel);

        // File transfer requests
        NotificationChannel syncChannel = new NotificationChannel(NOTIF_CHANNEL_SYNC, context.getString(R.string.notif_channel_sync), NotificationManager.IMPORTANCE_DEFAULT);
        syncChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        syncChannel.enableLights(false);
        syncChannel.enableVibration(false);
        syncChannel.setShowBadge(false);
        syncChannel.setSound(null, null);
        notificationManager.createNotificationChannel(syncChannel);

        // Background service channel
        NotificationChannel backgroundChannel = new NotificationChannel(NOTIF_CHANNEL_SERVICE, context.getString(R.string.notif_channel_background_service), NotificationManager.IMPORTANCE_LOW);
        backgroundChannel.setDescription(context.getString(R.string.notif_channel_background_service_descr));
        backgroundChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        backgroundChannel.enableLights(false);
        backgroundChannel.enableVibration(false);
        backgroundChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(backgroundChannel);
    }

    /**
     * Starts the call activity directly for Android TV
     *
     * @param callId the call ID
     */
    private void startCallActivity(String callId) {
        mContext.startActivity(new Intent(Intent.ACTION_VIEW)
                .putExtra(KEY_CALL_ID, callId)
                .setClass(mContext.getApplicationContext(), TVCallActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private Notification buildCallNotification(@NonNull Conference conference) {
        String ongoingCallId = null;
        for (Conference conf : currentCalls.values()) {
            if (conf != conference && conf.getState() == Call.CallStatus.CURRENT)
                ongoingCallId = conf.getParticipants().get(0).getDaemonIdString();
        }

        Call call = conference.getParticipants().get(0);

        notificationManager.cancel(NOTIF_CALL_ID);

        PendingIntent gotoIntent = PendingIntent.getService(mContext,
                random.nextInt(),
                new Intent(DRingService.ACTION_CALL_VIEW)
                        .setClass(mContext, DRingService.class)
                        .putExtra(KEY_CALL_ID, call.getDaemonIdString()), 0);

        Contact contact = call.getContact();
        NotificationCompat.Builder messageNotificationBuilder;
        if (conference.isOnGoing()) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS);
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getDisplayName()))
                    .setContentText(mContext.getText(R.string.notif_current_call))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(gotoIntent)
                    .setSound(null)
                    .setVibrate(null)
                    .setColorized(true)
                    .setColor(mContext.getResources().getColor(R.color.color_primary_light))
                    .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_hangup),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_CALL_END)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (conference.isRinging()) {
            if (conference.isIncoming()) {
                messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_INCOMING_CALL);
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getDisplayName()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(mContext.getText(R.string.notif_incoming_call))
                        .setContentIntent(gotoIntent)
                        .setSound(null)
                        .setVibrate(null)
                        .setFullScreenIntent(gotoIntent, true)
                        .addAction(R.drawable.baseline_call_end_24, mContext.getText(R.string.action_call_decline),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_REFUSE)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.baseline_call_24, ongoingCallId == null ?
                                        mContext.getText(R.string.action_call_accept) : mContext.getText(R.string.action_call_end_accept),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(ongoingCallId == null ? DRingService.ACTION_CALL_ACCEPT : DRingService.ACTION_CALL_END_ACCEPT)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_END_ID, ongoingCallId)
                                                .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                        PendingIntent.FLAG_ONE_SHOT));
                if (ongoingCallId != null) {
                    messageNotificationBuilder.addAction(R.drawable.baseline_call_24, mContext.getText(R.string.action_call_hold_accept),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_CALL_HOLD_ACCEPT)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(KEY_HOLD_ID, ongoingCallId)
                                            .putExtra(KEY_CALL_ID, call.getDaemonIdString()),
                                    PendingIntent.FLAG_ONE_SHOT));
                }
            } else {
                messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL_IN_PROGRESS);
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.getDisplayName()))
                        .setContentText(mContext.getText(R.string.notif_outgoing_call))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(gotoIntent)
                        .setSound(null)
                        .setVibrate(null)
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

    @Override
    public Object showCallNotification(int notifId) {
        return callNotifications.remove(notifId);
    }

    @Override
    public void showLocationNotification(Account first, Contact contact) {
        android.net.Uri path = ConversationPath.toUri(first.getAccountID(), contact.getUri());

        Intent intentConversation = new Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity.class)
                .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true);

        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setLargeIcon(getContactPicture(contact))
                .setContentText(mContext.getString(R.string.location_share_contact, contact.getDisplayName()))
                .setContentIntent(PendingIntent.getActivity(mContext, random.nextInt(), intentConversation, 0))
                .setAutoCancel(false)
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));
        notificationManager.notify(Objects.hash( "Location", path), messageNotificationBuilder.build());
    }

    @Override
    public void cancelLocationNotification(Account first, Contact contact) {
        notificationManager.cancel(Objects.hash( "Location", ConversationPath.toUri(first.getAccountID(), contact.getUri())));
    }

    /**
     * Updates a notification
     *
     * @param notification   a built notification object
     * @param notificationId the notification's id
     */
    @Override
    public void updateNotification(Object notification, int notificationId) {
        if(notification != null)
            notificationManager.notify(notificationId, (Notification) notification);
    }

    /**
     * Starts a service (data transfer or call)
     *
     * @param id            the notification id
     */
    private void startForegroundService(int id, Class serviceClass) {
        ContextCompat.startForegroundService(mContext, new Intent(mContext, serviceClass)
                .putExtra(KEY_NOTIFICATION_ID, id));
    }

    /**
     * Handles the creation and destruction of services associated with calls as well as displaying notifications.
     *
     * @param conference the conference object for the notification
     * @param remove     true if it should be removed from current calls
     */
    @Override
    public void handleCallNotification(Conference conference, boolean remove) {
        if (DeviceUtils.isTv(mContext)) {
            if (!remove)
                startCallActivity(conference.getId());
            return;
        }

        Notification notification = null;

        // Build notification
        int id = conference.getId().hashCode();
        currentCalls.remove(id);
        if (!remove) {
            currentCalls.put(id, conference);
            notification = buildCallNotification(conference);
        }
        if (notification == null && !currentCalls.isEmpty()) {
            // Build notification for other calls if any remains
            for (Conference c : currentCalls.values())
                conference = c;
            notification = buildCallNotification(conference);
        }

        // Send notification to the  Service
        if (notification != null) {
            int nid = random.nextInt();
            callNotifications.put(nid, notification);
            ContextCompat.startForegroundService(mContext, new Intent(CallNotificationService.ACTION_START, null, mContext, CallNotificationService.class)
                    .putExtra(KEY_NOTIFICATION_ID, nid));
        } else {
            try {
                mContext.startService(new Intent(CallNotificationService.ACTION_STOP, null, mContext, CallNotificationService.class));
            } catch (Exception e) {
                Log.w(TAG, "Error stopping service", e);
            }
        }
    }

    @Override
    public void onConnectionUpdate(Boolean b) {
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
    @Override
    public void handleDataTransferNotification(DataTransfer transfer, Conversation conversation, boolean remove) {
        Log.d(TAG, "handleDataTransferNotification, a data transfer event is in progress");
        if (DeviceUtils.isTv(mContext)) {
            return;
        }
        if (!remove) {
            showFileTransferNotification(conversation, transfer);
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
            mContext.stopService(new Intent(mContext, DataTransferService.class));
        else {
            startForegroundService(dataTransferNotifications.keySet().iterator().next(), DataTransferService.class);
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

        Log.w(TAG, "showTextNotification start " + accountId + " " + conversation.getUri() + " " + texts.size());

        //TODO handle groups
        if (texts.isEmpty() || conversation.isVisible()) {
            cancelTextNotification(conversation.getUri());
            return;
        }
        if (texts.lastEntry().getValue().isNotified()) {
            return;
        }

        Log.w(TAG, "showTextNotification " + accountId + " " + conversation.getUri());
        mContactService.getLoadedContact(accountId, conversation.getContacts(), false)
                .subscribe(c -> textNotification(accountId, texts, conversation),
                        e -> Log.w(TAG, "Can't load contact", e));
    }

    private void textNotification(String accountId, TreeMap<Long, TextMessage> texts, Conversation conversation) {
        android.net.Uri path = ConversationPath.toUri(conversation.getAccountId(), conversation.getUri());
        Pair<Bitmap, String> conversationProfile = getProfile(conversation);

        int notificationVisibility = mPreferencesService.getSettings().getNotificationVisibility();
        switch (notificationVisibility){
            case SettingsFragment.NOTIFICATION_PUBLIC:
                notificationVisibility = Notification.VISIBILITY_PUBLIC;
                break;
            case SettingsFragment.NOTIFICATION_SECRET:
                notificationVisibility = Notification.VISIBILITY_SECRET;
                break;
            case SettingsFragment.NOTIFICATION_PRIVATE:
            default:
                notificationVisibility = Notification.VISIBILITY_PRIVATE;
        }

        TextMessage last = texts.lastEntry().getValue();
        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService.class);
        Intent intentDelete = new Intent(DRingService.ACTION_CONV_DISMISS, path, mContext, DRingService.class);

        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(notificationVisibility)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentTitle(conversationProfile.second)
                .setContentText(last.getBody())
                .setWhen(last.getTimestamp())
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, 0))
                .setAutoCancel(true)
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));

        String key = ConversationPath.toKey(accountId, conversation.getUri());

        Person contactPerson = new Person.Builder()
                .setKey(key)
                .setName(conversationProfile.second)
                .setIcon(conversationProfile.first == null ? null : IconCompat.createWithBitmap(conversationProfile.first))
                .build();

        if (conversationProfile.first != null) {
            messageNotificationBuilder.setLargeIcon(conversationProfile.first);
            Intent intentBubble = new Intent(Intent.ACTION_VIEW, path, mContext, ConversationActivity.class);
            intentBubble.putExtra(EXTRA_BUBBLE, true);
            messageNotificationBuilder.setBubbleMetadata(new NotificationCompat.BubbleMetadata.Builder()
                    .setDesiredHeight(600)
                    .setIcon(IconCompat.createWithAdaptiveBitmap(conversationProfile.first))
                    .setIntent(PendingIntent.getActivity(mContext, 0, intentBubble,
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .build())
                    .setShortcutId(key);
        }

        UnreadConversation.Builder unreadConvBuilder = new UnreadConversation.Builder(conversationProfile.second)
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

            NotificationCompat.MessagingStyle history = new NotificationCompat.MessagingStyle(userPerson);
            for (TextMessage textMessage : texts.values()) {
                Contact contact = textMessage.getContact();
                Bitmap contactPicture = getContactPicture(contact);
                Person contactPerson = new Person.Builder()
                        .setKey(textMessage.getAuthor())
                        .setName(contact.getDisplayName())
                        .setIcon(contactPicture == null ? null : IconCompat.createWithBitmap(contactPicture))
                        .build();
                history.addMessage(new NotificationCompat.MessagingStyle.Message(
                        textMessage.getBody(),
                        textMessage.getTimestamp(),
                        textMessage.isIncoming() ? contactPerson : null));
                unreadConvBuilder.addMessage(textMessage.getBody());
            }
            messageNotificationBuilder.setStyle(history);
        }

        int notificationId = getTextNotificationId(conversation.getUri());
        int replyId = notificationId + 1;
        int markAsReadId = notificationId + 2;

        CharSequence replyLabel = mContext.getText(R.string.notif_reply);
        RemoteInput remoteInput = new RemoteInput.Builder(DRingService.KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        PendingIntent replyPendingIntent = PendingIntent.getService(mContext, replyId,
                new Intent(DRingService.ACTION_CONV_REPLY_INLINE, path, mContext, DRingService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent readPendingIntent = PendingIntent.getService(mContext, markAsReadId,
                new Intent(DRingService.ACTION_CONV_READ, path, mContext, DRingService.class), 0);

        messageNotificationBuilder
                .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_reply_24, replyLabel, replyPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .addRemoteInput(remoteInput)
                        .extend(new NotificationCompat.Action.WearableExtender()
                                .setHintDisplayActionInline(true))
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
            String contactKey = request.getUri().getRawUriString();
            if (notifiedRequests.contains(contactKey)) {
                return;
            }
            mContactService.getLoadedContact(account.getAccountID(), request.getContacts(), false).subscribe(c -> {
                NotificationCompat.Builder builder = getRequestNotificationBuilder(account.getAccountID());
                mPreferencesService.saveRequestPreferences(account.getAccountID(), contactKey);
                android.net.Uri info = ConversationPath.toUri(account.getAccountID(), request.getUri());
                builder.setContentText(request.getUriTitle())
                        .addAction(R.drawable.baseline_person_add_24, mContext.getText(R.string.accept),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT, info, mContext, DRingService.class),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.baseline_delete_24, mContext.getText(R.string.refuse),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_REFUSE, info, mContext, DRingService.class),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.baseline_block_24, mContext.getText(R.string.block),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_TRUST_REQUEST_BLOCK, info, mContext, DRingService.class),
                                        PendingIntent.FLAG_ONE_SHOT));

                Bitmap pic = getContactPicture(request);
                if (pic != null)
                    builder.setLargeIcon(pic);
                notificationManager.notify(notificationId, builder.build());
            }, e -> Log.w(TAG, "error showing notification", e));
        } else {
            NotificationCompat.Builder builder = getRequestNotificationBuilder(account.getAccountID());
            boolean newRequest = false;
            for (Conversation request : requests) {
                Contact contact = request.getContact();
                if (contact != null) {
                    String contactKey = contact.getUri().getRawRingId();
                    if (!notifiedRequests.contains(contactKey)) {
                        newRequest = true;
                        mPreferencesService.saveRequestPreferences(account.getAccountID(), contactKey);
                    }
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
    public void showFileTransferNotification(Conversation conversation, DataTransfer info) {
        if (info == null) {
            return;
        }
        InteractionStatus event = info.getStatus();
        if (event == null) {
            return;
        }
        long dataTransferId = info.getDaemonId();
        int notificationId = getFileTransferNotificationId(dataTransferId);

        android.net.Uri path = ConversationPath.toUri(info.getAccount(), conversation.getUri());

        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService.class);

        if (event.isOver()) {
            removeTransferNotification(dataTransferId);

            if (info.isOutgoing()) {
                return;
            }

            NotificationCompat.Builder notif = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER)
                    .setSmallIcon(R.drawable.ic_ring_logo_white)
                    .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                    .setAutoCancel(true);

            if (info.showPicture()) {
                File filePath = mDeviceRuntimeService.getConversationPath(conversation.getUri().getRawRingId(), info.getStoragePath());
                Bitmap img;
                try {
                    BitmapDrawable d = (BitmapDrawable) Glide.with(mContext)
                            .load(filePath)
                            .submit()
                            .get();
                    img = d.getBitmap();
                    notif.setContentTitle(mContext.getString(R.string.notif_incoming_picture, conversation.getTitle()));
                    notif.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(img));
                } catch (Exception e) {
                    Log.w(TAG, "Can't load image for notification", e);
                    return;
                }
            } else {
                notif.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title, conversation.getTitle()));
                notif.setStyle(null);
            }
            Bitmap picture = getContactPicture(conversation);
            if (picture != null)
                notif.setLargeIcon(picture);
            notificationManager.notify(random.nextInt(), notif.build());
            return;
        }
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER);
        }

        boolean ongoing = event == InteractionStatus.TRANSFER_ONGOING || event == InteractionStatus.TRANSFER_ACCEPTED;
        String titleMessage = mContext.getString(info.isOutgoing() ? R.string.notif_outgoing_file_transfer_title : R.string.notif_incoming_file_transfer_title, conversation.getTitle());
        messageNotificationBuilder.setContentTitle(titleMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(ongoing)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setContentText(event == Interaction.InteractionStatus.TRANSFER_ONGOING ?
                        Formatter.formatFileSize(mContext, info.getBytesProgress()) + " / " + Formatter.formatFileSize(mContext, info.getTotalSize()) :
                        info.getDisplayName() + ": " + ResourceMapper.getReadableFileTransferStatus(mContext, event))
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));
        Bitmap picture = getContactPicture(conversation);
        if (picture != null)
            messageNotificationBuilder.setLargeIcon(picture);
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
                                    new Intent(DRingService.ACTION_FILE_ACCEPT, ConversationPath.toUri(conversation), mContext, DRingService.class)
                                            .putExtra(DRingService.KEY_TRANSFER_ID, dataTransferId),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.baseline_cancel_24, mContext.getText(R.string.refuse),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_FILE_CANCEL, ConversationPath.toUri(conversation), mContext, DRingService.class)
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
        startForegroundService(notificationId, DataTransferService.class);
    }

    @Override
    public void showMissedCallNotification(Call call) {
        final int notificationId = call.getDaemonIdString().hashCode();
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);
        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MISSED_CALL);
        }

        android.net.Uri path = ConversationPath.toUri(call);

        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT, path, mContext, DRingService.class);

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

    public void cancelTextNotification(String accountId, Uri contact) {
        int notificationId = getTextNotificationId(contact);
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

    private Bitmap getContactPicture(Contact contact) {
        try {
            return AvatarFactory.getBitmapAvatar(mContext, contact, avatarSize, false).blockingGet();
        } catch (Exception e) {
            return null;
        }
    }
    private Bitmap getContactPicture(Conversation conversation) {
        try {
            return AvatarFactory.getBitmapAvatar(mContext, conversation, avatarSize, false).blockingGet();
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap getContactPicture(Account account) {
        return AvatarFactory.getBitmapAvatar(mContext, account, avatarSize).blockingGet();
    }

    private Pair<Bitmap, String> getProfile(Conversation conversation) {
        return Pair.create(getContactPicture(conversation), conversation.getTitle());
    }

    private void setContactPicture(Contact contact, NotificationCompat.Builder messageNotificationBuilder) {
        Bitmap pic = getContactPicture(contact);
        if (pic != null)
            messageNotificationBuilder.setLargeIcon(pic);
    }
}
