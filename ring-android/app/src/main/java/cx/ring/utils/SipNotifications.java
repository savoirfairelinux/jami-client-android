/**
 *  Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Adrien Béraud <adrien.beraud@gmail.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.utils;

import java.util.HashMap;
import java.util.Random;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.sip.SipProfile;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

public class SipNotifications {

    public final NotificationManagerCompat notificationManager;
    private final Context context;

    public static final String NOTIF_CREATION = "notif_creation";
    public static final String NOTIF_DELETION = "notif_deletion";

    private final int NOTIFICATION_ID = new Random().nextInt(1000);

    public static final int REGISTER_NOTIF_ID = 1;
    public static final int CALL_NOTIF_ID = REGISTER_NOTIF_ID + 1;
    public static final int CALLLOG_NOTIF_ID = REGISTER_NOTIF_ID + 2;
    public static final int MESSAGE_NOTIF_ID = REGISTER_NOTIF_ID + 3;
    public static final int VOICEMAIL_NOTIF_ID = REGISTER_NOTIF_ID + 4;

    private static boolean isInit = false;

    public SipNotifications(Context aContext) {
        context = aContext;
        notificationManager = NotificationManagerCompat.from(aContext);
        ;//(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!isInit) {
            cancelAll();
            cancelCalls();
            isInit = true;
        }

    }

    public void onServiceCreate() {

    }

    public void onServiceDestroy() {
        // Make sure our notification is gone.
        cancelAll();
        cancelCalls();
    }

    // Calls
    public void showNotificationForCall(SipCall callInfo) {
        // TODO
    }

    public void showNotificationForVoiceMail(SipProfile acc, int numberOfMessages) {
        // TODO
    }

    protected static CharSequence buildTickerMessage(Context context, String address, String body) {
        String displayAddress = address;

        StringBuilder buf = new StringBuilder(displayAddress == null ? "" : displayAddress.replace('\n', ' ').replace('\r', ' '));
        buf.append(':').append(' ');

        int offset = buf.length();

        if (!TextUtils.isEmpty(body)) {
            body = body.replace('\n', ' ').replace('\r', ' ');
            buf.append(body);
        }

        SpannableString spanText = new SpannableString(buf.toString());
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, offset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanText;
    }

    public final void cancelCalls() {
        notificationManager.cancel(CALL_NOTIF_ID);
    }

    public final void cancelMissedCalls() {
        notificationManager.cancel(CALLLOG_NOTIF_ID);
    }

    public final void cancelMessages() {
        notificationManager.cancel(MESSAGE_NOTIF_ID);
    }

    public final void cancelVoicemails() {
        notificationManager.cancel(VOICEMAIL_NOTIF_ID);
    }

    public final void cancelAll() {
        cancelMessages();
        cancelMissedCalls();
        cancelVoicemails();
    }

    public void publishMissedCallNotification(Conference missedConf) {

        CharSequence tickerText = context.getString(R.string.notif_missed_call_title);
        long when = System.currentTimeMillis();

        Builder nb = new NotificationCompat.Builder(context);
        nb.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
        nb.setSmallIcon(R.drawable.ic_action_call);

        nb.setTicker(tickerText);
        nb.setWhen(when);
        nb.setContentTitle(context.getString(R.string.notif_missed_call_title));
        nb.setContentText(context.getString(R.string.notif_missed_call_content, missedConf.getParticipants().get(0).getContact().getDisplayName()));
        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // notification.setLatestEventInfo(context, contentTitle,
        // contentText, contentIntent);
        nb.setOnlyAlertOnce(true);
        nb.setContentIntent(contentIntent);

        // We have to re-write content view because getNotification setLatestEventInfo implicitly
        // notification.contentView = contentView;

        // startForegroundCompat(CALL_NOTIF_ID, notification);
        notificationManager.notify(CALL_NOTIF_ID, nb.build());
    }

    public void makeNotification(HashMap<String, SipCall> calls) {
        if (calls.size() == 0) {
            return;
        }
        Intent notificationIntent = new Intent(context, HomeActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 007, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID); // clear previous notifications.

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(contentIntent).setOngoing(true).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(calls.size() + " ongoing calls").setTicker("Pending calls").setWhen(System.currentTimeMillis()).setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void removeNotification() {
        //NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}