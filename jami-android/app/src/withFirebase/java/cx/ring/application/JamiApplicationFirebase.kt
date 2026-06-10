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
package cx.ring.application

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.HiltAndroidApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class JamiApplicationFirebase : JamiApplication() {
    override val pushPlatform: String = PUSH_PLATFORM

    // Tracks how many activities are currently started (visible to user).
    // When this drops to 0 the app has gone fully to the background.
    private var startedActivityCount = 0
    private val backgroundHandler = Handler(Looper.getMainLooper())

    // Timestamps (SystemClock.elapsedRealtime) of the last push received in background,
    // used to keep accounts active long enough for the daemon to fully reconnect through
    // the DHT proxy and receive the incoming call or sync messages. An incoming call only
    // becomes visible to hasActiveCalls() once negotiation reaches CallService, which takes
    // well over the base deactivation delay after a cold proxy reconnection.
    // Only accessed on the main thread (backgroundHandler) so the pair is always coherent.
    private var lastPushTime = 0L
    private var lastCallPushTime = 0L

    private val deactivateRunnable = Runnable {
        // Double-check: still in background and push available.
        if (startedActivityCount != 0
            || !mPreferencesService.settings.enablePushNotifications
            || pushToken == null
        ) return@Runnable
        val now = SystemClock.elapsedRealtime()
        val graceRemaining = maxOf(
            lastCallPushTime + CALL_PUSH_GRACE_MS,
            lastPushTime + PUSH_GRACE_MS
        ) - now
        when {
            // An active or pending call (any non-terminal state: RINGING, CONNECTING,
            // CURRENT…): keep accounts active and check again later, so they still get
            // deactivated once the call ends even if no lifecycle event fires. Recheck
            // at a relaxed interval: waking the main thread every few seconds for the
            // whole duration of a long call would defeat the battery goal; the only
            // cost is deactivation landing up to CALL_ACTIVE_RECHECK_MS after hangup.
            mCallService.hasActiveCalls() -> scheduleBackgroundDeactivation(CALL_ACTIVE_RECHECK_MS)
            // A push was recently received: the daemon may still be reconnecting and the
            // incoming call/message may not have reached the client yet. Wait out the
            // grace window instead of killing the negotiation midway.
            graceRemaining > 0 -> scheduleBackgroundDeactivation(graceRemaining)
            else -> {
                Log.d(TAG, "App went to background with push enabled — deactivating accounts")
                mAccountService.deactivateProxyAccountsForBackground()
            }
        }
    }

    /**
     * Records that a push was received while in background, opening a grace window during
     * which background deactivation is deferred (longer for call pushes, to cover the full
     * DHT proxy reconnection and call negotiation).
     */
    fun notePushReceived(isCallPush: Boolean) {
        // Capture the receipt time now, and synchronously remove any pending — possibly
        // already-due — deactivation before posting: a due runnable sitting in the looper
        // queue would otherwise run ahead of the posted update, evaluate stale timestamps,
        // and deactivate the accounts the FCM path just restored, killing the incoming
        // call negotiation. The state update itself still happens on the main handler so
        // all lifecycle/deactivation state stays on a single thread, and the re-arm is
        // done in the same block (with a second remove covering a lifecycle re-schedule
        // racing in between).
        val now = SystemClock.elapsedRealtime()
        backgroundHandler.removeCallbacks(deactivateRunnable)
        backgroundHandler.post {
            lastPushTime = now
            if (isCallPush) lastCallPushTime = now
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundHandler.postDelayed(deactivateRunnable, BACKGROUND_DEACTIVATION_DELAY_MS)
        }
    }

    /**
     * Schedules a delayed background deactivation of accounts.
     * Called both by the activity lifecycle observer and by the FCM message handler after
     * processing a push while the app is in the background, so that accounts reactivated
     * for push handling are not left permanently active. The runnable re-schedules itself
     * while a call is active or a push grace window is open.
     */
    fun scheduleBackgroundDeactivation(delayMs: Long = BACKGROUND_DEACTIVATION_DELAY_MS) {
        // Serialize through the main looper: this is called from the FCM service thread
        // too, and mutating the callback queue directly from there would make ordering
        // with lifecycle callbacks and notePushReceived() dependent on cross-thread
        // enqueue timing (a stale re-schedule could cancel a more specific grace delay).
        backgroundHandler.post {
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundHandler.postDelayed(deactivateRunnable, delayMs)
        }
    }

    override var pushToken: Pair<String, String>? = null
        set(token) {
            //Log.d(TAG, "setPushToken: $token");
            field = token
            if (token != null && mPreferencesService.settings.enablePushNotifications) {
                mAccountService.setPushNotificationConfig(token.first, token.second, PUSH_PLATFORM)
            } else {
                mAccountService.setPushNotificationToken("")
            }
        }

    override fun onCreate() {
        super.onCreate()
        hardwareService.startTime = getCurrentTimestamp()
        hardwareService.highPriorityPushCount = 0
        hardwareService.normalPriorityPushCount = 0
        hardwareService.unknownPriorityPushCount = 0
        try {
            Log.w(TAG, "onCreate()")
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String? ->
                Log.w(TAG, "Found push token")
                try {
                    pushToken = if (token != null) Pair(token, "") else null
                } catch (e: Exception) {
                    Log.e(TAG, "Can't set push token", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't start service", e)
        }

        // When push notifications are available (FCM token set + enabled), the daemon does not
        // need to maintain its own DHT/SIP connections in the background — the proxy and FCM
        // handle incoming call/message delivery. Deactivate accounts on background to save
        // battery, and reactivate when the app comes back to the foreground.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                // Cancel any pending deactivation — app is back in foreground
                backgroundHandler.removeCallbacks(deactivateRunnable)
                // Unconditional restore: it only touches the recorded background-deactivated
                // set, so it is a no-op when nothing was deactivated. Gating it on network,
                // push setting or token state could leave accounts inactive indefinitely if
                // that state changed while the app was in background.
                if (startedActivityCount++ == 0) {
                    Log.d(TAG, "App came to foreground — reactivating accounts")
                    mAccountService.restoreProxyAccountsAfterBackground()
                }
            }
            override fun onActivityStopped(activity: Activity) {
                // Clamp to 0 to guard against lifecycle imbalance in multi-window or
                // process recreation scenarios where callbacks may be reordered.
                val newCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (newCount >= startedActivityCount) {
                    Log.w(TAG, "onActivityStopped: counter imbalance (was $startedActivityCount)")
                }
                startedActivityCount = newCount
                if (newCount == 0) scheduleBackgroundDeactivation()
            }
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun getCurrentTimestamp(withMilliseconds: Boolean = false): String {
        val pattern = if (withMilliseconds) "yyyy-MM-dd HH:mm:ss.SSS" else "yyyy-MM-dd HH:mm:ss"
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date())
    }

    fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Log.d(TAG, "onMessageReceived: ${remoteMessage.from} ${remoteMessage.priority} ${remoteMessage.originalPriority}")
        mAccountService.pushNotificationReceived(remoteMessage.from ?: "", remoteMessage.data)
        mNotificationService.processPush()
        when (remoteMessage.priority) {
            RemoteMessage.PRIORITY_HIGH -> hardwareService.highPriorityPushCount++
            RemoteMessage.PRIORITY_NORMAL -> hardwareService.normalPriorityPushCount++
            RemoteMessage.PRIORITY_UNKNOWN -> hardwareService.unknownPriorityPushCount++
        }
        val messageData = remoteMessage.data.toString()
        val currentTimestamp = getCurrentTimestamp(withMilliseconds = true)
        hardwareService.pushLogMessage("[$currentTimestamp] Received message from: ${remoteMessage.from}, data: $messageData")
    }

    companion object {
        private const val PUSH_PLATFORM = "android"
        private const val BACKGROUND_DEACTIVATION_DELAY_MS = 5_000L
        // Recheck interval while a call is active, deliberately less aggressive than
        // the base delay to avoid periodic main-thread wakeups during long calls.
        private const val CALL_ACTIVE_RECHECK_MS = 30_000L
        // Grace windows after a background push during which accounts are kept active.
        // Cold reconnection (proxy resolve + resubscribe + ICE + call signaling) routinely
        // takes tens of seconds; calls get a longer window than plain message pushes.
        private const val PUSH_GRACE_MS = 30_000L
        private const val CALL_PUSH_GRACE_MS = 60_000L
        private val TAG = JamiApplicationFirebase::class.simpleName
    }
}