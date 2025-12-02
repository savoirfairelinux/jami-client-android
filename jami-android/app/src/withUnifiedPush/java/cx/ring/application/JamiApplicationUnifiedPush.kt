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
import dagger.hilt.android.HiltAndroidApp
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.ui.SelectDistributorDialogsBuilder
import org.unifiedpush.android.connector.ui.UnifiedPushFunctions

@HiltAndroidApp
class JamiApplicationUnifiedPush : JamiApplication() {
    override val pushPlatform: String = PUSH_PLATFORM

    override var pushToken: Pair<String, String> = Pair("", "")
        set(token) {
            Log.d(TAG, "setPushToken: $token");
            field = token
            if (mPreferencesService.settings.enablePushNotifications) {
                mAccountService.setPushNotificationConfig(token.first, token.second, PUSH_PLATFORM)
            } else {
                mAccountService.setPushNotificationToken("")
            }
        }

    override fun activityInit(activityContext: Context) {
        try {
            Log.w(TAG, "onCreate()")
            SelectDistributorDialogsBuilder(
                activityContext,
                object : UnifiedPushFunctions {
                    override fun tryUseDefaultDistributor(callback: (Boolean) -> Unit) =
                        UnifiedPush.tryUseDefaultDistributor(activityContext, callback)

                    override fun getAckDistributor(): String? =
                        UnifiedPush.getAckDistributor(activityContext)

                    override fun getDistributors(): List<String> =
                        UnifiedPush.getDistributors(activityContext)

                    override fun register(instance: String) =
                        UnifiedPush.register(activityContext, instance)

                    override fun saveDistributor(distributor: String) =
                        UnifiedPush.saveDistributor(activityContext, distributor)
                }
            ).run()
        } catch (e: Exception) {
            Log.e(TAG, "Can't start service", e)
        }
    }

    fun onMessage(remoteMessage: Map<String, String>) {
        //Log.d(TAG, "onMessage: from:${remoteMessage.from} priority:${remoteMessage.priority} (was ${remoteMessage.originalPriority})")
        mAccountService.pushNotificationReceived("", remoteMessage)
    }

    companion object {
        private const val PUSH_PLATFORM = "unifiedpush"
        private val TAG = JamiApplicationUnifiedPush::class.simpleName
    }
}