/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Random;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.utils.Log;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();
    public static final int NOTIF_SYNC_SERVICE_ID = 1004;
    public static final String ACTION_START = "startService";
    public static final String ACTION_STOP = "stopService";

    private boolean isFirst = true;
    private final Random mRandom = new Random();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand " + intent);

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            if (isFirst) {
                isFirst = false;
                final Intent deleteIntent = new Intent(ACTION_STOP)
                        .setClass(getApplicationContext(), SyncService.class);
                final Intent contentIntent = new Intent(Intent.ACTION_VIEW)
                        .setClass(getApplicationContext(), HomeActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                Notification notif = new NotificationCompat.Builder(this, NotificationServiceImpl.NOTIF_CHANNEL_SYNC)
                        .setContentTitle(getString(R.string.notif_sync_title))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setVibrate(null)
                        .setSmallIcon(R.drawable.ic_ring_logo_white)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setOnlyAlertOnce(true)
                        .setDeleteIntent(PendingIntent.getService(getApplicationContext(), mRandom.nextInt(), deleteIntent, 0))
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), mRandom.nextInt(), contentIntent, 0))
                        .build();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    startForeground(NOTIF_SYNC_SERVICE_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                else
                    startForeground(NOTIF_SYNC_SERVICE_ID, notif);
            }
        }
        else if (ACTION_STOP.equals(action)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
            isFirst = true;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}