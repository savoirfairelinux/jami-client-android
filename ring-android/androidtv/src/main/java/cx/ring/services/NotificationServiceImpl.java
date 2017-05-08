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

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TextMessage;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class NotificationServiceImpl extends NotificationService implements Observer<ServiceEvent> {

    private static final String TAG = NotificationServiceImpl.class.getName();

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
    protected PreferencesService mPreferencesService;

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
    public void update(Observable observable, ServiceEvent event) {

    }

    @Override
    public void showCallNotification(Conference conference) {

    }

    @Override
    public void showTextNotification(CallContact contact, Conversation conversation, TreeMap<Long, TextMessage> texts) {

    }

    @Override
    public void cancelCallNotification(int notificationId) {

    }

    @Override
    public void cancelTextNotification(CallContact contact) {

    }

    @Override
    public void cancelAll() {

    }

    @Override
    public void showIncomingTrustRequestNotification(Account account) {

    }

    @Override
    public void cancelTrustRequestNotification(String accountID) {

    }
}