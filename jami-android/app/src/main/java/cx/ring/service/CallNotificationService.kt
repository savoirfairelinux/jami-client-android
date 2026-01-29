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
import io.reactivex.rxjava3.disposables.CompositeDisposable
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

    private val mDisposable = CompositeDisposable()

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (ACTION_START == action) {
            val startScreenshare = intent.getBooleanExtra(NotificationService.KEY_SCREENSHARE, false)
            mDisposable.clear()
            mDisposable.add(mNotificationService.callNotificationStream()
                .subscribe({ notificationObj ->
                    val notification = notificationObj as? Notification
                    if (notification != null) {
                        try {
                            updateForeground(notification, startScreenshare)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start foreground service", e)
                        }
                    }
                }, { e ->
                    Log.e(TAG, "Error in notification stream", e)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }, {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }))

            if (startScreenshare)
                mNotificationService.startPendingScreenshare("")
        } else if (ACTION_STOP == action) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            mNotificationService.cancelCallNotification()
        }
        return START_NOT_STICKY
    }

    private fun updateForeground(notification: Notification, startScreenshare: Boolean) {
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            startForeground(
                NotificationServiceImpl.NOTIF_CALL_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                        or (if (startScreenshare) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0)
            )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(
                NotificationServiceImpl.NOTIF_CALL_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        else
            startForeground(NotificationServiceImpl.NOTIF_CALL_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val TAG = "CallNotificationService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
    }
}
