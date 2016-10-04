/**
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 * <p>
 * Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * If you own a pjsip commercial license you can also redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as an android library.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.model.account;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AccountDetailVolatile implements AccountDetail {

    private static final String TAG = "AccountDetailVolatile";

    public static final String CONFIG_ACCOUNT_REGISTRATION_STATUS = "Account.registrationStatus";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_CODE = "Account.registrationCode";
    public static final String CONFIG_ACCOUNT_REGISTRATION_STATE_DESC = "Account.registrationDescription";

    public static final String STATE_REGISTERED = "REGISTERED";
    public static final String STATE_READY = "READY";
    public static final String STATE_UNREGISTERED = "UNREGISTERED";
    public static final String STATE_TRYING = "TRYING";
    public static final String STATE_ERROR = "ERROR";
    public static final String STATE_ERROR_GENERIC = "ERROR_GENERIC";
    public static final String STATE_ERROR_AUTH = "ERROR_AUTH";
    public static final String STATE_ERROR_NETWORK = "ERROR_NETWORK";
    public static final String STATE_ERROR_HOST = "ERROR_HOST";
    public static final String STATE_ERROR_CONF_STUN = "ERROR_CONF_STUN";
    public static final String STATE_ERROR_EXIST_STUN = "ERROR_EXIST_STUN";
    public static final String STATE_ERROR_SERVICE_UNAVAILABLE = "ERROR_SERVICE_UNAVAILABLE";
    public static final String STATE_ERROR_NOT_ACCEPTABLE = "ERROR_NOT_ACCEPTABLE";
    public static final String STATE_REQUEST_TIMEOUT = "Request Timeout";
    public static final String STATE_INITIALIZING = "INITIALIZING";
    public static final String STATE_NEED_MIGRATION = "ERROR_NEED_MIGRATION";

    private final ArrayList<PreferenceEntry> privateArray = new ArrayList<>();

    public AccountDetailVolatile() {
    }

    public AccountDetailVolatile(Map<String, String> pref) {
        for (String key : pref.keySet()) {
            PreferenceEntry preferenceEntry = new PreferenceEntry(key);
            preferenceEntry.mValue = pref.get(key);

            privateArray.add(preferenceEntry);
        }
    }

    public ArrayList<PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<>();

        for (PreferenceEntry preferenceEntry : privateArray) {
            Log.d(TAG, "" + preferenceEntry.mValue);
            valueList.add(preferenceEntry.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<>();

        for (PreferenceEntry preferenceEntry : privateArray) {
            map.put(preferenceEntry.mKey, preferenceEntry.mValue);
        }

        return map;
    }

    public String getDetailString(String key) {
        String value = "";

        for (PreferenceEntry preferenceEntry : privateArray) {
            if (preferenceEntry.mKey.equals(key)) {
                value = preferenceEntry.mValue;
                return value;
            }
        }
        return value;
    }

    public void setDetailString(String key, String newValue) {
        for (PreferenceEntry preferenceEntry : privateArray) {
            if (preferenceEntry.mKey.equals(key)) {
                preferenceEntry.mValue = newValue;
            }
        }

    }

}
