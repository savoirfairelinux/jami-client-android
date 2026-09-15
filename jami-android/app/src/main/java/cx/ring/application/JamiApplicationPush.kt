/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
import cx.ring.service.ActiveServiceMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Behavior shared by every flavor whose push arrives through a system-wide relay
 * (FCM on Google devices, HMS on Huawei ones): background account deactivation, the
 * grace windows that keep accounts alive long enough to answer, and token registration.
 *
 * A subclass only contributes its SDK: token retrieval in [initializePush], and delivery
 * through [onMessageReceived]. Nothing here may reference a vendor SDK type, since this
 * class is compiled into the flavors that ship none.
 */
abstract class JamiApplicationPush : JamiApplication() {

    private val backgroundHandler = Handler(Looper.getMainLooper())

    /** Acquires the push token from the vendor SDK. Called once, from [onCreate]. */
    protected abstract fun initializePush()

    // True while an activity is visible. Must be read on the main thread.
    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    // Cached foreground state, readable from any thread (set on main thread by the
    // lifecycle observer).
    @Volatile var isForeground: Boolean = false
        private set

    // elapsedRealtime of the last background push or token refresh / last call push, used
    // as grace windows that keep accounts active long enough for the daemon to reconnect
    // and deliver the incoming call or message. Written from the FCM thread, read from the
    // main thread.
    private val lastPushTime = AtomicLong(0L)
    private val lastCallPushTime = AtomicLong(0L)
    private val lastMessagePushTime = AtomicLong(0L)

    // Start of the current continuous background-active episode (0 when none). Bounds how
    // long sustained pushes can keep accounts active; only the first event records it.
    private val backgroundActiveSince = AtomicLong(0L)

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
            // Recent push/token refresh: wait out the grace window unless the episode cap is reached.
            graceRemaining > 0 && !capReached -> scheduleBackgroundDeactivation(graceRemaining)
            else -> {
                Log.d(TAG, "App went to background with push enabled — deactivating accounts"
                        + if (capReached) " (background-active cap reached)" else "")
                backgroundActiveSince.set(0L)
                mAccountService.deactivateProxyAccountsForBackground()
            }
        }
    }

    /**
     * Handles an actionable push received while backgrounded: opens the grace window,
     * restores accounts (and reconnects for call/message pushes), then re-arms the
     * deactivation check. The caller filters on relay priority, so presence and expiration
     * pushes never get here.
     */
    fun onBackgroundPushReceived(isCallPush: Boolean, isMessagePush: Boolean) {
        // Publish the grace window before cancelling: a deactivateRunnable already running
        // reads these atomics, and any deactivation it queues runs FIFO after the restore below.
        val now = SystemClock.elapsedRealtime()
        lastPushTime.set(now)
        if (isCallPush) lastCallPushTime.set(now)
        if (isMessagePush) lastMessagePushTime.set(now)
        backgroundActiveSince.compareAndSet(0L, now)
        backgroundHandler.removeCallbacks(deactivateRunnable)
        backgroundHandler.post {
            // No-op unless a background deactivation actually recorded accounts to restore.
            mAccountService.restoreProxyAccountsAfterBackground()
            // Full DHT/SIP reconnect to rebuild sockets torn down in doze.
            if (isCallPush || isMessagePush) hardwareService.connectivityChanged(true)
            backgroundHandler.removeCallbacks(deactivateRunnable)
            backgroundHandler.postDelayed(deactivateRunnable, BACKGROUND_DEACTIVATION_DELAY_MS)
        }
    }

    /**
     * Schedules a delayed background deactivation, serialized through the main looper since
     * it is also called from the push service thread. The runnable re-schedules itself while
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
                mAccountService.setPushNotificationConfig(token.first, token.second, pushPlatform)
                // Keep the account online long enough to re-announce the new token before
                // background optimization shuts the DHT down.
                lastPushTime.set(SystemClock.elapsedRealtime())
                backgroundHandler.post {
                    mAccountService.restoreProxyAccountsAfterBackground()
                    if (!isAppVisible()) {
                        hardwareService.connectivityChanged(true)
                        scheduleBackgroundDeactivation(PUSH_GRACE_MS)
                    }
                }
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
            initializePush()
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

    /**
     * Hands a received push to the daemon. [priority] uses the [PUSH_PRIORITY_HIGH] /
     * [PUSH_PRIORITY_NORMAL] scale, which each flavor maps from its own SDK.
     */
    fun onMessageReceived(from: String, data: Map<String, String>, priority: Int) {
        Log.d(TAG, "onMessageReceived: $from $priority")
        mAccountService.pushNotificationReceived(from, data)
        mNotificationService.processPush()
        when (priority) {
            PUSH_PRIORITY_HIGH -> hardwareService.highPriorityPushCount++
            PUSH_PRIORITY_NORMAL -> hardwareService.normalPriorityPushCount++
            else -> hardwareService.unknownPriorityPushCount++
        }
        val currentTimestamp = getCurrentTimestamp(withMilliseconds = true)
        hardwareService.pushLogMessage("[$currentTimestamp] Received message from: $from, data: $data")
    }

    // Logs report the concrete flavor rather than this base class.
    private val TAG = this::class.simpleName

    companion object {
        // Values match FCM's RemoteMessage.PRIORITY_* constants, so the Firebase flavor can
        // forward its priority unchanged; other SDKs map onto this scale.
        const val PUSH_PRIORITY_UNKNOWN = 0
        const val PUSH_PRIORITY_HIGH = 1
        const val PUSH_PRIORITY_NORMAL = 2

        private const val BACKGROUND_DEACTIVATION_DELAY_MS = 5_000L
        // Recheck interval while a call is active (less aggressive than the base delay).
        private const val CALL_ACTIVE_RECHECK_MS = 30_000L
        // Grace windows keeping accounts active after a background push; calls get longer.
        private const val PUSH_GRACE_MS = 30_000L
        private const val CALL_PUSH_GRACE_MS = 60_000L
        // Upper bound for one continuous background-active episode under sustained pushes.
        private const val MAX_BACKGROUND_ACTIVE_MS = 10 * 60_000L
    }
}
