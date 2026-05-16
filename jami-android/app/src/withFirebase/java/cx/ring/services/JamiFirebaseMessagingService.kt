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
package cx.ring.services

import android.app.ActivityManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationFirebase
import cx.ring.service.PushForegroundService
import kotlinx.coroutines.*

class JamiFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val app = JamiApplication.instance as JamiApplicationFirebase?
        app?.onPushReceived()

        var wl: PowerManager.WakeLock? = null
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jami:push")
            wl.setReferenceCounted(false)
            wl.acquire((30 * 1000).toLong())
        } catch (e: Exception) {
            Log.w(TAG, "Can't acquire wake lock", e)
        }

        // Start foreground service for all pushes to prevent process kill
        if (!isAppInForeground()) {
            Handler(Looper.getMainLooper()).post {
                try {
                    val intent = Intent(this, PushForegroundService::class.java)
                    startForegroundService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service", e)
                }
            }
        }

        if (remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH && !isAppInForeground()) {
            app?.hardwareService?.connectivityChanged(true)
        }

        serviceScope.launch {
            try {
                if (app != null) {
                    // Ensure daemon is fully started before dispatching push
                    app.ensureDaemonStarted()
                    app.onMessageReceived(remoteMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processing message", e)
            } finally {
                // Release WakeLock early and signal push processing complete
                try { wl?.release() } catch (_: Exception) {}
                app?.onPushProcessed()
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val procs = am.runningAppProcesses ?: return false
        val pid = android.os.Process.myPid()
        return procs.any { p ->
            p.pid == pid
                    && (p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    || p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE)
        }
    }

    override fun onNewToken(refreshedToken: String) {
        Log.w(TAG, "onNewToken $refreshedToken")
        val app = JamiApplication.instance as JamiApplicationFirebase?
        app?.pushToken = Pair(refreshedToken, "")
    }

    companion object {
        private const val TAG = "JamiFirebaseMessaging"
    }
}