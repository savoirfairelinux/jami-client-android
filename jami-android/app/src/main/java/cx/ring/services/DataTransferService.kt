/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import android.app.Notification
import android.app.Service
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.jami.services.NotificationService
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.util.HashSet

@AndroidEntryPoint
class DataTransferService : Service() {

    @Inject
    lateinit var mNotificationService: NotificationService

    private lateinit var notificationManager: NotificationManagerCompat
    private var started = false
    private var serviceNotificationId = 0
    private val serviceNotifications: MutableSet<Int> = HashSet()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val notificationId = intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1)
        val action = intent.action
        if (ACTION_START == action) {
            serviceNotifications.add(notificationId)
            val notification = mNotificationService.getDataTransferNotification(notificationId) as Notification
            // Log.w(TAG, "Updating notification " + intent);
            if (!started) {
                Log.w(TAG, "starting transfer service $intent")
                serviceNotificationId = notificationId
                started = true
            }
            if (notificationId == serviceNotificationId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    startForeground(NOTIF_FILE_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                else
                    startForeground(NOTIF_FILE_SERVICE_ID, notification)
            } else {
                notificationManager.notify(notificationId, notification)
            }
        } else if (ACTION_STOP == action) {
            serviceNotifications.remove(notificationId)
            mNotificationService.cancelFileNotification(notificationId)
            if (notificationId == serviceNotificationId) {
                while (true) {
                    // The service notification is removed. Migrate service to other notification or stop it
                    if (serviceNotifications.isEmpty()) {
                        serviceNotificationId = 0
                        Log.w(TAG, "stopping transfer service $intent")
                        stopForeground(true)
                        stopSelf()
                        started = false
                    } else {
                        serviceNotificationId = serviceNotifications.iterator().next()
                        // migrate notification to service
                        notificationManager.cancel(serviceNotificationId)
                        val notification = mNotificationService.getDataTransferNotification(serviceNotificationId) as Notification?
                        if (notification != null) {
                            notificationManager.notify(NOTIF_FILE_SERVICE_ID, notification)
                        } else {
                            serviceNotifications.remove(serviceNotificationId)
                            continue
                        }
                    }
                    break
                }
            } else {
                notificationManager.cancel(notificationId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        Log.d(TAG, "OnCreate(), DataTransferService has been initialized")
        notificationManager = NotificationManagerCompat.from(this)
        super.onCreate()
    }

    override fun onDestroy() {
        Log.d(TAG, "OnDestroy(), DataTransferService has been destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val ACTION_START = "startTransfer"
        const val ACTION_STOP = "stopTransfer"
        private const val NOTIF_FILE_SERVICE_ID = 1002
        private val TAG = DataTransferService::class.simpleName!!
    }
}
