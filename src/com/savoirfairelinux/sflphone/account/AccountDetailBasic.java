/**
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
package com.savoirfairelinux.sflphone.account;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountDetail;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;

public class AccountDetailBasic implements AccountDetail {

    private static final String TAG = "AccountDetailBasic";
    public static final String BUNDLE_TAG = "BasicPreferenceArrayList";

    public static final String CONFIG_ACCOUNT_ENABLE = "Account.enable";
    public static final String CONFIG_ACCOUNT_TYPE = "Account.type";
    public static final String CONFIG_ACCOUNT_ALIAS = "Account.alias";
    public static final String CONFIG_ACCOUNT_HOSTNAME = "Account.hostname";
    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_ROUTESET = "Account.routeset";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";
    public static final String CONFIG_ACCOUNT_DEFAULT_REALM = "*";
    public static final String CONFIG_ACCOUNT_USERAGENT = "Account.useragent";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries()
    {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<AccountDetail.PreferenceEntry>();

        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_ENABLE, R.string.account_enabled_label, true));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_TYPE, R.string.account_type_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_ALIAS, R.string.account_alias_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_HOSTNAME, R.string.account_hostname_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_USERNAME, R.string.account_username_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_ROUTESET, R.string.account_routeset_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_PASSWORD, R.string.account_password_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REALM, R.string.account_realm_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_DEFAULT_REALM, R.string.account_useragent_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_USERAGENT, R.string.account_autoanswer_label));

        return preference;
    }

    public AccountDetailBasic()
    {
        privateArray = getPreferenceEntries();
    }

    public AccountDetailBasic(HashMap<String, String> pref)
    {
        privateArray = getPreferenceEntries();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }
    }

    public AccountDetailBasic(ArrayList<String> pref)
    {
        privateArray = getPreferenceEntries();

        if(pref.size() != privateArray.size()) {
            Log.i(TAG, "Error list are not of equal size");
        }
        else {
            int index = 0; 
            for(String s : pref) {
                privateArray.get(index).mValue = s;
                index++;
            }
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues()
    {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly()
    {
        ArrayList<String> valueList = new ArrayList<String>();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap()
    {
        HashMap<String, String> map = new HashMap<String, String>();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            map.put(p.mKey, p.mValue);
        }

        return map;
    }

    public String getDetailString(String key)
    {
        String value = "";
        
        for(AccountDetail.PreferenceEntry p : privateArray) {
            if(p.mKey.equals(key)) {
                value = p.mValue;
                return value;
            }
        }

        return value;
    }

    public void setDetailString(int position, String newValue)
    {
        privateArray.get(position).mValue = newValue;
    }


    public boolean getDetailBoolean()
    {
        return true;
    }
}
