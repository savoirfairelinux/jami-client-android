/*
*  Copyright (C) 2017 Savoir-faire Linux Inc.
*
*  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.text.format.DateUtils;

import java.util.Random;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.LocalService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class NotificationServiceImpl extends NotificationService implements Observer<ServiceEvent> {

    private static final String TAG = NotificationServiceImpl.class.getName();

    @Inject
    Context mContext;

    @Inject
    AccountService mAccountService;

    private NotificationCompat.Builder mMessageNotificationBuilder;
    private NotificationManagerCompat notificationManager;

    public void initHelper() {
        if (mMessageNotificationBuilder == null) {
            mMessageNotificationBuilder = new NotificationCompat.Builder(mContext);
        }
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(mContext);
        }
        mAccountService.addObserver(this);
    }

    @Override
    public void showCallNotification(Conference conference) {

        notificationManager.cancel(conference.getUuid());

        if (conference.getParticipants().isEmpty()) {
            return;
        }

        SipCall call = conference.getParticipants().get(0);
        CallContact contact = call.getContact();

        String[] split = contact.getDisplayName().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }

        final Uri callUri = Uri.withAppendedPath(ContentUriHandler.CALL_CONTENT_URI, call.getCallId());
        PendingIntent gotoIntent = PendingIntent.getActivity(mContext, new Random().nextInt(),
                ActionHelper.getViewIntent(mContext, conference), PendingIntent.FLAG_ONE_SHOT);

        if (conference.isOnGoing()) {
            mMessageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getDisplayName()))
                    .setContentText(mContext.getText(R.string.notif_current_call))
                    .setContentIntent(gotoIntent)
                    .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_hangup),
                            PendingIntent.getService(mContext, new Random().nextInt(),
                                    new Intent(LocalService.ACTION_CALL_END)
                                            .setClass(mContext, LocalService.class)
                                            .setData(callUri),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (conference.isRinging()) {
            if (conference.isIncoming()) {
                Bundle extras = new Bundle();
                extras.putBoolean(CallManagerCallBack.INCOMING_CALL, true);
                mMessageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getDisplayName()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(mContext.getText(R.string.notif_incoming_call))
                        .setContentIntent(gotoIntent)
                        .setFullScreenIntent(gotoIntent, true)
                        .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_decline),
                                PendingIntent.getService(mContext, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_REFUSE)
                                                .setClass(mContext, LocalService.class)
                                                .setData(callUri),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_action_accept, mContext.getText(R.string.action_call_accept),
                                PendingIntent.getService(mContext, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_ACCEPT)
                                                .setClass(mContext, LocalService.class)
                                                .setData(callUri),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addExtras(extras);
            } else {
                mMessageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.getDisplayName()))
                        .setContentText(mContext.getText(R.string.notif_outgoing_call))
                        .setContentIntent(gotoIntent)
                        .addAction(R.drawable.ic_call_end_white, mContext.getText(R.string.action_call_hangup),
                                PendingIntent.getService(mContext, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_END)
                                                .setClass(mContext, LocalService.class)
                                                .setData(callUri),
                                        PendingIntent.FLAG_ONE_SHOT));
            }

        } else {
            notificationManager.cancel(conference.getUuid());
            return;
        }

        mMessageNotificationBuilder.setOngoing(true).setCategory(NotificationCompat.CATEGORY_CALL).setSmallIcon(R.drawable.ic_ring_logo_white);

        if (contact.getPhoto() != null) {
            Resources res = mContext.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            Bitmap bmp = BitmapUtils.bytesToBitmap(contact.getPhoto());
            if (bmp != null) {
                mMessageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
            }
        }

        int notificationId = splitAddress(contact.getDisplayName());
        notificationManager.notify(notificationId, mMessageNotificationBuilder.build());
    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {
        mMessageNotificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white);
        mMessageNotificationBuilder.setContentTitle(contact.getDisplayName());

        String[] split = contact.getDisplayName().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }
        Intent intentConversation;
        if (ConversationFragment.isTabletMode(mContext)) {
            intentConversation = new Intent(LocalService.ACTION_CONV_ACCEPT)
                    .setClass(mContext, HomeActivity.class)
                    .putExtra("conversationID", contact.getIds().get(0));
        } else {
            intentConversation = new Intent(Intent.ACTION_VIEW)
                    .setClass(mContext, ConversationActivity.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));
        }

        Intent intentDelete = new Intent(LocalService.ACTION_CONV_READ)
                .setClass(mContext, LocalService.class)
                .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));
        mMessageNotificationBuilder.setContentIntent(PendingIntent.getActivity(mContext, new Random().nextInt(), intentConversation, 0))
                .setDeleteIntent(PendingIntent.getService(mContext, new Random().nextInt(), intentDelete, 0));

        if (contact.getPhoto() != null) {
            Resources res = mContext.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

            Bitmap bmp = BitmapUtils.bytesToBitmap(contact.getPhoto());
            if (bmp != null) {
                mMessageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
            }
        }
        if (texts.size() == 1) {
            TextMessage txt = texts.firstEntry().getValue();
            txt.setNotified(true);
            mMessageNotificationBuilder.setContentText(txt.getMessage());
            mMessageNotificationBuilder.setStyle(null);
            mMessageNotificationBuilder.setWhen(txt.getTimestamp());
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for (TextMessage s : texts.values()) {
                inboxStyle.addLine(Html.fromHtml("<b>" + DateUtils.formatDateTime(mContext, s.getTimestamp(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL) + "</b> " + s.getMessage()));
                s.setNotified(true);
            }
            mMessageNotificationBuilder.setContentText(texts.lastEntry().getValue().getMessage());
            mMessageNotificationBuilder.setStyle(inboxStyle);
            mMessageNotificationBuilder.setWhen(texts.lastEntry().getValue().getTimestamp());
        }

        int notificationId = splitAddress(contact.getDisplayName());
        notificationManager.notify(notificationId, mMessageNotificationBuilder.build());
    }

    @Override
    public void cancel(String address) {
        notificationManager.cancel(splitAddress(address));
    }

    @Override
    public void cancelAll() {
        notificationManager.cancelAll();
    }

    private int splitAddress(String address) {
        int notificationID = address.hashCode();
        String[] split = address.split(":");
        if (split.length > 1) {
            notificationID = split[1].hashCode();
        }
        return notificationID;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

            }
        }
    };

    @Override
    public void update(Observable observable, ServiceEvent arg) {
        if (observable instanceof AccountService && arg != null) {
            if (ServiceEvent.EventType.REGISTERED_NAME_FOUND.equals(arg.getEventType())) {
                final String name = arg.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = arg.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = arg.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);

                //state 0: name found
                if (mMessageNotificationBuilder != null && state == 0) {
                    Bundle extras = mMessageNotificationBuilder.getExtras();
                    if (extras != null) {
                        if (extras.getBoolean(CallManagerCallBack.INCOMING_CALL, false)) {
                            mMessageNotificationBuilder.setContentTitle(mContext.getApplicationContext().getString(R.string.notif_incoming_call_title, name));
                        } else {
                            mMessageNotificationBuilder.setContentTitle(name);
                        }
                    } else {
                        mMessageNotificationBuilder.setContentTitle(name);
                    }
                    notificationManager.notify(address.hashCode(), mMessageNotificationBuilder.build());
                }
            }
        }
    }
}
