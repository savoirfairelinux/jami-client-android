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
import cx.ring.service.ActiveServiceMonitor
import dagger.hilt.android.HiltAndroidApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

@HiltAndroidApp
class JamiApplicationFirebase : JamiApplication() {
    override val pushPlatform: String = PUSH_PLATFORM

    private val backgroundHandler = Handler(Looper.getMainLooper())

    // True while an activity is visible. Must be read on the main thread.
    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    // Cached foreground state, readable from any thread (set on main thread by the
    // lifecycle observer).
    @Volatile var isForeground: Boolean = false
        private set

    // elapsedRealtime of the last background push / last call push, used as grace windows
    // that keep accounts active long enough for the daemon to reconnect and deliver the
    // incoming call or message. Written from the FCM thread, read from the main thread.
    private val lastPushTime = AtomicLong(0L)
    private val lastCallPushTime = AtomicLong(0L)
    private val lastMessagePushTime = AtomicLong(0L)

    // Start of the current continuous background-active episode (0 when none). Bounds how
    // long sustained pushes can keep accounts active; only the first event records it.
    private val backgroundActiveSince = AtomicLong(0L)

    // elapsedRealtime of the last background deactivation (0 if none). Gates how soon a
    // non-call push may restore accounts again, breaking the restore/deactivate churn.
    private val lastBackgroundDeactivation = AtomicLong(0L)

    private val deactivateRunnable = Runnable {
        if (isAppVisible()) return@Runnable
        // Push no longer usable: restore accounts and fall back to always-connected behavior.
        if (!mPreferencesService.settings.enablePushNotifications || pushToken == null) {
            Log.d(TAG, "Push unavailable while backgrounded — restoring accounts")
            backgroundActiveSince.set(0L)
            mAccountService.restoreProxyAccountsAfterBackground()
            return@Runnable
        }
        val now = SystemClock.elapsedRealtime()
        val callGraceRemaining = lastCallPushTime.get() + CALL_PUSH_GRACE_MS - now
        val messageGraceRemaining = lastMessagePushTime.get() + PUSH_GRACE_MS - now
        val graceRemaining = maxOf(callGraceRemaining, messageGraceRemaining, lastPushTime.get() + PUSH_GRACE_MS - now)
        val episodeStart = backgroundActiveSince.get()
        val capReached = episodeStart != 0L && now - episodeStart >= MAX_BACKGROUND_ACTIVE_MS
        when {
            // Active/pending call or any running foreground service (file transfer, peer/hosted
            // tunnel, location sharing): keep accounts active and recheck until all sessions finish.
            mCallService.hasActiveCalls() || ActiveServiceMonitor.hasActiveServices() ->
                scheduleBackgroundDeactivation(CALL_ACTIVE_RECHECK_MS)
            // Call push still negotiating (not yet visible to hasActiveCalls): wait out its
            // grace window, also exempt from the cap.
            callGraceRemaining > 0 -> scheduleBackgroundDeactivation(callGraceRemaining)
            messageGraceRemaining > 0 -> scheduleBackgroundDeactivation(messageGraceRemaining)
            // Recent push: wait out the grace window unless the episode cap is reached.
            graceRemaining > 0 && !capReached -> scheduleBackgroundDeactivation(graceRemaining)
            else -> {
                Log.d(TAG, "App went to background with push enabled — deactivating accounts"
                        + if (capReached) " (background-active cap reached)" else "")
                // Open the non-call cooldown only when a real episode concludes; redundant
                // passes (no episode) must not slide it forward.
                if (episodeStart != 0L) lastBackgroundDeactivation.set(now)
                backgroundActiveSince.set(0L)
                mAccountService.deactivateProxyAccountsForBackground()
            }
        }
    }

    /**
     * Handles a push received while backgrounded: opens the grace window, restores accounts
     * (and reconnects for call/message pushes), then re-arms the deactivation check.
     */
    fun onBackgroundPushReceived(isCallPush: Boolean, isMessagePush: Boolean, isExpiration: Boolean = false) {
        // Expired value: already gone from the DHT, nothing to fetch or answer.
        if (isExpiration) return
        // Background noise (neither call nor message): gated during the post-deactivation
        // cooldown to avoid re-feeding the reconnect churn. 0 means no deactivation yet.
        if (!isCallPush && !isMessagePush) {
            val lastDeactivation = lastBackgroundDeactivation.get()
            if (lastDeactivation != 0L
                && SystemClock.elapsedRealtime() - lastDeactivation < NONCALL_RESTORE_COOLDOWN_MS
            ) {
                return
            }
        }
        // Publish the grace window before cancelling: a deactivateRunnable already running
        // reads these atomics, and any deactivation it queues runs FIFO after the restore below.
        val now = SystemClock.elapsedRealtime()
        lastPushTime.set(now)
        if (isCallPush) lastCallPushTime.set(now)
        if (isMessagePush) lastMessagePushTime.set(now)
        backgroundActiveSince.compareAndSet(0L, now)
        backgroundHandler.removeCallbacks(deactivateRunnable)
        backgroundHandler.post {
            // Call pushes always restore/reconnect; non-call pushes keep the push-availability
            // gate so a stale delivery after push was disabled cannot reactivate accounts.
            if (isCallPush
                || (mPreferencesService.settings.enablePushNotifications && pushToken != null)
            ) {
                mAccountService.restoreProxyAccountsAfterBackground()
                // Full DHT/SIP reconnect to rebuild sockets torn down in doze.
                if (isCallPush || isMessagePush) hardwareService.connectivityChanged(true)
            }
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundHandler.postDelayed(deactivateRunnable, BACKGROUND_DEACTIVATION_DELAY_MS)
        }
    }

    /**
     * Schedules a delayed background deactivation, serialized through the main looper since
     * it is also called from the FCM service thread. The runnable re-schedules itself while
     * a call is active or a push grace window is open.
     */
    fun scheduleBackgroundDeactivation(delayMs: Long = BACKGROUND_DEACTIVATION_DELAY_MS) {
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
                // Token arrived while already backgrounded: the last check ran without it and
                // restored accounts, so re-arm the deactivation now that push works.
                backgroundHandler.post { if (!isAppVisible()) scheduleBackgroundDeactivation() }
            } else {
                mAccountService.setPushNotificationToken("")
                // Push unusable: restore immediately (no-op if nothing was deactivated).
                mAccountService.restoreProxyAccountsAfterBackground()
            }
        }

    // Guards against duplicate observer registration.
    private var lifecycleObserverRegistered = false
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            isForeground = true
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundActiveSince.set(0L)
            // Unconditional restore: only touches the recorded set, a no-op otherwise.
            Log.d(TAG, "App came to foreground — reactivating accounts")
            mAccountService.restoreProxyAccountsAfterBackground()
        }
        override fun onStop(owner: LifecycleOwner) {
            isForeground = false
            backgroundActiveSince.compareAndSet(0L, SystemClock.elapsedRealtime())
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

        // With push available, deactivate accounts in background to save battery and
        // restore them on foreground. ProcessLifecycleOwner gives reliable app-wide
        // foreground transitions across activity changes and configuration changes.
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
        Log.d(TAG, "onMessageReceived: ${remoteMessage.from} ${remoteMessage.priority} ${remoteMessage.originalPriority}")
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
        // Recheck interval while a call is active (less aggressive than the base delay).
        private const val CALL_ACTIVE_RECHECK_MS = 30_000L
        // Grace windows keeping accounts active after a background push; calls get longer.
        private const val PUSH_GRACE_MS = 30_000L
        private const val CALL_PUSH_GRACE_MS = 60_000L
        // Upper bound for one continuous background-active episode under sustained pushes.
        private const val MAX_BACKGROUND_ACTIVE_MS = 10 * 60_000L
        // Minimum time deactivated before a non-call push may restore accounts again.
        private const val NONCALL_RESTORE_COOLDOWN_MS = 3 * 60_000L
        private val TAG = JamiApplicationFirebase::class.simpleName
    }
}
