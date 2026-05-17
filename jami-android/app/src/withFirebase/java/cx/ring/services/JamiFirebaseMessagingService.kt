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

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationFirebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class JamiFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val app = JamiApplication.instance as JamiApplicationFirebase?

        // Centralised push lifecycle (WakeLock + optional foreground service).
        // Only high-priority pushes start the foreground service to avoid
        // draining the battery on every push.
        val isHigh = remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH
        app?.onPushReceived(isHighPriority = isHigh)

        if (isHigh && app?.isInForeground == false) {
            app.hardwareService?.connectivityChanged(true)
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
                // Do NOT release WakeLock or stop foreground service here:
                // the native dispatch only schedules work inside the daemon.
                app?.onPushProcessed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
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
