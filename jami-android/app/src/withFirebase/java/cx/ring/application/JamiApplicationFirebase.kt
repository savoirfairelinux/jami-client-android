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

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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

    private val backgroundHandler = Handler(Looper.getMainLooper())

    // Process-level foreground state, from ProcessLifecycleOwner: STARTED or above means
    // some activity is visible. Must be read on the main thread — all callers here run
    // on backgroundHandler (main looper).
    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    // Timestamps (SystemClock.elapsedRealtime) of the last push received in background,
    // used to keep accounts active long enough for the daemon to fully reconnect through
    // the DHT proxy and receive the incoming call or sync messages. An incoming call only
    // becomes visible to hasActiveCalls() once negotiation reaches CallService, which takes
    // well over the base deactivation delay after a cold proxy reconnection.
    // Only accessed on the main thread (backgroundHandler) so the pair is always coherent.
    private var lastPushTime = 0L
    private var lastCallPushTime = 0L

    private val deactivateRunnable = Runnable {
        // Still in foreground: nothing to do.
        if (isAppVisible()) return@Runnable
        // Push became unavailable while backgrounded (user disabled it, token cleared):
        // the deactivation model is no longer viable — without FCM wakeups the recorded
        // accounts would stay unreachable until the next foreground event. Restore them
        // and fall back to the historical always-connected background behavior.
        if (!mPreferencesService.settings.enablePushNotifications || pushToken == null) {
            Log.d(TAG, "Push unavailable while backgrounded — restoring accounts")
            mAccountService.restoreProxyAccountsAfterBackground()
            return@Runnable
        }
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
     * Handles a push received while in background: opens the deactivation grace window,
     * queues the account restore and optional reconnect, and re-arms the deactivation
     * check — all from the main thread, in that order. Serializing the whole sequence
     * here makes the ordering deterministic: the grace timestamps are set before the
     * restore is queued, and the deactivation timer is armed only after. Since the
     * daemon executor is a single FIFO queue, any deactivation the re-armed runnable
     * later enqueues is guaranteed to execute after the restore queued here.
     */
    fun onBackgroundPushReceived(isCallPush: Boolean, triggerReconnect: Boolean) {
        // Capture the receipt time now, and synchronously remove any pending — possibly
        // already-due — deactivation before posting: a due runnable sitting in the looper
        // queue would otherwise run ahead of the posted update, evaluate stale timestamps,
        // and deactivate the accounts this push is about to restore, killing the incoming
        // call negotiation.
        val now = SystemClock.elapsedRealtime()
        backgroundHandler.removeCallbacks(deactivateRunnable)
        backgroundHandler.post {
            lastPushTime = now
            if (isCallPush) lastCallPushTime = now
            // Restore accounts deactivated by the background optimization: receiving an
            // FCM push implies the network is available, so don't gate this on
            // possibly-stale connectivity state. Accounts deliberately disabled by the
            // user are not in the restored set and stay inactive.
            mAccountService.restoreProxyAccountsAfterBackground()
            // connectivityChanged triggers a full DHT/SIP reconnect — only requested for
            // call pushes, where low latency matters.
            if (triggerReconnect) hardwareService.connectivityChanged(true)
            // Arm the deactivation check only after the restore has been queued, with a
            // second remove covering a lifecycle re-schedule racing in between.
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundHandler.postDelayed(deactivateRunnable, BACKGROUND_DEACTIVATION_DELAY_MS)
        }
    }

    /**
     * Schedules a delayed background deactivation of accounts.
     * Called by the process lifecycle observer when the app leaves the foreground, and by
     * the FCM message handler after processing a push in the background, so that accounts
     * reactivated for push handling are not left permanently active. The runnable
     * re-schedules itself while a call is active or a push grace window is open.
     */
    fun scheduleBackgroundDeactivation(delayMs: Long = BACKGROUND_DEACTIVATION_DELAY_MS) {
        // Serialize through the main looper: this is called from the FCM service thread
        // too, and mutating the callback queue directly from there would make ordering
        // with lifecycle callbacks and onBackgroundPushReceived() dependent on cross-thread
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
                // If the token arrives while already backgrounded (app left the
                // foreground before the FCM fetch completed), the last deactivation
                // check ran with a null token and restored the accounts: re-arm it now
                // that push is functional, otherwise they stay active until the next
                // lifecycle transition.
                backgroundHandler.post { if (!isAppVisible()) scheduleBackgroundDeactivation() }
            } else {
                mAccountService.setPushNotificationToken("")
                // Push became unusable (token cleared or push disabled): if accounts
                // were already deactivated there may be no pending deactivateRunnable
                // left to run the restore fallback — restore immediately, reachability
                // cannot wait for the next foreground transition.
                mAccountService.restoreProxyAccountsAfterBackground()
            }
        }

    // Guards against duplicate observer registration if onCreate() runs again through
    // custom/test initialization paths: duplicate observers would schedule duplicate
    // restore/deactivation work.
    private var lifecycleObserverRegistered = false
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Cancel any pending deactivation — app is back in foreground.
            backgroundHandler.removeCallbacks(deactivateRunnable)
            // Unconditional restore: it only touches the recorded background-deactivated
            // set, so it is a no-op when nothing was deactivated. Gating it on network,
            // push setting or token state could leave accounts inactive indefinitely if
            // that state changed while the app was in background.
            Log.d(TAG, "App came to foreground — reactivating accounts")
            mAccountService.restoreProxyAccountsAfterBackground()
        }
        override fun onStop(owner: LifecycleOwner) {
            scheduleBackgroundDeactivation()
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
        // ProcessLifecycleOwner replaces a manual started-activity counter: it natively
        // handles configuration changes and activity-to-activity transitions (onStop is
        // dispatched only after a delay once the last activity stops), so a reordered or
        // unbalanced stop event cannot deactivate accounts while the app is still visible.
        if (!lifecycleObserverRegistered) {
            lifecycleObserverRegistered = true
            ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
        }
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