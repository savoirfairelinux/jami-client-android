/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.savoirfairelinux.sflphone.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ConfigurationManagerCallback extends ConfigurationCallback {
    private static final String TAG = "ConfigurationManagerCallback";
    private Context mContext; 

    public ConfigurationManagerCallback(Context context) {
        mContext = context;
    }

    @Override
    public void on_account_state_changed() {
        Log.i(TAG, "on_account_state_changed ninja!!!!!!!!!!!!!!");
        sendAccountsChangedMessage();
    }

    private void sendAccountsChangedMessage() {
        Log.d("sender", "Boradcasting message");
        Intent intent = new Intent("accounts-changed");
        intent.putExtra("message", "Accounts Changed");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
