/**
 *  Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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

package org.sflphone.utils;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sflphone.model.SipCall;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Typeface;
import android.net.sip.SipProfile;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SipNotifications {

    private final NotificationManager notificationManager;
    private final Context context;
    private Builder inCallNotification;
    private Builder missedCallNotification;
    private Builder messageNotification;
    private Builder messageVoicemail;
    private boolean resolveContacts = true;
    
    public static final String NOTIF_CREATION = "notif_creation";
    public static final String NOTIF_DELETION = "notif_deletion";

    public static final int REGISTER_NOTIF_ID = 1;
    public static final int CALL_NOTIF_ID = REGISTER_NOTIF_ID + 1;
    public static final int CALLLOG_NOTIF_ID = REGISTER_NOTIF_ID + 2;
    public static final int MESSAGE_NOTIF_ID = REGISTER_NOTIF_ID + 3;
    public static final int VOICEMAIL_NOTIF_ID = REGISTER_NOTIF_ID + 4;

    private static boolean isInit = false;

    public SipNotifications(Context aContext) {
        context = aContext;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!isInit) {
            cancelAll();
            cancelCalls();
            isInit = true;
        }
        
        if( ! Compatibility.isCompatible(9) ) {
            searchNotificationPrimaryText(aContext);
        }
    }

    private Integer notificationPrimaryTextColor = null;

    private static String TO_SEARCH = "Search";
    // Retrieve notification textColor with android < 2.3
    @SuppressWarnings("deprecation")
    private void searchNotificationPrimaryText(Context aContext) {
        try {
            Notification ntf = new Notification();
            ntf.setLatestEventInfo(aContext, TO_SEARCH, "", null);
            LinearLayout group = new LinearLayout(aContext);
            ViewGroup event = (ViewGroup) ntf.contentView.apply(aContext, group);
            recurseSearchNotificationPrimaryText(event);
            group.removeAllViews();
        } catch (Exception e) {
            Log.e(THIS_FILE, "Can't retrieve the color", e);
        }
    }
    
    private boolean recurseSearchNotificationPrimaryText(ViewGroup gp) {
        final int count = gp.getChildCount();
        for (int i = 0; i < count; ++i) {
            if (gp.getChildAt(i) instanceof TextView){
                final TextView text = (TextView) gp.getChildAt(i);
                final String szText = text.getText().toString();
                if (TO_SEARCH.equals(szText)) {
                    notificationPrimaryTextColor = text.getTextColors().getDefaultColor();
                    return true;
                }
            } else if (gp.getChildAt(i) instanceof ViewGroup) {
                if(recurseSearchNotificationPrimaryText((ViewGroup) gp.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
    

    // Foreground api

    private static final Class<?>[] SET_FG_SIG = new Class[] { boolean.class };
    private static final Class<?>[] START_FG_SIG = new Class[] { int.class, Notification.class };
    private static final Class<?>[] STOP_FG_SIG = new Class[] { boolean.class };
    private static final String THIS_FILE = "Notifications";

    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    private void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(context, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w(THIS_FILE, "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w(THIS_FILE, "Unable to invoke method", e);
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    private void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        notificationManager.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    private void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API. Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        notificationManager.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }

    private boolean isServiceWrapper = false;

    public void onServiceCreate() {
        try {
            mStartForeground = context.getClass().getMethod("startForeground", START_FG_SIG);
            mStopForeground = context.getClass().getMethod("stopForeground", STOP_FG_SIG);
            isServiceWrapper = true;
            return;
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = context.getClass().getMethod("setForeground", SET_FG_SIG);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("OS doesn't have Service.startForeground OR Service.setForeground!");
        }
        isServiceWrapper = true;
    }

    public void onServiceDestroy() {
        // Make sure our notification is gone.
        cancelAll();
        cancelCalls();
    }

    // Announces

//    // Register
//    public synchronized void notifyRegisteredAccounts(ArrayList<SipProfileState> activeAccountsInfos, boolean showNumbers) {
//        if (!isServiceWrapper) {
//            Log.e(THIS_FILE, "Trying to create a service notification from outside the service");
//            return;
//        }
//        int icon = R.drawable.ic_stat_sipok;
//        CharSequence tickerText = context.getString(R.string.service_ticker_registered_text);
//        long when = System.currentTimeMillis();
//        
//
//        Builder nb = new NotificationCompat.Builder(context);
//        nb.setSmallIcon(icon);
//        nb.setTicker(tickerText);
//        nb.setWhen(when);
//        Intent notificationIntent = new Intent(SipManager.ACTION_SIP_DIALER);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        
//        RegistrationNotification contentView = new RegistrationNotification(context.getPackageName());
//        contentView.clearRegistrations();
//        if(!Compatibility.isCompatible(9)) {
//            contentView.setTextsColor(notificationPrimaryTextColor);
//        }
//        contentView.addAccountInfos(context, activeAccountsInfos);
//
//        // notification.setLatestEventInfo(context, contentTitle,
//        // contentText, contentIntent);
//        nb.setOngoing(true);
//        nb.setOnlyAlertOnce(true);
//        nb.setContentIntent(contentIntent);
//        nb.setContent(contentView);
//        
//        Notification notification = nb.build();
//        notification.flags |= Notification.FLAG_NO_CLEAR;
//        // We have to re-write content view because getNotification setLatestEventInfo implicitly
//        notification.contentView = contentView;
//        if (showNumbers) {
//            // This only affects android 2.3 and lower
//            notification.number = activeAccountsInfos.size();
//        }
//        startForegroundCompat(REGISTER_NOTIF_ID, notification);
//    }
    
    /**
     * Format the remote contact name for the call info
     * @param callInfo the callinfo to format
     * @return the name to display for the contact
     */
    private String formatRemoteContactString(String remoteContact) {
        String formattedRemoteContact = remoteContact;
        //TODO
        return formattedRemoteContact;
    }
    
    /**
     * Format the notification title for a call info
     * @param title
     * @param callInfo
     * @return
     */
    private String formatNotificationTitle(int title, long accId) {
        //TODO
        return null;
    }

    // Calls
    public void showNotificationForCall(SipCall callInfo) {
        //TODO
    }

    public void showNotificationForMissedCall(ContentValues callLog) {
        //TODO
    }

    public void showNotificationForVoiceMail(SipProfile acc, int numberOfMessages) {
        //TODO
    }

    private static String viewingRemoteFrom = null;

    public void setViewingMessageFrom(String remoteFrom) {
        viewingRemoteFrom = remoteFrom;
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

    // Cancels
    public final void cancelRegisters() {
        if (!isServiceWrapper) {
            Log.e(THIS_FILE, "Trying to cancel a service notification from outside the service");
            return;
        }
        stopForegroundCompat(REGISTER_NOTIF_ID);
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
        // Do not cancel calls notification since it's possible that there is
        // still an ongoing call.
        if (isServiceWrapper) {
            cancelRegisters();
        }
        cancelMessages();
        cancelMissedCalls();
        cancelVoicemails();
    }

}