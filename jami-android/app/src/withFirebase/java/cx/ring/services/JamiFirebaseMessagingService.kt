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

        // Same normalized classifier as the restore path below: a case-sensitive
        // substring check here would let a producer-side casing change restore
        // accounts via isDhtProxyCallWakeup but skip the foreground-service/wakelock
        // protection intended for incoming calls.
        val isCallNotification = isDhtProxyCallWakeup(remoteMessage)
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
            // Positive call signal only, from the protocol-guaranteed pt field: the
            // daemon always labels call connection requests audioCall/videoCall, so a
            // priority-based fallback is not needed — and would misclassify regular DHT
            // message pushes (high priority by default, often without pt) as calls,
            // triggering a full reconnect on every message and defeating the battery
            // optimization. A call push lacking pt still gets the standard grace window
            // and account restore below, which covers negotiation comfortably.
            val isCallPush = isCallNotification || isDhtProxyCallWakeup(remoteMessage)
            // Expired-value notification from the DHT proxy ("exp" field): the value is
            // gone from the DHT, there is nothing left to fetch or answer — measured at
            // ~24% of all pushes during peer retry storms. Never classify a call push as
            // expiration: a mislabeled call must keep the full restore path.
            val isExpirationOnly = !isCallPush && remoteMessage.data.containsKey("exp")
            // Single serialized entry point: the push-availability gate, grace window,
            // account restore, optional reconnect and deactivation re-arm are all
            // evaluated on the main thread (see onBackgroundPushReceived) — preference
            // and token state must not be read from this FCM thread, where they could
            // race a settings/token transition. connectivityChanged (full DHT/SIP
            // reconnect) is only requested for call pushes, where low latency matters:
            // message/sync pushes are also delivered with high priority (DHT values
            // default to high), so priority alone must not trigger it.
            app.onBackgroundPushReceived(
                isCallPush = isCallPush,
                triggerReconnect = isCallPush
                        && remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH,
                isExpiration = isExpirationOnly
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
                val isCallPush = isCallNotification || isDhtProxyCallWakeup(remoteMessage)
                if (!isCallPush) {
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
     * Classifies a DHT proxy call wakeup from a positive signal only: the proxy copies
     * the connection request type into the "pt" field ("audioCall"/"videoCall" for
     * calls, "sip"/"git"… for messaging and sync channels), as a comma-separated list
     * of exact values when the push covers several. FCM priority is not used as fallback:
     * regular DHT values default to high priority too, so payloads without "pt" would
     * all be misclassified as calls and needlessly get the longer call grace window.
     * Non-call pushes misclassified as calls keep accounts active for up to 60s;
     * a call push lacking "pt" still gets the standard 30s grace window, which covers
     * the measured negotiation time several times over.
     */
    private fun isDhtProxyCallWakeup(remoteMessage: RemoteMessage): Boolean {
        val pushType = remoteMessage.data["pt"] ?: return false
        // Normalize case: this gates incoming-call reliability, so a producer-side
        // casing change must not silently downgrade call wakeups to plain pushes.
        return pushType.splitToSequence(',')
            .map { it.trim().lowercase(Locale.ROOT) }
            .any { it == "audiocall" || it == "videocall" }
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
        val app = JamiApplication.instance as? JamiApplicationFirebase
        app?.pushToken = Pair(refreshedToken, "")
    }

    companion object {
        private const val TAG = "JamiFirebaseMessaging"
    }
}