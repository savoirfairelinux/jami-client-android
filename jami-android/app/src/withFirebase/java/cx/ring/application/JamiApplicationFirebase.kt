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
    private val deactivateRunnable = Runnable {
        // Double-check: still in background, push available, and no active/pending call.
        // hasActiveCalls() covers all non-terminal states (RINGING, CONNECTING, CURRENT…)
        // so an incoming call being negotiated also prevents premature deactivation.
        if (startedActivityCount == 0
            && mPreferencesService.settings.enablePushNotifications
            && pushToken != null
            && !mCallService.hasActiveCalls()
        ) {
            Log.d(TAG, "App went to background with push enabled — deactivating accounts")
            mAccountService.setAccountsActiveForBackground(false)
        }
    }

    /**
     * Schedules a delayed background deactivation of accounts.
     * Called both by the activity lifecycle observer and by the FCM message handler after
     * processing a push while the app is in the background, so that accounts reactivated
     * for push handling are not left permanently active.
     */
    fun scheduleBackgroundDeactivation() {
        backgroundHandler.removeCallbacks(deactivateRunnable)
        backgroundHandler.postDelayed(deactivateRunnable, BACKGROUND_DEACTIVATION_DELAY_MS)
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
                if (startedActivityCount++ == 0
                    && mPreferencesService.settings.enablePushNotifications
                    && pushToken != null
                    && mPreferencesService.hasNetworkConnected()
                ) {
                    Log.d(TAG, "App came to foreground — reactivating accounts")
                    mAccountService.setAccountsActiveForBackground(true)
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
        private val TAG = JamiApplicationFirebase::class.simpleName
    }
}