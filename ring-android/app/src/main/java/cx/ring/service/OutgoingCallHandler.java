/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import cx.ring.R;
import cx.ring.client.CallActivity;
import cx.ring.fragments.SettingsFragment;
import cx.ring.model.SipUri;

public class OutgoingCallHandler extends BroadcastReceiver
{
    private static final String TAG = OutgoingCallHandler.class.getSimpleName();
    public static final String KEY_CACHE_HAVE_RINGACCOUNT = "cache_haveRingAccount";
    public static final String KEY_CACHE_HAVE_SIPACCOUNT = "cache_haveSipAccount";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String phoneNumber = getResultData();
        if (phoneNumber == null)
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean systemDialer = sharedPreferences.getBoolean(context.getString(R.string.pref_systemDialer_key), false);
        if (systemDialer) {
            boolean systemDialerSip = sharedPreferences.getBoolean(KEY_CACHE_HAVE_SIPACCOUNT, false);
            boolean systemDialerRing = sharedPreferences.getBoolean(KEY_CACHE_HAVE_RINGACCOUNT, false);

            SipUri sipUri = new SipUri(phoneNumber);
            boolean isRingId = sipUri.isRingId();
            if ((!isRingId && systemDialerSip) || (isRingId && systemDialerRing) || sipUri.isSingleIp()) {
                Intent i = new Intent(CallActivity.ACTION_CALL)
                        .setClass(context, CallActivity.class)
                        .setData(Uri.parse(phoneNumber))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(i);

                setResultData(null);
            }
        }
    }
}
