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

import android.content.Context
import android.util.Log
import com.huawei.hms.push.HmsMessaging
import com.huawei.hms.push.ups.UPSService
import com.huawei.hms.push.ups.entity.TokenResult
import com.huawei.hms.push.ups.entity.UPSRegisterCallBack
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JamiApplicationHuawei : JamiApplication() {
    override val pushPlatform: String = PUSH_PLATFORM

    override var pushToken: Pair<String, String>? = null
        set(token) {
            Log.d(TAG, "setPushToken: $token")
            field = token
            if (!isRuntimeReady) return
            if (token != null && mPreferencesService.settings.enablePushNotifications) {
                mAccountService.setPushNotificationConfig(token.first, token.second, PUSH_PLATFORM)
            } else {
                mAccountService.setPushNotificationToken("")
            }
        }

    override fun onRuntimeReady() {
        super.onRuntimeReady()
        try {
            Log.w(TAG, "onRuntimeReady()")
            HmsMessaging.getInstance(this).isAutoInitEnabled = true
            // HMS Core already knows the token on devices where it manages the app; asking
            // again makes the token available without waiting for a rotation.
            UPSService.registerToken(this, "", HmsMessaging.DEFAULT_TOKEN_SCOPE, "", object : UPSRegisterCallBack {
                override fun onResult(result: TokenResult) {
                    Log.w(TAG, "registerToken result: ${result.returnCode} ${result.reason}")
                    result.token?.let { pushToken = Pair(it, "") }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Can't start HMS push", e)
        }
    }

    override fun activityInit(activityContext: Context) {
        // Nothing to select at runtime: the token comes from HMS Core on the device.
    }

    fun onMessage(remoteMessage: Map<String, String>) {
        if (!prepareRuntime()) return
        bootstrapDaemon()
        mAccountService.pushNotificationReceived("", remoteMessage)
        mNotificationService.processPush()
    }

    companion object {
        // The DHT proxy registers the token under this platform name.
        private const val PUSH_PLATFORM = "huawei"
        private val TAG = JamiApplicationHuawei::class.simpleName
    }
}
