/**
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
package cx.ring.model.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class AccountDetailAdvanced implements AccountDetail {

    private static final String TAG = "AccountDetailAdvanced";

    public static final String CONFIG_ACCOUNT_MAILBOX = "Account.mailbox";
    public static final String CONFIG_ACCOUNT_REGISTRATION_EXPIRE = "Account.registrationExpire";
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

    public static final String CONFIG_TURN_ENABLE = "TURN.enable";
    public static final String CONFIG_TURN_SERVER = "TURN.server";
    public static final String CONFIG_TURN_USERNAME = "TURN.username";
    public static final String CONFIG_TURN_PASSWORD = "TURN.password";
    public static final String CONFIG_TURN_REALM = "TURN.realm";

    public static final String CONFIG_AUDIO_PORT_MIN = "Account.audioPortMin";
    public static final String CONFIG_AUDIO_PORT_MAX = "Account.audioPortMax";

    private static final Set<String> CONFIG_KEYS = new HashSet<>(Arrays.asList(
            CONFIG_ACCOUNT_MAILBOX,
            CONFIG_ACCOUNT_REGISTRATION_EXPIRE,
            CONFIG_CREDENTIAL_NUMBER,
            CONFIG_ACCOUNT_DTMF_TYPE,
            CONFIG_RINGTONE_PATH, CONFIG_RINGTONE_ENABLED,
            CONFIG_KEEP_ALIVE_ENABLED,
            CONFIG_LOCAL_INTERFACE, CONFIG_PUBLISHED_SAMEAS_LOCAL, CONFIG_LOCAL_PORT,
            CONFIG_PUBLISHED_PORT, CONFIG_PUBLISHED_ADDRESS,
            CONFIG_STUN_SERVER, CONFIG_STUN_ENABLE,
            CONFIG_TURN_ENABLE, CONFIG_TURN_SERVER, CONFIG_TURN_USERNAME, CONFIG_TURN_PASSWORD, CONFIG_TURN_REALM,
            CONFIG_AUDIO_PORT_MIN, CONFIG_AUDIO_PORT_MAX));

    private static final Set<String> TWO_STATES = new HashSet<>(Arrays.asList(
            CONFIG_RINGTONE_ENABLED,
            CONFIG_KEEP_ALIVE_ENABLED,
            CONFIG_PUBLISHED_SAMEAS_LOCAL,
            CONFIG_STUN_ENABLE, CONFIG_TURN_ENABLE));

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public AccountDetailAdvanced(Map<String, String> pref) {
        privateArray = new ArrayList<>(pref.size());

        for (String key : pref.keySet()) {
            if (!CONFIG_KEYS.contains(key))
                continue;
            PreferenceEntry p = new PreferenceEntry(key, TWO_STATES.contains(key));
            p.mValue = pref.get(key);
            privateArray.add(p);
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            Log.i(TAG, "" + p.mValue);
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<>();

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
