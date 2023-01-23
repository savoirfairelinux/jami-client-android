/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.application

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JamiApplicationFirebase : JamiApplication() {

    override var pushToken: String? = null
        set(token) {
            //Log.d(TAG, "setPushToken: $token");
            field = token
            if (mPreferencesService.settings.enablePushNotifications) {
                mAccountService.setPushNotificationToken(token ?: "")
            }
        }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.w(TAG, "onCreate()")
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String? ->
                Log.w(TAG, "Found push token")
                try {
                    pushToken = token
                } catch (e: Exception) {
                    Log.e(TAG, "Can't set push token", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't start service", e)
        }
    }

    fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
        mAccountService.pushNotificationReceived(remoteMessage.from ?: "", remoteMessage.data)
    }

    companion object {
        private val TAG = JamiApplicationFirebase::class.simpleName
    }
}