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
        try {
            // Even if wakeLock is deprecated, without this part, some devices are blocking
            // during the call negotiation. So, re-add this code to avoid to block here.
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push")
            wl.setReferenceCounted(false)
            wl.acquire((10 * 1000).toLong())
        } catch (e: Exception) {
            Log.w(TAG, "Can't acquire wake lock", e)
        }

        val pushType = remoteMessage.data["pt"] ?: ""
        val isCallNotification = (pushType.contains("audioCall")
                || pushType.contains("videoCall"))
                && remoteMessage.priority == RemoteMessage.PRIORITY_HIGH

        if (isCallNotification) {
            try {
                val isForeground = isAppInForeground()
                if (!isForeground) {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            val intent = Intent(this, PushForegroundService::class.java)
                            startForegroundService(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start foreground service on main thread", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start foreground service for push notification", e)
            }
        }

        val appInForeground = isAppInForeground()
        val app = JamiApplication.instance as JamiApplicationFirebase?

        if (!appInForeground) {
            // Open the deactivation grace window before reactivating accounts, so the
            // daemon has time to reconnect through the proxy and receive the incoming
            // call (which only becomes visible to hasActiveCalls() once negotiated).
            val isCallPush = isCallNotification || isDhtProxyCallWakeup(remoteMessage)
            app?.notePushReceived(isCallPush)
            // Reactivate accounts for any push type: accounts may have been deactivated
            // while the app was in background. Receiving an FCM push implies the network
            // is available, so don't gate this on possibly-stale connectivity state.
            app?.mAccountService?.setAccountsActiveForBackground(true)
            // connectivityChanged triggers a full DHT/SIP reconnect — only needed for calls.
            if (remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH) {
                app?.hardwareService?.connectivityChanged(true)
            }
        }

        serviceScope.launch {
            try {
                app?.onMessageReceived(remoteMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error in processing message", e)
            } finally {
                // If the app is still in background after handling the push, schedule
                // deactivation so accounts reactivated above are not left active indefinitely.
                if (!appInForeground) {
                    app?.scheduleBackgroundDeactivation()
                }
            }
        }
    }

    /**
     * Classifies a DHT proxy call wakeup. The proxy always copies the connection request
     * type into the "pt" field ("audioCall"/"videoCall" for calls, "sip"/"git"… for
     * messaging and sync channels), so classify from it when present: FCM priority alone
     * is unreliable, as message-channel requests are also sent at high priority and FCM
     * may downgrade the delivered priority of real call pushes. Only fall back to the
     * requested priority for minimal payloads that carry no "pt" field.
     */
    private fun isDhtProxyCallWakeup(remoteMessage: RemoteMessage): Boolean {
        val pushType = remoteMessage.data["pt"]
        return if (pushType.isNullOrEmpty())
            remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH
        else pushType.contains("audioCall") || pushType.contains("videoCall")
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