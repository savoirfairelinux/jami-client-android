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

import android.util.Log
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessaging
import cx.ring.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlin.concurrent.thread

/**
 * Push through Huawei Mobile Services, for AppGallery builds on devices without Google
 * Play services. The DHT proxy relays these through gorush exactly like FCM ones, so only
 * token acquisition differs.
 */
@HiltAndroidApp
class JamiApplicationHms : JamiApplicationPush() {
    override val pushPlatform: String = PUSH_PLATFORM

    override fun initializePush() {
        if (BuildConfig.HMS_APP_ID.isEmpty()) {
            Log.e(TAG, "No HMS app id configured, push notifications are disabled")
            return
        }
        // Lets the SDK refresh the token on its own and report it through onNewToken.
        HmsMessaging.getInstance(this).isAutoInitEnabled = true
        // getToken() performs a blocking network call on first run.
        thread(name = "hms-token") {
            try {
                val token = HmsInstanceId.getInstance(this)
                    .getToken(BuildConfig.HMS_APP_ID, HmsMessaging.DEFAULT_TOKEN_SCOPE)
                if (token.isNullOrEmpty()) {
                    // Normal on first run: the SDK delivers it later through onNewToken.
                    Log.w(TAG, "No push token yet, waiting for onNewToken")
                    return@thread
                }
                Log.w(TAG, "Found push token")
                pushToken = Pair(token, "")
            } catch (e: Exception) {
                Log.e(TAG, "Can't get push token", e)
            }
        }
    }

    companion object {
        // Must match DhtProxyServer::getTypeFromString, which maps it to PushType::Huawei.
        private const val PUSH_PLATFORM = "huawei"
        private val TAG = JamiApplicationHms::class.simpleName
    }
}
