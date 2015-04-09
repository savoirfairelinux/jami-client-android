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
package cx.ring.model.account;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

public class AccountDetailBasic implements AccountDetail {

    private static final String TAG = AccountDetailBasic.class.getSimpleName();

    public static final String CONFIG_ACCOUNT_ALIAS = "Account.alias";
    public static final String CONFIG_ACCOUNT_HOSTNAME = "Account.hostname";
    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";

    public static final String CONFIG_ACCOUNT_USERAGENT = "Account.useragent";
    public static final String CONFIG_ACCOUNT_ROUTESET = "Account.routeset";
    public static final String CONFIG_ACCOUNT_AUTOANSWER = "Account.autoAnswer";

    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";
    public static final String CONFIG_ACCOUNT_TYPE = "Account.type";
    public static final String CONFIG_ACCOUNT_ENABLE = "Account.enable";
    public static final String CONFIG_PRESENCE_ENABLE = "Account.presenceEnabled";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public AccountDetailBasic(HashMap<String, String> pref) {
        privateArray = new ArrayList<AccountDetail.PreferenceEntry>();

        for (String key : pref.keySet()) {
            PreferenceEntry p = new PreferenceEntry(key);
            p.mValue = pref.get(key);

            if(key.contentEquals(CONFIG_ACCOUNT_ENABLE) || key.contentEquals(CONFIG_ACCOUNT_AUTOANSWER))
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
        for (int i = 0; i < privateArray.size(); ++i) {
            PreferenceEntry p = privateArray.get(i);
            if (p.mKey.equals(key)) {
                privateArray.get(i).mValue = newValue;
            }
        }

    }

}
