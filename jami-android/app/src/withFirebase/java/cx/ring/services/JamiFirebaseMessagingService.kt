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
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationFirebase
import kotlinx.coroutines.*

class JamiFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val app = JamiApplication.instance as JamiApplicationFirebase?
        val isHigh = remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH
        // onPushReceived() handles WakeLock acquisition, foreground service
        // start (only when isHighPriority and the app is in background),
        // synchronous account reactivation and event-driven release.
        app?.onPushReceived(isHigh)

        // NOTE: do NOT call hardwareService.connectivityChanged() here.
        // The network has not actually changed when a push arrives, but
        // libjami::connectivityChanged() unconditionally tears down all
        // existing sockets and forces every account to reconnect — which
        // breaks in-flight TLS/ICE sessions and causes a reconnect storm
        // on busy accounts (one storm per push). Daemon wake-up is now
        // handled by onPushReceived() via setAccountsActive(true,
        // awaitCompletion=true), which is the proper mechanism.

        serviceScope.launch {
            try {
                if (app == null) return@launch
                // Ensure daemon is fully started before dispatching push.
                if (!app.ensureDaemonStarted()) {
                    Log.w(TAG, "Daemon not started, dropping push")
                    return@launch
                }
                app.onMessageReceived(remoteMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error in processing message", e)
            } finally {
                // Do NOT release WakeLock or stop foreground service here:
                // the native dispatch only schedules work inside the daemon.
                // onPushProcessed() decrements the in-flight counter; actual
                // tear-down is driven by daemon events or the 25s timeout.
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
