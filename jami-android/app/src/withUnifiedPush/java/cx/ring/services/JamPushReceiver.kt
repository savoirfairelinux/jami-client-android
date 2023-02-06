/*
 *  Copyright (C) 2022 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import android.content.Context
import android.util.Log
import cx.ring.application.JamiApplication
import cx.ring.application.JamiApplicationUnifiedPush
import org.json.JSONObject
import org.unifiedpush.android.connector.MessagingReceiver
import java.net.URI

class JamiPushReceiver : MessagingReceiver() {
    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        Log.w("JamiPushReceiver", "onMessage ${String(message)} $instance")
        val obj = JSONObject(String(message))
        val msg = HashMap<String, String>()
        obj.keys().forEach { msg[it] = obj.getString(it) }
        val app = JamiApplication.instance as JamiApplicationUnifiedPush?
        app?.onMessage(msg)
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        Log.w("JamiPushReceiver", "onNewEndpoint $endpoint $instance")
        val app = JamiApplication.instance as JamiApplicationUnifiedPush?
        //val uri = URI(endpoint) // Drop ?up=1 or query
        //app?.pushToken = "${uri.scheme}://${uri.authority}${uri.path}"
        app?.pushToken = endpoint
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        // called when the registration is not possible, eg. no network
        Log.w("JamiPushReceiver", "onRegistrationFailed $instance")
    }

    override fun onUnregistered(context: Context, instance: String){
        // called when this application is unregistered from receiving push messages
        Log.w("JamiPushReceiver", "onUnregistered $instance")
    }

}