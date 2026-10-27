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

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationHuawei
import cx.ring.service.PushForegroundService
import java.util.Locale

class JamiHmsMessagingService : HmsMessageService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val app = JamiApplication.instance as? JamiApplicationHuawei ?: return
        if (!app.prepareRuntime()) return
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

        val data = remoteMessage.dataOfMap ?: emptyMap()
        // HMS has no equivalent of FCM's original priority: the urgency the proxy asked for
        // is the only signal. Presence announcements, CRLs and expirations go out normal;
        // an unknown value fails open, a missed call being worse than a redundant wakeup.
        val isRenewal = data.containsKey("timeout")
        val isActionable = isRenewal || remoteMessage.urgency != RemoteMessage.PRIORITY_NORMAL
        val wakeup = if (isActionable) classifyWakeup(data) else PushWakeup(false, false)
        val needsFgs = (wakeup.isCall || wakeup.isMessage) && remoteMessage.urgency == RemoteMessage.PRIORITY_HIGH
        Log.d(TAG, "push actionable=$isActionable renewal=$isRenewal call=${wakeup.isCall} message=${wakeup.isMessage} urgency=${remoteMessage.urgency}")

        // Calls and messages trigger an async daemon fetch (proxy reconnect + DHT pull).
        if (needsFgs) {
            Handler(Looper.getMainLooper()).post {
                try {
                    startForegroundService(Intent(this, PushForegroundService::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service", e)
                }
            }
        }

        try {
            app.onMessage(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error in processing message", e)
        }

        // Presence/expiration push: nothing to do, so release early instead of burning the
        // 10s timeout. Actionable pushes keep the lock for the async fetch they start.
        if (!isActionable) {
            try {
                wakeLock?.let { if (it.isHeld) it.release() }
            } catch (e: Exception) {
                Log.w(TAG, "Can't release wake lock", e)
            }
        }
    }

    /**
     * Classifies an actionable proxy wakeup from the "pt" field (the connection request
     * type the proxy copies in): audioCall/videoCall for calls, application/im-gitmessage-id
     * or application/invite for swarm messages and invitations, and sync for multi-device
     * account sync. Only value ids never seen by this process count, dropping the catch-up
     * re-deliveries the proxy emits on every fresh listener; the dedupe caches are
     * in-memory, so after a restart a stale id classifies once.
     */
    private fun classifyWakeup(data: Map<String, String>): PushWakeup {
        val pushTypes = data["pt"] ?: return PushWakeup(false, false)
        val ids = data["ids"]?.split(',') ?: emptyList()
        // Scope the dedupe key by destination client id and DHT key: value ids are random
        // 64-bit values, unique in practice but not across keys. Missing fields degrade to
        // coarser scoping, never to dropping a wakeup.
        val scope = "${data["to"] ?: ""}:${data["key"] ?: ""}"
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
                    || type == "sync"
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

    override fun onNewToken(token: String) {
        Log.w(TAG, "onNewToken")
        val app = JamiApplication.instance as? JamiApplicationHuawei
        app?.pushToken = Pair(token, "")
    }

    override fun onTokenError(e: Exception) {
        Log.e(TAG, "onTokenError", e)
    }

    override fun onDeletedMessages() {
        Log.w(TAG, "onDeletedMessages")
    }

    companion object {
        private const val TAG = "JamiHmsMessaging"

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
