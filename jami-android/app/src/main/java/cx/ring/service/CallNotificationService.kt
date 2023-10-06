/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import cx.ring.services.NotificationServiceImpl
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.NotificationService
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationService : Service() {

    @Inject
    lateinit var mNotificationService: NotificationService

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (ACTION_START == intent.action) {
            val notification = mNotificationService.showCallNotification(intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1)) as Notification?
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    startForeground(NotificationServiceImpl.NOTIF_CALL_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    startForeground(NotificationServiceImpl.NOTIF_CALL_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                else
                    startForeground(NotificationServiceImpl.NOTIF_CALL_ID, notification)
            }
        } else if (ACTION_STOP == intent.action) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            mNotificationService.cancelCallNotification()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
    }
}
