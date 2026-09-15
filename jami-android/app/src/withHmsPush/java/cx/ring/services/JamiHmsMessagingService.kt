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
package cx.ring.services

import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class JamiHmsMessagingService : HmsMessageService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        PushMessageHandler.handlePush(
            service = this,
            from = remoteMessage.from ?: "",
            data = remoteMessage.dataOfMap ?: emptyMap(),
            priority = toPushPriority(remoteMessage.urgency),
            originalPriority = toPushPriority(remoteMessage.originalUrgency),
            scope = serviceScope,
        )
    }

    override fun onNewToken(token: String?) {
        Log.w(TAG, "onNewToken")
        if (token.isNullOrEmpty()) return
        val app = JamiApplication.instance as? JamiApplicationPush
        app?.pushToken = Pair(token, "")
    }

    override fun onTokenError(e: Exception) {
        Log.e(TAG, "onTokenError", e)
    }

    // HMS urgency values happen to match the FCM scale, but map explicitly so a divergence
    // shows up here rather than as a silently wrong push count or missing foreground service.
    private fun toPushPriority(urgency: Int) = when (urgency) {
        RemoteMessage.PRIORITY_HIGH -> JamiApplicationPush.PUSH_PRIORITY_HIGH
        RemoteMessage.PRIORITY_NORMAL -> JamiApplicationPush.PUSH_PRIORITY_NORMAL
        else -> JamiApplicationPush.PUSH_PRIORITY_UNKNOWN
    }

    companion object {
        private const val TAG = "JamiHmsMessaging"
    }
}
