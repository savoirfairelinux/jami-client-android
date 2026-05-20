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
package cx.ring.services

import android.util.Log
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationUnifiedPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class JamiPushService : PushService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewEndpoint(
        endpoint: PushEndpoint,
        instance: String
    ) {
        Log.w(TAG, "onNewEndpoint $endpoint $instance")
        val app = JamiApplication.instance as JamiApplicationUnifiedPush?
        val topicKey = endpoint.pubKeySet?.let { "${it.pubKey}|${it.auth}" } ?: ""
        app?.pushToken = Pair(endpoint.url, topicKey)
    }

    override fun onMessage(
        message: PushMessage,
        instance: String
    ) {
        val app = JamiApplication.instance as JamiApplicationUnifiedPush?

        // Parse the JSON payload to extract the priority field added by the
        // OpenDHT proxy. The HTTP Urgency header is not exposed by the
        // UnifiedPush connector, so the proxy includes it in the body.
        val msgStr = String(message.content)
        val isHigh = try {
            val obj = JSONObject(msgStr)
            obj.optString("priority", "high").equals("high", ignoreCase = true)
        } catch (e: Exception) {
            true // default to high if parsing fails
        }

        if (app?.onPushReceived(isHigh) == false) {
            return
        }

        serviceScope.launch {
            try {
                if (app == null) return@launch
                if (!app.ensureDaemonStarted()) {
                    Log.w(TAG, "Daemon not started, dropping push")
                    return@launch
                }
                Log.w(TAG, "onMessage $msgStr $instance")
                val obj = JSONObject(msgStr)
                val msg = HashMap<String, String>()
                obj.keys().forEach { msg[it] = obj.getString(it) }
                app.onMessage(msg)
            } catch (e: Exception) {
                Log.e(TAG, "onMessage", e)
            } finally {
                app?.onPushProcessed()
            }
        }
    }

    override fun onRegistrationFailed(
        reason: FailedReason,
        instance: String
    ) {
        Log.w(TAG, "onRegistrationFailed $instance")
    }

    override fun onUnregistered(instance: String) {
        Log.w(TAG, "onUnregistered $instance")
    }

    companion object {
        private val TAG = JamiPushService::class.simpleName
    }
}
