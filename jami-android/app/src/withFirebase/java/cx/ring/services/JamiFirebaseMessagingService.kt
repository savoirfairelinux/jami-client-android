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
        // Even if wakeLock is deprecated, without this part, some devices are blocking
        // during the call negotiation. So, re-add this code to avoid to block here.
        // The 10s timeout is only a safety net: the lock is released as soon as the
        // message is fully processed (typically well under a second). Holding it for
        // the full 10s on every push — measured at ~20 pushes/min with stale proxy
        // sessions — kept the device awake over an hour per day for no benefit.
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

        // Expired-value notification from the DHT proxy ("exp" field): the value has
        // left the DHT. An incoming call is a value *addition*; its later expiration
        // (proxy server sets "exp" only on expired events) arrives with the original
        // pt copied — including audioCall/videoCall for stale call values. Classify
        // expirations first, unconditionally: treating an expired call value as an
        // incoming call restores accounts, reconnects and starts a foreground service
        // for a call whose caller gave up minutes ago (observed: a videoCall value
        // pushed at 12:39 re-triggered the full call path on its expiration at 12:49).
        val isExpiration = remoteMessage.data.containsKey("exp")

        // Same normalized classifier as the restore path below: a case-sensitive
        // substring check here would let a producer-side casing change restore
        // accounts via isDhtProxyCallWakeup but skip the foreground-service/wakelock
        // protection intended for incoming calls.
        // Only *new* call value ids count: when the proxy server creates a fresh
        // push listener (every re-subscription after the server dropped or never had
        // one), its new internal DHT listen immediately re-delivers all values still
        // stored on the key — including call requests from hours ago, with their
        // original pt. Re-running the call path for those re-deliveries made every
        // resubscribe dump look like an incoming call, feeding a restore/deactivate
        // churn that ends with FCM dropping genuine call pushes (oversized catch-up
        // messages and per-device quota, both observed server-side).
        val wakeup = if (isExpiration) PushWakeup(false, false) else classifyWakeup(remoteMessage)
        val isCallWakeup = wakeup.isCall
        val isMessageWakeup = wakeup.isMessage
        val isCallNotification = isCallWakeup
                && remoteMessage.priority == RemoteMessage.PRIORITY_HIGH

        // Snapshot UI visibility once, before the foreground service below can start:
        // a started FGS promotes the process to IMPORTANCE_FOREGROUND, so a later
        // check could report foreground while the UI is actually backgrounded and
        // skip the account-restore path, missing the incoming call.
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

        // Safe cast: a null or non-Firebase application instance must not crash the FCM
        // service — a push delivery would otherwise terminate the process before the
        // accounts are restored or the notification is processed.
        val app = JamiApplication.instance as? JamiApplicationFirebase

        if (!appInForeground && app != null) {
            // Positive, protocol-defined signal from the pt field, never priority: the
            // daemon labels calls audioCall/videoCall and human-awaited swarm traffic
            // application/im-gitmessage-id (new commits) or application/invite
            // (conversation invitations), and sends exactly these at high push priority
            // (background sync/git-fetch uses other types at normal priority).
            // Classifying message wakeups by priority instead would let the residual
            // high-priority empty-pt noise (presence/connection churn still delivered
            // high) restore accounts on every push, re-opening the restore/deactivate
            // churn the cooldown exists to break. Expirations and re-delivered ids are
            // excluded (see classifyWakeup), so only genuinely new calls/messages reach
            // the wakeup paths.
            // Single serialized entry point: the push-availability gate, grace window,
            // account restore, reconnect and deactivation re-arm are all evaluated on
            // the main thread (see onBackgroundPushReceived) — preference and token
            // state must not be read from this FCM thread, where they could race a
            // settings/token transition.
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
                // If the app is still in background after handling the push, schedule
                // deactivation so accounts reactivated above are not left active indefinitely.
                if (!appInForeground) {
                    app?.scheduleBackgroundDeactivation()
                }
                // Non-call pushes are fully handled here: release immediately rather than
                // letting the 10s safety timeout burn wakelock time on every push (measured
                // at ~20 pushes/min: that timeout alone kept the device awake >1h/day).
                // Call pushes keep the lock until timeout — the historical protection for
                // devices that block during call negotiation after the CPU drops.
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
     * Classifies a DHT proxy wakeup from a positive signal only: the proxy copies the
     * connection request type into the "pt" field — "audioCall"/"videoCall" for calls,
     * "application/im-gitmessage-id/<conv>" for new swarm commits, "application/invite"
     * for conversation invitations, "sip"/"git"… for background sync — as a
     * comma-separated list of exact values when the push covers several, positionally
     * aligned with the "ids" list of DHT value ids. FCM priority is never used to
     * classify: regular DHT values default to high priority too, so empty- or
     * other-typed high-priority pushes (presence/connection churn) would all be
     * misclassified as calls or messages and needlessly restore accounts, re-opening
     * the restore/deactivate churn the cooldown breaks.
     *
     * Only ids never seen by this process count: the proxy re-delivers all values still
     * stored on a key every time it creates a fresh internal listener (catch-up dump on
     * re-subscription), so a value keeps reappearing — with its original pt — for as
     * long as it stays in the DHT, long after the call ended or the commit was fetched.
     * The cache is in-memory and bounded: after a process restart a stale id can be
     * re-classified at most once.
     */
    private fun classifyWakeup(remoteMessage: RemoteMessage): PushWakeup {
        val pushTypes = remoteMessage.data["pt"] ?: return PushWakeup(false, false)
        val ids = remoteMessage.data["ids"]?.split(',') ?: emptyList()
        // Scope the dedupe key by destination: DHT value ids are random 64-bit values
        // chosen by each producer, unique in practice but not guaranteed unique across
        // keys. The proxy sets "to" to the subscribing client id (one per account) and
        // "key" to the listened DHT key; scoping by both confines any collision to the
        // same account and key, where a same-id re-delivery is by definition the same
        // value. Missing fields degrade to coarser scoping, never to dropping a wakeup.
        val scope = "${remoteMessage.data["to"] ?: ""}:${remoteMessage.data["key"] ?: ""}"
        var newCall = false
        var newMessage = false
        pushTypes.splitToSequence(',').forEachIndexed { i, rawType ->
            val type = rawType.trim().lowercase(Locale.ROOT)
            val isCall = type == "audiocall" || type == "videocall"
            // Exact type or explicit separator only: the message pt is the bare type
            // (id-less commit) or "application/im-gitmessage-id/<conv>", the invite pt
            // is "application/invite" or "application/invite+json". A plain prefix match
            // would also accept unrelated future types (application/invite-*,
            // application/im-gitmessage-identity) and let them bypass the cooldown.
            val isMessage = type == "application/im-gitmessage-id"
                    || type.startsWith("application/im-gitmessage-id/")
                    || type == "application/invite"
                    || type.startsWith("application/invite+")
            if (!isCall && !isMessage) return@forEachIndexed
            val id = ids.getOrNull(i)?.trim()
            val isNew = if (id.isNullOrEmpty()) {
                // No id to deduplicate on (single-value push without ids, or misaligned
                // lists): fail open, a missed call/message is worse than a redundant
                // restore.
                true
            } else synchronized(seenWakeupIds) {
                // Separate dedupe domains for calls and messages: both are announced on
                // the same device connection key (same scope), so a single shared key
                // would let a seen message id suppress a later call wakeup on an id
                // collision — a missed call, the worst outcome. The kind keeps the call
                // and message domains disjoint.
                val kind = if (isCall) "call" else "message"
                seenWakeupIds.put("$scope:$kind:$id", Unit) == null
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

        // Call/message value ids already handled by this process, to drop re-deliveries
        // (catch-up dumps re-push stored values on every fresh server listener).
        // Bounded LRU; 256 entries outlive by far the ~10 min DHT retention of the
        // request values being deduplicated. Companion-level: the FCM service
        // instance can be recreated between pushes within the same process.
        private const val SEEN_WAKEUP_IDS_MAX = 256
        private val seenWakeupIds = object : LinkedHashMap<String, Unit>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>) =
                size > SEEN_WAKEUP_IDS_MAX
        }
    }
}