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

import android.content.Context
import android.net.NetworkInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    /**
     * Get the network info
     */
    fun getNetworkInfo(context: Context): NetworkInfo? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (n in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(n)
                if (caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                val nInfo = cm.getNetworkInfo(n)
                if (nInfo != null && nInfo.isConnected)
                    return nInfo
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting network info")
        }
        return null
    }

    fun isConnectivityAllowed(context: Context): Boolean {
        val info = getNetworkInfo(context)
        return info != null && info.isConnected
    }

    fun isPushAllowed(context: Context, allowMobile: Boolean): Boolean {
        if (allowMobile) return true
        val info = getNetworkInfo(context)
        return info != null && info.type != ConnectivityManager.TYPE_MOBILE
    }

    const val TAG = "NetworkUtils"
}