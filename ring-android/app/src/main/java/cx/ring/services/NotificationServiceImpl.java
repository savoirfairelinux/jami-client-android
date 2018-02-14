/*
*  Copyright (C) 2017 Savoir-faire Linux Inc.
*
*  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
*  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package cx.ring.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.daemon.DataTransferInfo;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryFileTransfer;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.service.DRingService;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import ezvcard.property.Photo;

public class NotificationServiceImpl extends NotificationService implements Observer<ServiceEvent> {

    private static final String TAG = NotificationServiceImpl.class.getSimpleName();

    private static final String NOTIF_MSG = "MESSAGE";
    private static final String NOTIF_TRUST_REQUEST = "TRUST REQUEST";
    private static final String NOTIF_FILE_TRANSFER = "FILE_TRANSFER";

    private static final String NOTIF_CHANNEL_CALL = "call";
    private static final String NOTIF_CHANNEL_MESSAGE = "messages";
    private static final String NOTIF_CHANNEL_REQUEST = "requests";
    private static final String NOTIF_CHANNEL_FILE_TRANSFER = "file_transfer";

    private final SparseArray<NotificationCompat.Builder> mNotificationBuilders = new SparseArray<>();
    @Inject
    protected Context mContext;
    @Inject
    protected AccountService mAccountService;
    @Inject
    protected CallService mCallService;
    @Inject
    protected PreferencesService mPreferencesService;
    @Inject
    protected HistoryService mHistoryService;
    @Inject
    protected DeviceRuntimeService mDeviceRuntimeService;
    private NotificationManagerCompat notificationManager;
    private Random random;

    public void initHelper() {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(mContext);
        }
        mAccountService.addObserver(this);
        mCallService.addObserver(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannels();
        }
        random = new Random();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;

        // Call channel
        NotificationChannel callChannel = new NotificationChannel(NOTIF_CHANNEL_CALL, mContext.getString(R.string.notif_channel_calls), NotificationManager.IMPORTANCE_HIGH);
        callChannel.enableVibration(true);
        callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(callChannel);

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
    }

    @Override
    public void showCallNotification(Conference conference) {
        if (conference.getParticipants().isEmpty()) {
            return;
        }

        SipCall call = conference.getParticipants().get(0);
        CallContact contact = call.getContact();
        final int notificationId = call.getCallId().hashCode();
        notificationManager.cancel(notificationId);

        PendingIntent gotoIntent = PendingIntent.getService(mContext,
                random.nextInt(),
                new Intent(DRingService.ACTION_CALL_VIEW)
                        .setClass(mContext, DRingService.class)
                        .putExtra(KEY_CALL_ID, call.getCallId()), 0);
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_CALL);

        if (conference.isOnGoing()) {
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getRingUsername()))
                    .setContentText(mContext.getText(R.string.notif_current_call))
                    .setContentIntent(gotoIntent)
                    .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_hangup),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_CALL_END)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(KEY_CALL_ID, call.getCallId()),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (conference.isRinging()) {
            if (conference.isIncoming()) {
                Bundle extras = new Bundle();
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getRingUsername()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(mContext.getText(R.string.notif_incoming_call))
                        .setContentIntent(gotoIntent)
                        .setFullScreenIntent(gotoIntent, true)
                        .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_decline),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_REFUSE)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getCallId()),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.action_call_accept),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_ACCEPT)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getCallId()),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addExtras(extras);
            } else {
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.getRingUsername()))
                        .setContentText(mContext.getText(R.string.notif_outgoing_call))
                        .setContentIntent(gotoIntent)
                        .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_hangup),
                                PendingIntent.getService(mContext, random.nextInt(),
                                        new Intent(DRingService.ACTION_CALL_END)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getCallId()),
                                        PendingIntent.FLAG_ONE_SHOT));
            }
        } else {
            return;
        }

        messageNotificationBuilder.setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white);

        if (contact.getPhoto() != null) {
            Resources res = mContext.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            Bitmap bmp = BitmapUtils.bytesToBitmap(contact.getPhoto());
            if (bmp != null) {
                messageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
            }
        }

        messageNotificationBuilder.setColor(ResourcesCompat.getColor(mContext.getResources(),
                R.color.color_primary_dark, null));

        notificationManager.notify(notificationId, messageNotificationBuilder.build());
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {
        if (texts.isEmpty()) {
            cancelTextNotification(contact);
            return;
        }
        TextMessage last = texts.lastEntry().getValue();

        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, conversation.getLastAccountUsed())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contact.getPhones().get(0).getNumber().toString());

        Intent intentDelete = new Intent(DRingService.ACTION_CONV_DISMISS)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, conversation.getLastAccountUsed())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contact.getPhones().get(0).getNumber().toString());

        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_MESSAGE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentTitle(contact.getDisplayName())
                .setContentText(last.getMessage())
                .setWhen(last.getDate())
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setDeleteIntent(PendingIntent.getService(mContext, random.nextInt(), intentDelete, 0))
                .setAutoCancel(true)
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));

        if (contact.getPhoto() != null) {
            Resources res = mContext.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

            Bitmap bmp = BitmapUtils.bytesToBitmap(contact.getPhoto());
            if (bmp != null) {
                messageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
            }
        }
        if (texts.size() == 1) {
            last.setNotified(true);
            messageNotificationBuilder.setStyle(null);
        } else {
            ArrayList<Spanned> txts = new ArrayList<>(3);
            int i = 0;
            for (TextMessage textMessage : texts.descendingMap().values()) {
                if (i == 5)
                    break;
                txts.add(0, Html.fromHtml("<b>" + DateUtils.formatDateTime(mContext, textMessage.getDate(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL) + "</b> " + textMessage.getMessage()));
                textMessage.setNotified(true);
                i++;
            }
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for (Spanned spanned : txts) {
                inboxStyle.addLine(spanned);
            }
            messageNotificationBuilder.setStyle(inboxStyle);
        }

        Intent intentRead = new Intent(DRingService.ACTION_CONV_READ)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, conversation.getLastAccountUsed())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contact.getPhones().get(0).getNumber().toString());

        messageNotificationBuilder.addAction(0, mContext.getString(R.string.notif_mark_as_read), PendingIntent.getService(mContext, Long.valueOf(System.currentTimeMillis()).intValue(), intentRead, 0));
        int notificationId = getTextNotificationId(contact);
        notificationManager.notify(notificationId, messageNotificationBuilder.build());
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
    }

    @Override
    public void showIncomingTrustRequestNotification(Account account) {
        int notificationId = getIncomingTrustNotificationId(account.getAccountID());
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);

        if (messageNotificationBuilder != null) {
            notificationManager.cancel(notificationId);
        } else {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_REQUEST);
        }

        Collection<TrustRequest> requests = account.getRequests();
        if (requests.isEmpty()) {
            return;
        } else if (requests.size() == 1) {
            TrustRequest request = requests.iterator().next();
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_REQUEST);
            Bundle info = new Bundle();
            info.putString(TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID, account.getAccountID());
            info.putString(TRUST_REQUEST_NOTIFICATION_FROM, request.getContactId());
            messageNotificationBuilder.setContentText(request.getDisplayname())
                    .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.accept),
                            PendingIntent.getService(mContext, random.nextInt(),
                                    new Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT)
                                            .setClass(mContext, DRingService.class)
                                            .putExtras(info),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.ic_delete_white, mContext.getText(R.string.refuse),
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
            Resources res = mContext.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            List<Photo> photos = request.getVCard().getPhotos();
            if (photos != null && !photos.isEmpty()) {
                Photo photo = photos.get(0);
                messageNotificationBuilder.setLargeIcon(BitmapUtils.bytesToBitmap(photo.getData()));
            } else {
                Bitmap bmp;
                bmp = BitmapFactory.decodeResource(res, R.drawable.ic_contact_picture);
                if (bmp != null) {
                    messageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
                }
            }
        } else {
            messageNotificationBuilder.setContentText(String.format(mContext.getString(R.string.contact_request_msg), Integer.toString(requests.size())));
            messageNotificationBuilder.setLargeIcon(null);
            messageNotificationBuilder.mActions.clear();
        }

        messageNotificationBuilder
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setContentTitle(mContext.getString(R.string.contact_request_title));
        Intent intentOpenTrustRequestFragment = new Intent(HomeActivity.ACTION_PRESENT_TRUST_REQUEST_FRAGMENT)
                .setClass(mContext, HomeActivity.class)
                .putExtra(ContactRequestsFragment.ACCOUNT_ID, account.getAccountID());
        messageNotificationBuilder.setContentIntent(PendingIntent.getActivity(mContext,
                random.nextInt(), intentOpenTrustRequestFragment, PendingIntent.FLAG_ONE_SHOT));

        messageNotificationBuilder.setColor(ResourcesCompat.getColor(mContext.getResources(),
                R.color.color_primary_dark, null));

        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
        notificationManager.notify(notificationId, messageNotificationBuilder.build());
    }

    @Override
    public void showFileTransferNotification(Long dataTransferId, String contactAccountId) {
        if (dataTransferId == null || contactAccountId == null) {
            return;
        }

        String contactUri = new Uri(contactAccountId).getRawUriString();

        Intent intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                .setClass(mContext, DRingService.class)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, mAccountService.getCurrentAccount().getAccountID())
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactUri);

        int notificationId = getFileTransferNotificationId(dataTransferId);
        NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);

        if (messageNotificationBuilder == null) {
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIF_CHANNEL_FILE_TRANSFER);
        } else {
            notificationManager.cancel(notificationId);
        }

        messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_file_transfer_title))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setContentText(mContext.getString(R.string.notif_incoming_file_transfer))
                .setContentIntent(PendingIntent.getService(mContext, random.nextInt(), intentConversation, 0))
                .setColor(ResourcesCompat.getColor(mContext.getResources(), R.color.color_primary_dark, null));

        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
        notificationManager.notify(notificationId, messageNotificationBuilder.build());
    }

    @Override
    public void cancelTextNotification(CallContact contact) {
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
        mNotificationBuilders.remove(notificationId);
    }

    @Override
    public void cancelCallNotification(int notificationId) {
        notificationManager.cancel(notificationId);
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

    private int getTextNotificationId(CallContact contact) {
        return (NOTIF_MSG + contact.getPhones().get(0).getNumber().toString()).hashCode();
    }

    private int getFileTransferNotificationId(Long dataTransferId) {
        return (NOTIF_FILE_TRANSFER + dataTransferId).hashCode();
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (observable instanceof AccountService && event != null) {
            switch (event.getEventType()) {
                case INCOMING_TRUST_REQUEST: {
                    final String accountID = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                    final String from = event.getEventInput(ServiceEvent.EventInput.FROM, String.class);
                    Log.d(TAG, "update: INCOMING_TRUST_REQUEST " + accountID + " " + from);
                    Account account = mAccountService.getAccount(accountID);
                    Set<String> notifiedRequests = mPreferencesService.loadRequestsPreferences(accountID);
                    if (notifiedRequests == null || !notifiedRequests.contains(from)) {
                        showIncomingTrustRequestNotification(account);
                        mPreferencesService.saveRequestPreferences(accountID, from);
                    } else {
                        Log.d(TAG, "INCOMING_TRUST_REQUEST: already notified for " + from);
                    }
                    break;
                }
            }
        } else if (observable instanceof CallService && event != null) {
            switch (event.getEventType()) {
                case DATA_TRANSFER: {
                    Long transferId = event.getEventInput(ServiceEvent.EventInput.TRANSFER_ID, Long.class);
                    DataTransferEventCode transferEventCode = event.getEventInput(ServiceEvent.EventInput.TRANSFER_EVENT_CODE, DataTransferEventCode.class);
                    DataTransferInfo dataTransferInfo = new DataTransferInfo();
                    mCallService.dataTransferInfo(transferId, dataTransferInfo);

                    if (transferEventCode == DataTransferEventCode.CREATED) {

                        HistoryFileTransfer historyFileTransfer = new HistoryFileTransfer(transferId, dataTransferInfo.getDisplayName(),
                                dataTransferInfo.getFlags() == 0, dataTransferInfo.getTotalSize(),
                                dataTransferInfo.getBytesProgress(), dataTransferInfo.getPeer(),
                                dataTransferInfo.getAccountId());
                        mHistoryService.addFileTransfer(historyFileTransfer);

                        if (dataTransferInfo.getFlags() == 1){
                            showFileTransferNotification(transferId, dataTransferInfo.getPeer());
                        }
                    }

                    if (dataTransferInfo.getFlags() == 1) {
                        mHistoryService.updateFileTransferStatus(transferId, transferEventCode);
                    }

                    if (transferEventCode == DataTransferEventCode.FINISHED) {
                        notificationManager.cancel(getFileTransferNotificationId(transferId));
                    }
                    break;
                }
            }
        }
    }
}