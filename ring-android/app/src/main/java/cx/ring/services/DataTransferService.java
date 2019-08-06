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
package cx.ring.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.utils.Log;

public class DataTransferService extends Service {


    @Inject
    NotificationService mNotificationService;

    private final String TAG = DataTransferService.class.getSimpleName();
    private boolean isFirst = true;
    private static final int NOTIF_FILE_SERVICE_ID = 1002;
    private int serviceNotificationId;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        int notificationId = intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1);
        Notification notification = (Notification) mNotificationService.getDataTransferNotification(intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1));

        if (notification == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (isFirst) {
            isFirst = false;
            mNotificationService.cancelFileNotification(notificationId, true);
            serviceNotificationId = notificationId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(NOTIF_FILE_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
            else
                startForeground(NOTIF_FILE_SERVICE_ID, notification);
        }

        if (mNotificationService.getDataTransferNotification(serviceNotificationId) == null) {
            mNotificationService.cancelFileNotification(notificationId, true);
            serviceNotificationId = notificationId;
        }

        if(notificationId == serviceNotificationId)
            mNotificationService.updateNotification(notification, NOTIF_FILE_SERVICE_ID);
        else
            mNotificationService.updateNotification(notification, notificationId);


        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "OnCreate(), Service has been initialized");
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy(), Service has been destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}