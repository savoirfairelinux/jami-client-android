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
import java.util.Locale

class JamiFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Some devices block during call negotiation without a wake lock. The 10s timeout
        // is a safety net only: the lock is released as soon as the push is processed.
        var wakeLock: PowerManager.WakeLock? = null
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push").apply {
                setReferenceCounted(false)
                acquire((10 * 1000).toLong())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Can't acquire wake lock", e)
        }

        // Expired-value notification ("exp"): the value already left the DHT. Classified
        // first, since a stale call value keeps its audioCall/videoCall pt on expiration.
        val isExpiration = remoteMessage.data.containsKey("exp")
        val wakeup = if (isExpiration) PushWakeup(false, false) else classifyWakeup(remoteMessage)
        val isCallWakeup = wakeup.isCall
        val isMessageWakeup = wakeup.isMessage
        // Use originalPriority (server intent) not the delivered priority, which FCM/OEM
        // layers can downgrade for a genuine call push and leave it without FGS protection.
        val isCallNotification = isCallWakeup
                && remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH

        // Snapshot visibility before the FGS below promotes the process to IMPORTANCE_FOREGROUND,
        // which would otherwise make a later check skip the background restore path.
        val appInForeground = isAppInForeground()

        if (isCallNotification) {
            try {
                if (!appInForeground) {
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

        // Safe cast: a null or non-Firebase instance must not crash the FCM service.
        val app = JamiApplication.instance as? JamiApplicationFirebase

        if (!appInForeground && app != null) {
            // Single serialized entry point; push-availability, grace window, restore,
            // reconnect and deactivation re-arm are all evaluated on the main thread.
            app.onBackgroundPushReceived(
                isCallPush = isCallWakeup,
                isMessagePush = isMessageWakeup,
                isExpiration = isExpiration
            )
        }

        serviceScope.launch {
            try {
                app?.onMessageReceived(remoteMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error in processing message", e)
            } finally {
                // Re-arm deactivation so accounts restored above are not left active.
                if (!appInForeground) {
                    Log.d(TAG, "scheduling deactivation")
                    app?.scheduleBackgroundDeactivation()
                }
                // Non-call push is fully handled here: release now instead of burning the
                // 10s safety timeout. Call pushes keep the lock until timeout.
                if (!isCallWakeup) {
                    try {
                        wakeLock?.let { if (it.isHeld) it.release() }
                    } catch (e: Exception) {
                        Log.w(TAG, "Can't release wake lock", e)
                    }
                }
            }
        }
    }

    /**
     * Classifies a DHT proxy wakeup from the "pt" field (the connection request type the
     * proxy copies in): audioCall/videoCall for calls, application/im-gitmessage-id or
     * application/invite for swarm messages and invitations. FCM priority is never used,
     * as regular DHT values are also high priority. Only value ids never seen by this
     * process count, dropping the catch-up re-deliveries the proxy emits on every fresh
     * listener; the dedupe caches are in-memory, so after a restart a stale id classifies once.
     */
    private fun classifyWakeup(remoteMessage: RemoteMessage): PushWakeup {
        val pushTypes = remoteMessage.data["pt"] ?: return PushWakeup(false, false)
        val ids = remoteMessage.data["ids"]?.split(',') ?: emptyList()
        // Scope the dedupe key by destination client id and DHT key: value ids are random
        // 64-bit values, unique in practice but not across keys. Missing fields degrade to
        // coarser scoping, never to dropping a wakeup.
        val scope = "${remoteMessage.data["to"] ?: ""}:${remoteMessage.data["key"] ?: ""}"
        var newCall = false
        var newMessage = false
        pushTypes.splitToSequence(',').forEachIndexed { i, rawType ->
            val type = rawType.trim().lowercase(Locale.ROOT)
            val isCall = type == "audiocall" || type == "videocall"
            // Exact type or explicit separator only, so unrelated future types cannot match.
            val isMessage = type == "application/im-gitmessage-id"
                    || type.startsWith("application/im-gitmessage-id/")
                    || type == "application/invite"
                    || type.startsWith("application/invite+")
            if (!isCall && !isMessage) return@forEachIndexed
            val id = ids.getOrNull(i)?.trim()
            val isNew = if (id.isNullOrEmpty()) {
                true // No id to deduplicate on: fail open, a missed call is worse than a redundant restore.
            } else {
                // Per-kind caches so message volume cannot evict call dedupe state.
                val seen = if (isCall) seenCallIds else seenMessageIds
                synchronized(seen) { seen.put("$scope:$id", Unit) == null }
            }
            if (isNew) {
                if (isCall) newCall = true
                if (isMessage) newMessage = true
            }
        }
        return PushWakeup(newCall, newMessage)
    }

    private data class PushWakeup(val isCall: Boolean, val isMessage: Boolean)

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
        val app = JamiApplication.instance as? JamiApplicationFirebase
        app?.pushToken = Pair(refreshedToken, "")
    }

    companion object {
        private const val TAG = "JamiFirebaseMessaging"

        // Value ids already handled by this process, to drop proxy catch-up re-deliveries.
        // Two bounded LRU caches so frequent message ids cannot evict call dedupe state.
        private const val SEEN_CALL_IDS_MAX = 256
        private const val SEEN_MESSAGE_IDS_MAX = 2048
        private val seenCallIds = object : LinkedHashMap<String, Unit>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>) =
                size > SEEN_CALL_IDS_MAX
        }
        private val seenMessageIds = object : LinkedHashMap<String, Unit>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>) =
                size > SEEN_MESSAGE_IDS_MAX
        }
    }
}
