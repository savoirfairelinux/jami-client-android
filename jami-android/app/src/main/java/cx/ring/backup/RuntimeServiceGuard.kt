package cx.ring.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.application.JamiApplication

internal object RuntimeServiceGuard {
    fun canInitialize(context: Context): Boolean {
        val application = context.applicationContext as? JamiApplication ?: return false
        if (!application.prepareRuntime()) return false
        application.bootstrapDaemon()
        return true
    }

    fun rejectStart(service: Service, startId: Int): Int {
        val notificationManager = service.getSystemService(NotificationManager::class.java)
        val channelId = "backup_restore"
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, service.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
        )
        val notification = NotificationCompat.Builder(service, channelId)
            .setSmallIcon(R.drawable.ic_ring_logo_white)
            .setContentTitle(service.getString(R.string.app_name))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        try {
            val notificationId = service.javaClass.name.hashCode() and Int.MAX_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(notificationId, notification, 0)
            } else {
                service.startForeground(notificationId, notification)
            }
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } catch (exception: RuntimeException) {
            Log.w("RuntimeServiceGuard", "Unable to finish deferred foreground start", exception)
        }
        service.stopSelf(startId)
        return Service.START_NOT_STICKY
    }
}