/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import cx.ring.services.NotificationServiceImpl
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.NotificationService
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationService : Service() {

    private fun PackageManager.hasPermission(permission: String, pn: String = packageName): Boolean =
        checkPermission(permission, pn) == PackageManager.PERMISSION_GRANTED

    private fun PackageManager.hasPermissions(vararg permissions: String): Boolean {
        val pn = packageName
        return permissions.all { checkPermission(it, pn) == PackageManager.PERMISSION_GRANTED }
    }

    @Inject
    lateinit var mNotificationService: NotificationService

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (ACTION_START == intent.action) {
            val confId = intent.getStringExtra(NotificationService.KEY_CALL_ID)
            val notification = mNotificationService.showCallNotification(intent.getIntExtra(NotificationService.KEY_NOTIFICATION_ID, -1)) as Notification?
            val startScreenshare = intent.getBooleanExtra(NotificationService.KEY_SCREENSHARE, false)
            if (notification != null) {
                try {
                    // Since API 34, foreground services
                    // should not be specified before user grants permission.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pm = packageManager
                        val cameraServiceType = if (pm.hasPermissions(Manifest.permission.FOREGROUND_SERVICE_CAMERA, Manifest.permission.CAMERA))
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0
                        val microphoneServiceType = if (pm.hasPermissions(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE, Manifest.permission.RECORD_AUDIO))
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
                        val callServiceType = if (pm.hasPermission(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL))
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else 0
                        val screenShareType = if (startScreenshare && pm.hasPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION))
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service", e)
                }
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
        private const val TAG = "CallNotificationService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
    }
}
