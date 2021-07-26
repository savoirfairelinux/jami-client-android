/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import net.jami.services.NotificationService;

import java.util.HashSet;
import java.util.Set;

@AndroidEntryPoint
public class DataTransferService extends Service {
    private final String TAG = DataTransferService.class.getSimpleName();
    public static final String ACTION_START = "startTransfer";
    public static final String ACTION_STOP = "stopTransfer";
    private static final int NOTIF_FILE_SERVICE_ID = 1002;

    @Inject
    NotificationService mNotificationService;

    private NotificationManagerCompat notificationManager;
    private boolean started = false;

    private int serviceNotificationId = 0;
    private final Set<Integer> serviceNotifications = new HashSet<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int notificationId = intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1);
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            serviceNotifications.add(notificationId);
            Notification notification = (Notification) mNotificationService.getDataTransferNotification(notificationId);
            // Log.w(TAG, "Updating notification " + intent);
            if (!started) {
                Log.w(TAG, "starting transfer service " + intent);
                serviceNotificationId = notificationId;
                started = true;
            }
            if (notificationId == serviceNotificationId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    startForeground(NOTIF_FILE_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                else
                    startForeground(NOTIF_FILE_SERVICE_ID, notification);
            } else {
                notificationManager.notify(notificationId, notification);
            }
        }
        else if (ACTION_STOP.equals(action)) {
            serviceNotifications.remove(notificationId);
            if (notificationId == serviceNotificationId) {
                // The service notification is removed. Migrate service to other notification or stop it
                serviceNotificationId = serviceNotifications.isEmpty() ? 0 : serviceNotifications.iterator().next();
                if (serviceNotificationId == 0) {
                    Log.w(TAG, "stopping transfer service " + intent);
                    stopForeground(true);
                    stopSelf();
                    started = false;
                } else {
                    // migrate notification to service
                    notificationManager.cancel(serviceNotificationId);
                    Notification notification = (Notification) mNotificationService.getDataTransferNotification(serviceNotificationId);
                    notificationManager.notify(NOTIF_FILE_SERVICE_ID, notification);
                }
            } else {
                notificationManager.cancel(notificationId);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "OnCreate(), DataTransferService has been initialized");
        notificationManager = NotificationManagerCompat.from(this);
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy(), DataTransferService has been destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}