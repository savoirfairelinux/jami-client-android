/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.client;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.EditTextPreference;
import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ISipService;

public class AccountCreationActivity extends PreferenceActivity
{
    static final String TAG = "SFLPhonePreferenceActivity";
    private ISipService service;
    private boolean mBound = false;
    private PreferenceManager mPreferenceManager = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            mBound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.account_creation_preferences);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mPreferenceManager = getPreferenceManager();

        EditTextPreference accountAliasPref = (EditTextPreference) mPreferenceManager.findPreference("AccountAlias");
        EditTextPreference accountUsername = (EditTextPreference) mPreferenceManager.findPreference("AccountUserName");
        EditTextPreference accountHostname = (EditTextPreference) mPreferenceManager.findPreference("AccountHostname");
        EditTextPreference accountPassword = (EditTextPreference) mPreferenceManager.findPreference("AccountPassword");
        EditTextPreference accountRoutset = (EditTextPreference) mPreferenceManager.findPreference("AccountRealm");
        EditTextPreference accountUseragent = (EditTextPreference) mPreferenceManager.findPreference("AccountUserAgent");
        EditTextPreference accountAutoAnswer = (EditTextPreference) mPreferenceManager.findPreference("AccountAutoAnswer");
    }
}
