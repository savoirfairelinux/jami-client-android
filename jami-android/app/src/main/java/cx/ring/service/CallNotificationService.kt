/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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
            val confId = intent.getStringExtra(NotificationService.KEY_CALL_ID)
            val notification = mNotificationService.showCallNotification(intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1)) as Notification?
            val startScreenshare = intent.getBooleanExtra(NotificationService.KEY_SCREENSHARE, false)
            if (notification != null) {
                // Since API 34, screen sharing (media projection)
                // should not be specified before user grants permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pm = packageManager
                    val name = packageName
                    val cameraServiceType = if (pm.checkPermission(Manifest.permission.FOREGROUND_SERVICE_CAMERA, name) ==
                            PackageManager.PERMISSION_GRANTED) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0
                    val microphoneServiceType = if (pm.checkPermission(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE, name) ==
                            PackageManager.PERMISSION_GRANTED) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
                    val callServiceType = if (pm.checkPermission(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL, name) ==
                            PackageManager.PERMISSION_GRANTED) ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else 0
                    val screenShareType = if (startScreenshare && pm.checkPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, name) ==
                        PackageManager.PERMISSION_GRANTED) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
                    startForeground(
                        NotificationServiceImpl.NOTIF_CALL_ID,
                        notification,
                        callServiceType
                                or microphoneServiceType
                                or cameraServiceType
                                or screenShareType
                    )
                    // Since API 30, microphone and camera should be specified for app to use them.
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    startForeground(
                        NotificationServiceImpl.NOTIF_CALL_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                                or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                                or (if (startScreenshare) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0)
                    )
                // Since API 29, should specify foreground service type.
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    startForeground(
                        NotificationServiceImpl.NOTIF_CALL_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                else // Before API 29, just start foreground service.
                    startForeground(NotificationServiceImpl.NOTIF_CALL_ID, notification)
                if (startScreenshare && confId != null)
                    mNotificationService.startPendingScreenshare(confId)
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
