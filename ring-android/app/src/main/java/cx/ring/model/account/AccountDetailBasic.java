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

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccountDetailBasic implements AccountDetail {

    private static final String TAG = AccountDetailBasic.class.getSimpleName();

    public static final String CONFIG_ACCOUNT_ALIAS = "Account.alias";
    public static final String CONFIG_ACCOUNT_HOSTNAME = "Account.hostname";
    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";

    public static final String CONFIG_ACCOUNT_USERAGENT = "Account.useragent";
    public static final String CONFIG_ACCOUNT_UPNP_ENABLE = "Account.upnpEnabled";
    public static final String CONFIG_ACCOUNT_ROUTESET = "Account.routeset";
    public static final String CONFIG_ACCOUNT_AUTOANSWER = "Account.autoAnswer";

    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";
    public static final String CONFIG_ACCOUNT_TYPE = "Account.type";
    public static final String CONFIG_ACCOUNT_ENABLE = "Account.enable";
    public static final String CONFIG_VIDEO_ENABLED = "Account.videoEnabled";

    public static final String CONFIG_PRESENCE_ENABLE = "Account.presenceEnabled";

    public static final String ACCOUNT_TYPE_RING = "RING";
    public static final String ACCOUNT_TYPE_SIP = "SIP";

    private static final Set<String> TWO_STATES = new HashSet<>(Arrays.asList(
            CONFIG_ACCOUNT_ENABLE,
            CONFIG_ACCOUNT_AUTOANSWER,
            CONFIG_ACCOUNT_UPNP_ENABLE,
            CONFIG_VIDEO_ENABLED));

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public String getAlias() {
        return getDetailString(CONFIG_ACCOUNT_ALIAS);
    }

    public String getUsername() {
        return getDetailString(CONFIG_ACCOUNT_USERNAME);
    }

    public String getHostname() {
        return getDetailString(CONFIG_ACCOUNT_HOSTNAME);
    }

    public AccountDetailBasic(Map<String, String> pref) {
        privateArray = new ArrayList<>(pref.size());

        for (String key : pref.keySet()) {
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
                Log.w(TAG, "setDetailString " + key + " -> " + newValue);
                return;
            }
        }
        Log.w(TAG, "setDetailString FAIL" + key + " -> " + newValue);
    }

}
