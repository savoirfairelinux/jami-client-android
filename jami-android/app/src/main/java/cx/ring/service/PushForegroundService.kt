package cx.ring.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.services.NotificationServiceImpl.Companion.NOTIF_CHANNEL_PUSH_SYNC

class PushForegroundService : Service() {
    private val recoveryWindow = PushRecoveryWindow()
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private val stopRunnable = Runnable {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_PUSH_SYNC)
            .setContentTitle(getString(R.string.notif_reconnect_title))
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setVibrate(null)
            .build()

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

        handler.removeCallbacks(stopRunnable)
        val remaining = recoveryWindow.extend(SystemClock.elapsedRealtime())
        if (remaining == 0L) {
            stopRunnable.run()
            return START_NOT_STICKY
        }
        try {
            val lock = wakeLock ?: (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push-recovery")
                .apply { setReferenceCounted(false) }
                .also { wakeLock = it }
            lock.acquire(remaining)
        } catch (e: Exception) {
            Log.w(TAG, "Can't acquire recovery wake lock", e)
        }
        handler.postDelayed(stopRunnable, remaining)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.w(TAG, "Can't release recovery wake lock", e)
        }
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopRunnable.run()
    }

    companion object {
        private const val TAG = "PushForegroundService"
        private const val NOTIFICATION_ID = 2001
    }
}
