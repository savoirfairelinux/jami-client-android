/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.services.NotificationServiceImpl.Companion.NOTIF_CHANNEL_PEER_TUNNEL
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.ExposedServicesService
import net.jami.services.PeerServicesService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PeerTunnelForegroundService : Service() {

    @Inject
    lateinit var peerServicesService: PeerServicesService
    @Inject
    lateinit var exposedServicesService: ExposedServicesService
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ActiveServiceMonitor.onServiceStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()

        if (intent?.action == ACTION_STOP_ALL) {
            // Close every connected tunnel off the main thread, then disable the hosted
            // servers on the main thread (where runningServers is otherwise mutated).
            worker.execute {
                peerServicesService.closeAllTunnels()
                mainHandler.post {
                    exposedServicesService.disableAllHostedServices()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelfResult(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java).apply {
                action = HomeActivity.ACTION_SHOW_SHARED_SERVICES
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopAllIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PeerTunnelForegroundService::class.java).apply {
                action = ACTION_STOP_ALL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_PEER_TUNNEL)
            .setContentTitle(getString(R.string.notif_peer_tunnel_title))
            .setContentText(getString(R.string.notif_peer_tunnel_text))
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setContentIntent(tapIntent)
            .addAction(0, getString(R.string.notif_peer_tunnel_stop_all), stopAllIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setVibrate(null)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        ActiveServiceMonitor.onServiceStopped()
        worker.shutdown()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2002
        const val ACTION_STOP_ALL = "cx.ring.action.STOP_ALL_SHARED_SERVICES"
    }
}
