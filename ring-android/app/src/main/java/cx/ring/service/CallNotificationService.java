/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
package cx.ring.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.services.NotificationService;

public class CallNotificationService extends Service {

    private boolean isFirst = true;
    private static final int NOTIF_CALL_ID = 1001;

    @Inject
    NotificationService mNotificationService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Notification notification = (Notification) mNotificationService.showCallNotification(intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1));


        if (notification == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (isFirst) {
            isFirst = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(NOTIF_CALL_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
            else
                startForeground(NOTIF_CALL_ID, notification);

        }

        mNotificationService.updateNotification(notification, NOTIF_CALL_ID);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}