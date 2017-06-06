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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.SparseArray;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.R;
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

    private static final String NOTIF_CALL = "CALL";
    private static final String NOTIF_MSG = "MESSAGE";
    private static final String NOTIF_TRUST_REQUEST = "TRUST REQUEST";

    private static final String EXTRAS_NUMBER_TRUST_REQUEST_KEY = BuildConfig.APPLICATION_ID + "numberOfTrustRequestKey";
    private static final String EXTRAS_TRUST_REQUEST_FROM_KEY = BuildConfig.APPLICATION_ID + "trustRequestFrom";


    @Inject
    protected Context mContext;

    @Inject
    protected AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    protected PreferencesService mPreferencesService;

    private NotificationManagerCompat notificationManager;

    private final SparseArray<NotificationCompat.Builder> mNotificationBuilders = new SparseArray<>();

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
        NotificationCompat.Builder messageNotificationBuilder = new NotificationCompat.Builder(mContext);

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

        String[] split = contact.getDisplayName().split(":");
        if (split.length > 1) {
            mAccountService.lookupAddress("", "", split[1]);
        }
    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {

    }

    @Override
    public void showIncomingTrustRequestNotification(Account account) {

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
    public void cancelTextNotification(CallContact contact) {

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
                default:
                    Log.d(TAG, "Event " + arg.getEventType() + " is not handled here");
                    break;
            }
        }
    }
}