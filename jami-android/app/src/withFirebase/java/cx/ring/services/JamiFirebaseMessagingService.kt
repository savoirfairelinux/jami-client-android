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
import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationFirebase

class JamiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            // Even if wakeLock is deprecated, without this part, some devices are blocking
            // during the call negotiation. So, re-add this code to avoid to block here.
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push")
            wl.setReferenceCounted(false)
            wl.acquire((10 * 1000).toLong())
        } catch (e: Exception) {
            Log.w("JamiFirebaseMessaging", "Can't acquire wake lock", e)
        }
        val app = JamiApplication.instance as JamiApplicationFirebase?
        val isForeground = isAppInForeground()

        if (shouldPostReconnectNotification(remoteMessage, isForeground)) {
            val intent = Intent(this, cx.ring.service.PushForegroundService::class.java)
            startForegroundService(intent)
        }

        app?.onMessageReceived(remoteMessage)
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val procs = am.runningAppProcesses ?: return false
        val pkg = packageName

        return procs.any { p ->
            p.processName == pkg &&
                    p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun shouldPostReconnectNotification(
        remoteMessage: RemoteMessage,
        isForeground: Boolean
    ): Boolean {
        if (isForeground) return false

        val isHighPriority =
            remoteMessage.priority == RemoteMessage.PRIORITY_HIGH &&
                    remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH

        return isHighPriority
    }

    override fun onNewToken(refreshedToken: String) {
        Log.w("JamiFirebaseMessaging", "onNewToken $refreshedToken")
        val app = JamiApplication.instance as JamiApplicationFirebase?
        app?.pushToken = Pair(refreshedToken, "")
    }
}