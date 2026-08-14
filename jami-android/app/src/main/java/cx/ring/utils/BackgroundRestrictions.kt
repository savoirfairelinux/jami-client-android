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
package cx.ring.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Reports the system restrictions that keep the application from being reachable in background.
 *
 * A high priority push notification always wakes the application up, but waking up is not enough:
 * the system may still deny the network access needed to open the peer connection. That happens
 * when the application sits in a degraded standby bucket, when its battery usage is set to
 * "Restricted", or when Data Saver is on and the application is not allowlisted. None of these is
 * visible to the user, who only observes that calls and messages never arrive.
 *
 * Every value is read for the calling application only and requires no permission.
 */
object BackgroundRestrictions {
    private val TAG = BackgroundRestrictions::class.simpleName!!

    enum class StandbyBucket { ACTIVE, WORKING_SET, FREQUENT, RARE, RESTRICTED, UNKNOWN }

    /**
     * Immutable snapshot of every restriction, gathered in a single pass. Each field costs a
     * binder call, so callers should hold a snapshot rather than query repeatedly.
     */
    data class Status(
        val standbyBucket: StandbyBucket,
        val ignoringBatteryOptimizations: Boolean,
        val backgroundRestricted: Boolean,
        val dataSaverStatus: Int
    ) {
        /**
         * True when the system is expected to deny background network access, in which case an
         * incoming call or message fails to connect even though its push notification was
         * delivered and correctly classified. The platform documents background network as
         * disabled for the rare and restricted buckets.
         *
         * @see <a href="https://developer.android.com/topic/performance/power/power-details">Power
         * management resource limits</a>
         */
        val networkRestricted: Boolean
            get() = backgroundRestricted
                    || standbyBucket == StandbyBucket.RARE
                    || standbyBucket == StandbyBucket.RESTRICTED
                    || dataSaverStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED

        /** Compact description meant for logs and bug reports. */
        override fun toString(): String =
            "standbyBucket=$standbyBucket" +
                    " ignoringBatteryOptimizations=$ignoringBatteryOptimizations" +
                    " backgroundRestricted=$backgroundRestricted" +
                    " dataSaver=${dataSaverName(dataSaverStatus)}" +
                    " networkRestricted=$networkRestricted"

        private fun dataSaverName(status: Int): String = when (status) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "disabled"
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "whitelisted"
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "enabled"
            else -> "unknown"
        }
    }

    fun status(context: Context) = Status(
        standbyBucket(context),
        isIgnoringBatteryOptimizations(context),
        isBackgroundRestricted(context),
        dataSaverStatus(context)
    )

    /** The standby bucket the system currently assigns to this application. */
    fun standbyBucket(context: Context): StandbyBucket {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return StandbyBucket.UNKNOWN
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return StandbyBucket.UNKNOWN
        return try {
            when (usageStats.appStandbyBucket) {
                UsageStatsManager.STANDBY_BUCKET_ACTIVE -> StandbyBucket.ACTIVE
                UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> StandbyBucket.WORKING_SET
                UsageStatsManager.STANDBY_BUCKET_FREQUENT -> StandbyBucket.FREQUENT
                UsageStatsManager.STANDBY_BUCKET_RARE -> StandbyBucket.RARE
                UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> StandbyBucket.RESTRICTED
                else -> StandbyBucket.UNKNOWN
            }
        } catch (e: Exception) {
            Log.w(TAG, "Can't read standby bucket", e)
            StandbyBucket.UNKNOWN
        }
    }

    /** Whether the user granted the battery optimization exemption ("Unrestricted"). */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Whether the user set battery usage to "Restricted", the most severe setting. */
    fun isBackgroundRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        return activityManager.isBackgroundRestricted
    }

    /** One of `ConnectivityManager.RESTRICT_BACKGROUND_STATUS_*`. */
    fun dataSaverStatus(context: Context): Int {
        val connectivity =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED
        return connectivity.restrictBackgroundStatus
    }

    /**
     * Asks the user to exempt the application from battery optimizations, falling back to the
     * system list when the direct dialog is unavailable.
     *
     * Real-time communication is one of the use cases the Play Store policy accepts for this
     * permission: without the exemption the system may deny background network access, and
     * incoming calls and messages are then silently dropped.
     *
     * @return true if a settings screen was opened.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean = try {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "Can't request battery optimization exemption", e)
        openSettings(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /** Opens the application details, from which battery and data usage are both reachable. */
    fun openApplicationSettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "Can't open application settings", e)
        false
    }

    private fun openSettings(context: Context, action: String): Boolean = try {
        context.startActivity(Intent(action))
        true
    } catch (e: Exception) {
        Log.w(TAG, "Can't open $action", e)
        false
    }
}
