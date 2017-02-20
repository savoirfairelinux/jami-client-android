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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.text.format.DateUtils;

import java.util.HashMap;
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
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class NotificationServiceImpl extends NotificationService implements Observer<ServiceEvent> {

    private static final String TAG = NotificationServiceImpl.class.getName();

    private static final String NOTIF_CALL = "CALL";
    private static final String NOTIF_MSG = "MESSAGE";
    private static final String NOTIF_TRUST_REQUEST = "TRUST REQUEST";

    @Inject
    Context mContext;

    @Inject
    AccountService mAccountService;

    private NotificationManagerCompat notificationManager;

    private HashMap<Integer, NotificationCompat.Builder> mNotificationBuilders;

    public void initHelper() {
        mNotificationBuilders = new HashMap<>();
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
        final int notificationId = getCallNotificationId(call);
        notificationManager.cancel(notificationId);

        final Uri callUri = Uri.withAppendedPath(ContentUriHandler.CALL_CONTENT_URI, call.getCallId());
        PendingIntent gotoIntent = PendingIntent.getActivity(mContext, new Random().nextInt(),
                ActionHelper.getViewIntent(mContext, conference), PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext);

        if (conference.isOnGoing()) {
            messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_current_call_title, contact.getDisplayName()))
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
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_incoming_call_title, contact.getDisplayName()))
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
                messageNotificationBuilder.setContentTitle(mContext.getString(R.string.notif_outgoing_call_title, contact.getDisplayName()))
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

        String[] split = contact.getDisplayName().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }
    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext);

        messageNotificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_ring_logo_white);
        messageNotificationBuilder.setContentTitle(contact.getDisplayName());

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
        String[] split = contact.getDisplayName().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }
    }

    @Override
    public void showIncomingTrustRequestNotification(String accountID) {
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext);

        messageNotificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_ring_logo_white)
                .setContentTitle(mContext.getString(R.string.trust_request_title))
                .setContentText(String.format(mContext.getString(R.string.trust_request_msg),accountID));
        Intent intentOpenTrustRequestFragment = new Intent(LocalService.ACTION_SHOW_TRUST_REQUEST)
                .setClass(mContext, HomeActivity.class)
                .putExtra("accountID", accountID);
        messageNotificationBuilder.setContentIntent(PendingIntent.getActivity(mContext,
                new Random().nextInt(), intentOpenTrustRequestFragment, 0));
        Resources res = mContext.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        Bitmap bmp;
        bmp = BitmapFactory.decodeResource(res,R.drawable.ic_contact_picture);
        if (bmp != null) {
                messageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
        }
        int notificationId = getIncomingTrustNotificationId(accountID);
        notificationManager.notify(notificationId, messageNotificationBuilder.build());
        mNotificationBuilders.put(notificationId, messageNotificationBuilder);
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
    public void cancelCallNotification(SipCall call) {
        if (call == null) {
            return;
        }
        int notificationId = getCallNotificationId(call);
        notificationManager.cancel(notificationId);
        mNotificationBuilders.remove(notificationId);
    }

    @Override
    public void cancelAll() {
        notificationManager.cancelAll();
        mNotificationBuilders.clear();
    }

    private int getIncomingTrustNotificationId(String accountID) {
        cx.ring.model.Uri uri = new cx.ring.model.Uri(accountID);
        return (NOTIF_TRUST_REQUEST + uri.getRawUriString()).hashCode();
    }

    private int getCallNotificationId(SipCall call) {
        cx.ring.model.Uri uri = new cx.ring.model.Uri(call.getContact().getDisplayName());
        return (NOTIF_CALL + uri.getRawUriString()).hashCode();
    }

    private int getTextNotificationId(CallContact contact) {
        cx.ring.model.Uri uri = new cx.ring.model.Uri(contact.getDisplayName());
        return (NOTIF_MSG + uri.getRawUriString()).hashCode();
    }

    private int getCallNotificationId(String uri) {
        cx.ring.model.Uri formattedUri = new cx.ring.model.Uri(uri);
        return (NOTIF_CALL + formattedUri.getRawUriString()).hashCode();
    }

    private int getTextNotificationId(String uri) {
        cx.ring.model.Uri formattedUri = new cx.ring.model.Uri(uri);
        return (NOTIF_MSG + formattedUri.getRawUriString()).hashCode();
    }

    @Override
    public void update(Observable observable, ServiceEvent arg) {
        if (observable instanceof AccountService && arg != null) {
            if (ServiceEvent.EventType.REGISTERED_NAME_FOUND.equals(arg.getEventType())) {
                final String name = arg.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = arg.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = arg.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);

                Log.i(TAG, "Updating name " + name + " for address " + address);

                //state 0: name found
                if (state != 0) {
                    return;
                }

                // Try to update existing Call notification
                int notificationId = getCallNotificationId(address);
                NotificationCompat.Builder messageNotificationBuilder = mNotificationBuilders.get(notificationId);
                if (messageNotificationBuilder != null) {
                    updateNotification(messageNotificationBuilder, notificationId, name);
                }

                // Try to update existing Text notification
                notificationId = getTextNotificationId(address);
                messageNotificationBuilder = mNotificationBuilders.get(notificationId);
                if (messageNotificationBuilder != null) {
                    updateNotification(messageNotificationBuilder, notificationId, name);
                }
            }
        }
    }

    private void updateNotification(NotificationCompat.Builder messageNotificationBuilder,int notificationId,String name) {
        Bundle extras = messageNotificationBuilder.getExtras();
        if (extras != null) {
            if (extras.getBoolean(CallManagerCallBack.INCOMING_CALL, false)) {
                messageNotificationBuilder.setContentTitle(mContext.getApplicationContext().getString(R.string.notif_incoming_call_title, name));
            } else {
                messageNotificationBuilder.setContentTitle(name);
            }
        } else {
            messageNotificationBuilder.setContentTitle(name);
        }

        notificationManager.notify(notificationId, messageNotificationBuilder.build());
    }
}
