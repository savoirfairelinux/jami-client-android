/**
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
package org.sflphone.model.account;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

public class AccountDetailAdvanced implements AccountDetail {

    private static final String TAG = "AccountDetailAdvanced";

    public static final String CONFIG_ACCOUNT_MAILBOX = "Account.mailbox";
    public static final String CONFIG_ACCOUNT_REGISTRATION_EXPIRE = "Account.registrationExpire";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATUS = "Account.registrationStatus";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_CODE = "Account.registrationCode";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_DESC = "Account.registrationDescription";
    public static final String CONFIG_CREDENTIAL_NUMBER = "Credential.count";
    public static final String CONFIG_ACCOUNT_DTMF_TYPE = "Account.dtmfType";
    public static final String CONFIG_RINGTONE_PATH = "Account.ringtonePath";
    public static final String CONFIG_RINGTONE_ENABLED = "Account.ringtoneEnabled";
    public static final String CONFIG_KEEP_ALIVE_ENABLED = "Account.keepAliveEnabled";

    public static final String CONFIG_LOCAL_INTERFACE = "Account.localInterface";
    public static final String CONFIG_PUBLISHED_SAMEAS_LOCAL = "Account.publishedSameAsLocal";
    public static final String CONFIG_LOCAL_PORT = "Account.localPort";
    public static final String CONFIG_PUBLISHED_PORT = "Account.publishedPort";
    public static final String CONFIG_PUBLISHED_ADDRESS = "Account.publishedAddress";

    public static final String CONFIG_STUN_SERVER = "STUN.server";
    public static final String CONFIG_STUN_ENABLE = "STUN.enable";

    public static final String CONFIG_AUDIO_PORT_MIN = "Account.audioPortMin";
    public static final String CONFIG_AUDIO_PORT_MAX = "Account.audioPortMax";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public AccountDetailAdvanced(HashMap<String, String> pref) {
        privateArray = new ArrayList<AccountDetail.PreferenceEntry>();

        for (String key : pref.keySet()) {
            PreferenceEntry p = new PreferenceEntry(key);
            p.mValue = pref.get(key);

            if(key.contentEquals(CONFIG_RINGTONE_ENABLED) ||
                    key.contentEquals(CONFIG_KEEP_ALIVE_ENABLED) ||
                    key.contentEquals(CONFIG_PUBLISHED_SAMEAS_LOCAL) ||
                    key.contentEquals(CONFIG_STUN_ENABLE))
                p.isTwoState = true;

            privateArray.add(p);
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<String>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            Log.i(TAG, "" + p.mValue);
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<String, String>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            map.put(p.mKey, p.mValue);
        }

        return map;
    }

    public String getDetailString(String key) {
        String value = "";

        for (AccountDetail.PreferenceEntry p : privateArray) {
            if (p.mKey.equals(key)) {
                value = p.mValue;
                return value;
            }
        }
        return value;
    }

    public void setDetailString(String key, String newValue) {
        for (PreferenceEntry p : privateArray) {
            if (p.mKey.equals(key)) {
                p.mValue = newValue;
            }
        }

    }

}
