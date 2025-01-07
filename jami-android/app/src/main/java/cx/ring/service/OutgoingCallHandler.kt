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
package cx.ring.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import cx.ring.R
import cx.ring.client.CallActivity

class OutgoingCallHandler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_NEW_OUTGOING_CALL != intent.action) return
        var phoneNumber = resultData
        if (phoneNumber == null) phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val systemDialer = sharedPreferences.getBoolean(context.getString(R.string.pref_systemDialer_key), false)
        if (systemDialer) {
            val systemDialerSip = sharedPreferences.getBoolean(KEY_CACHE_HAVE_SIPACCOUNT, false)
            val systemDialerRing = sharedPreferences.getBoolean(KEY_CACHE_HAVE_RINGACCOUNT, false)
            val uri = net.jami.model.Uri.fromString(phoneNumber!!)
            val isRingId = uri.isHexId
            if (!isRingId && systemDialerSip || isRingId && systemDialerRing || uri.isSingleIp) {
                val i = Intent(Intent.ACTION_CALL)
                    .setClass(context, CallActivity::class.java)
                    .setData(Uri.parse(phoneNumber))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                resultData = null
            }
        }
    }

    companion object {
        const val KEY_CACHE_HAVE_RINGACCOUNT = "cache_haveRingAccount"
        const val KEY_CACHE_HAVE_SIPACCOUNT = "cache_haveSipAccount"
        private val TAG = OutgoingCallHandler::class.java.simpleName
    }
}