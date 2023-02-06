/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package cx.ring.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.services.NotificationServiceImpl
import cx.ring.utils.ContentUriHandler
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.NotificationService
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {
    private var serviceUsers = 0
    private val mRandom = Random()
    private var notification: Notification? = null

    @Inject
    lateinit var mNotificationService: NotificationService

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (ACTION_START == action) {
            if (notification == null) {
                val deleteIntent = Intent(ACTION_STOP)
                    .setClass(applicationContext, SyncService::class.java)
                val contentIntent = Intent(Intent.ACTION_VIEW)
                    .setClass(applicationContext, HomeActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                notification = NotificationCompat.Builder(this, NotificationServiceImpl.NOTIF_CHANNEL_SYNC)
                    .setContentTitle(getString(R.string.notif_sync_title))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(false)
                    .setVibrate(null)
                    .setSmallIcon(R.drawable.ic_ring_logo_white)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setOnlyAlertOnce(true)
                    .setDeleteIntent(PendingIntent.getService(applicationContext, mRandom.nextInt(), deleteIntent, ContentUriHandler.immutable()))
                    .setContentIntent(PendingIntent.getActivity(applicationContext, mRandom.nextInt(), contentIntent, ContentUriHandler.immutable()))
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(NOTIF_SYNC_SERVICE_ID, notification!!, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else
                startForeground(NOTIF_SYNC_SERVICE_ID, notification)
            if (serviceUsers == 0) {
                JamiApplication.instance?.startDaemon(this)
            }
            serviceUsers++
            val timeout = intent.getLongExtra(EXTRA_TIMEOUT, -1)
            if (timeout > 0) {
                Handler().postDelayed({
                    try {
                        startService(Intent(ACTION_STOP).setClass(applicationContext, SyncService::class.java))
                    } catch (ignored: IllegalStateException) {
                    }
                }, timeout)
            }
        } else if (ACTION_STOP == action) {
            serviceUsers--
            if (serviceUsers == 0) {
                stopForeground(true)
                stopSelf()
                notification = null
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val NOTIF_SYNC_SERVICE_ID = 1004
        const val ACTION_START = "startService"
        const val ACTION_STOP = "stopService"
        const val EXTRA_TIMEOUT = "timeout"
    }
}