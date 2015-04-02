/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
package org.sflphone.service;

import android.content.Intent;
import android.util.Log;

public class ConfigurationManagerCallback extends ConfigurationCallback {

    private  SipService mService;
    private static final String TAG = "ConfigurationManagerCallback";

    static public final String ACCOUNTS_CHANGED = "accounts-changed";
    static public final String ACCOUNT_STATE_CHANGED = "account-state-changed";

    public ConfigurationManagerCallback(SipService context) {
        super();
        mService = context;
    }

    @Override
    public void configOnVolumeChange(String device, int value) {
        super.configOnVolumeChange(device, value);
    }

    @Override
    public void configOnAccountsChange() {
        super.configOnAccountsChange();
        Intent intent = new Intent(ACCOUNTS_CHANGED);
        mService.sendBroadcast(intent);
    }

    @Override
    public void configOnStunStatusFail(String account_id) {
        Log.d(TAG, "configOnStunStatusFail : (" + account_id);
    }

    @Override
    public void configOnRegistrationStateChange(String accoundID, int state) {
        String strState = "";
        switch (state){
        case 0:
            strState = "UNREGISTERED";
            break;
        case 1:
            strState = "TRYING";
            break;
        case 2:
            strState = "REGISTERED";
            break;
        case 3:
            strState = "ERROR_GENERIC";
            break;
        case 4:
            strState = "ERROR_AUTH";
            break;
        case 5:
            strState = "ERROR_NETWORK";
            break;
        case 6:
            strState = "ERROR_HOST";
            break;
        case 7:
            strState = "ERROR_EXIST_STUN";
            break;
        case 8:
            strState = "ERROR_NOT_ACCEPTABLE";
            break;
        case 9:
            strState = "NUMBER_OF_STATES";
            break;
        }

        sendAccountStateChangedMessage(accoundID, strState, 0);
    }

    @Override
    public void configOnSipRegistrationStateChange(String account_id, String state, int code) {

    }

    @Override
    public void configOnError(int alert) {
        Log.d(TAG, "configOnError : (" + alert);
    }

    private void sendAccountStateChangedMessage(String accoundID, String state, int code) {
        Intent intent = new Intent(ACCOUNT_STATE_CHANGED);
        intent.putExtra("Account", accoundID);
        intent.putExtra("state", state);
        intent.putExtra("code", code);
        mService.sendBroadcast(intent);
    }

    public IntVect configGetHardwareAudioFormat(){
        IntVect result = new IntVect();

        OpenSlParams audioParams = OpenSlParams.createInstance(mService);
        result.add(audioParams.getSampleRate());
        result.add(audioParams.getBufferSize());

        return result;
    }

}
