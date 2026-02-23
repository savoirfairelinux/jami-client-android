package cx.ring.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.services.NotificationServiceImpl.Companion.NOTIF_CHANNEL_PUSH_SYNC
import net.jami.utils.Log

class PushForegroundService : Service() {
    private val timeoutMs = 5000L
    private val handler = Handler(Looper.getMainLooper())

    private val stopRunnable = Runnable {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_PUSH_SYNC)
            .setContentTitle(getString(R.string.notif_reconnect_title))
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setVibrate(null)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, timeoutMs)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PushForegroundService"
        private const val NOTIFICATION_ID = 2001
    }
}

