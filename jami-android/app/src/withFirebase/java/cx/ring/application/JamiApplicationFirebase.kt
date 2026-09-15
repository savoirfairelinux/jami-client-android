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
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JamiApplicationFirebase : JamiApplicationPush() {
    override val pushPlatform: String = PUSH_PLATFORM

    override fun initializePush() {
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String? ->
            Log.w(TAG, "Found push token")
            try {
                pushToken = if (token != null) Pair(token, "") else null
            } catch (e: Exception) {
                Log.e(TAG, "Can't set push token", e)
            }
        }
    }

    companion object {
        private const val PUSH_PLATFORM = "android"
        private val TAG = JamiApplicationFirebase::class.simpleName
    }
}
