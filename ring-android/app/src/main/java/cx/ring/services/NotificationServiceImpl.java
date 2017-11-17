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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.SparseArray;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.DRingService;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import ezvcard.property.Photo;

public class NotificationServiceImpl extends NotificationService implements Observer<ServiceEvent> {

    private static final String TAG = NotificationServiceImpl.class.getSimpleName();

    private static final String NOTIF_MSG = "MESSAGE";
    private static final String NOTIF_TRUST_REQUEST = "TRUST REQUEST";

    private final SparseArray<NotificationCompat.Builder> mNotificationBuilders = new SparseArray<>();
    @Inject
    protected Context mContext;
    @Inject
    protected AccountService mAccountService;
    @Inject
    protected PreferencesService mPreferencesService;
    @Inject
    DeviceRuntimeService mDeviceRuntimeService;
    private NotificationManagerCompat notificationManager;

    public void initHelper() {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(mContext);
        }
        mAccountService.addObserver(this);
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
                new Random().nextInt(),
                new Intent(DRingService.ACTION_CALL_VIEW)
                        .setClass(mContext, DRingService.class)
                        .putExtra(KEY_CALL_ID, call.getCallId()), 0);
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, createNotificationChannel());

        if (conference.isOnGoing()) {
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getRingUsername()))
                    .setContentText(mContext.getText(R.string.notif_current_call))
                    .setContentIntent(gotoIntent)
                    .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_hangup),
                            PendingIntent.getService(mContext, new Random().nextInt(),
                                    new Intent(DRingService.ACTION_CALL_END)
                                            .setClass(mContext, DRingService.class)
                                            .putExtra(KEY_CALL_ID, call.getCallId()),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (conference.isRinging()) {
            if (conference.isIncoming()) {
                Bundle extras = new Bundle();
                extras.putBoolean(CallManagerCallBack.INCOMING_CALL, true);
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getRingUsername()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(mContext.getText(R.string.notif_incoming_call))
                        .setContentIntent(gotoIntent)
                        .setFullScreenIntent(gotoIntent, true)
                        .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_decline),
                                PendingIntent.getService(mContext, new Random().nextInt(),
                                        new Intent(DRingService.ACTION_CALL_REFUSE)
                                                .setClass(mContext, DRingService.class)
                                                .putExtra(KEY_CALL_ID, call.getCallId()),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.action_call_accept),
                                PendingIntent.getService(mContext, new Random().nextInt(),
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
                                PendingIntent.getService(mContext, new Random().nextInt(),
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

        notificationManager.notify(notificationId, messageNotificationBuilder.build());
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext, createNotificationChannel());

        messageNotificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white);
        messageNotificationBuilder.setContentTitle(contact.getDisplayName());

        Intent intentConversation;
        if (ConversationFragment.isTabletMode(mContext)) {
            intentConversation = new Intent(DRingService.ACTION_CONV_ACCEPT)
                    .setClass(mContext, HomeActivity.class)
                    .putExtra(ConversationFragment.KEY_CONVERSATION_ID, contact.getIds().get(0));
        } else {
            intentConversation = new Intent(Intent.ACTION_VIEW)
                    .setClass(mContext, ConversationActivity.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));
        }

        Intent intentDelete = new Intent(DRingService.ACTION_CONV_READ)
                .setClass(mContext, DRingService.class)
                .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));

        messageNotificationBuilder.setContentIntent(PendingIntent.getActivity(mContext, new Random().nextInt(), intentConversation, 0))
                .setDeleteIntent(PendingIntent.getService(mContext, new Random().nextInt(), intentDelete, 0));

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
            TextMessage txt = texts.firstEntry().getValue();
            txt.setNotified(true);
            messageNotificationBuilder.setContentText(txt.getMessage());
            messageNotificationBuilder.setStyle(null);
            messageNotificationBuilder.setWhen(txt.getTimestamp());
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for (TextMessage s : texts.values()) {
                inboxStyle.addLine(Html.fromHtml("<b>" + DateUtils.formatDateTime(mContext, s.getTimestamp(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL) + "</b> " + s.getMessage()));
                s.setNotified(true);
            }
            messageNotificationBuilder.setContentText(texts.lastEntry().getValue().getMessage());
            messageNotificationBuilder.setStyle(inboxStyle);
            messageNotificationBuilder.setWhen(texts.lastEntry().getValue().getTimestamp());
        }

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
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, createNotificationChannel());
        }

        Collection<TrustRequest> requests = account.getRequests();
        if (requests.isEmpty()) {
            return;
        } else if (requests.size() == 1) {
            TrustRequest request = requests.iterator().next();
            messageNotificationBuilder = new NotificationCompat.Builder(mContext, createNotificationChannel());
            Bundle info = new Bundle();
            info.putString(TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID, account.getAccountID());
            info.putString(TRUST_REQUEST_NOTIFICATION_FROM, request.getContactId());
            messageNotificationBuilder.setContentText(request.getDisplayname())
                    .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.accept),
                            PendingIntent.getService(mContext, new Random().nextInt(),
                                    new Intent(DRingService.ACTION_TRUST_REQUEST_ACCEPT)
                                            .setClass(mContext, DRingService.class)
                                            .putExtras(info),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.ic_delete_white, mContext.getText(R.string.refuse),
                            PendingIntent.getService(mContext, new Random().nextInt(),
                                    new Intent(DRingService.ACTION_TRUST_REQUEST_REFUSE)
                                            .setClass(mContext, DRingService.class)
                                            .putExtras(info),
                                    PendingIntent.FLAG_ONE_SHOT))
                    .addAction(R.drawable.ic_close_white, mContext.getText(R.string.block),
                            PendingIntent.getService(mContext, new Random().nextInt(),
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
                new Random().nextInt(), intentOpenTrustRequestFragment, PendingIntent.FLAG_ONE_SHOT));

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
        cx.ring.model.Uri uri = new cx.ring.model.Uri(contact.getDisplayName());
        return (NOTIF_MSG + uri.getRawUriString()).hashCode();
    }

    private String createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return "default";
        }
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = mContext.getString(R.string.app_name);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, importance);
        notificationChannel.enableVibration(false);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(notificationChannel);
        return channelId;
    }

    @Override
    public void update(Observable observable, ServiceEvent arg) {
        if (observable instanceof AccountService && arg != null) {
            switch (arg.getEventType()) {
                case INCOMING_TRUST_REQUEST: {
                    final String accountID = arg.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                    final String from = arg.getEventInput(ServiceEvent.EventInput.FROM, String.class);
                    Log.d(TAG, "INCOMING_TRUST_REQUEST " + accountID + " " + from);
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
        }
    }
}