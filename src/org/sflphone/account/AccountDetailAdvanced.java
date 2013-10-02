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
package org.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.R;

import android.util.Log;

public class AccountDetailAdvanced implements AccountDetail {

    private static final String TAG = "AccountDetailAdvanced";
    public static final String BUNDLE_TAG = "AdvancedPreferenceArrayList";

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

    public static final String CONFIG_ACCOUNT_AUTOANSWER = "Account.autoAnswer";
    public static final String CONFIG_LOCAL_INTERFACE = "Account.localInterface";
    public static final String CONFIG_PUBLISHED_SAMEAS_LOCAL = "Account.publishedSameAsLocal";
    public static final String CONFIG_LOCAL_PORT = "Account.localPort";
    public static final String CONFIG_PUBLISHED_PORT = "Account.publishedPort";
    public static final String CONFIG_PUBLISHED_ADDRESS = "Account.publishedAddress";
    

    public static final String CONFIG_DEFAULT_LOCAL_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_SAMEAS_LOCAL = "true";

    public static final String CONFIG_DEFAULT_INTERFACE = "default";
    public static final String CONFIG_DEFAULT_REGISTRATION_EXPIRE = "60";

    public static final String CONFIG_DISPLAY_NAME = "Account.displayName";
    public static final String CONFIG_DEFAULT_ADDRESS = "0.0.0.0";

    public static final String CONFIG_STUN_SERVER = "STUN.server";
    public static final String CONFIG_STUN_ENABLE = "STUN.enable";
    
    public static final String CONFIG_DEFAULT_DTMF_TYPE = "overrtp";
    
    public static final String TRUE_STR = "true";
    public static final String FALSE_STR = "false";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries()
    {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<AccountDetail.PreferenceEntry>();

        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_EXPIRE, R.string.account_registration_exp_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATUS, R.string.account_registration_status_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, R.string.account_registration_code_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, R.string.account_registration_state_label));
        preference.add(new PreferenceEntry(CONFIG_CREDENTIAL_NUMBER, R.string.account_credential_count_label));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_DTMF_TYPE, R.string.account_config_dtmf_type_label));
        preference.add(new PreferenceEntry(CONFIG_RINGTONE_PATH, R.string.account_ringtone_path_label));
        preference.add(new PreferenceEntry(CONFIG_RINGTONE_ENABLED, R.string.account_ringtone_enabled_label, true));
        preference.add(new PreferenceEntry(CONFIG_KEEP_ALIVE_ENABLED, R.string.account_keep_alive_label, true));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_AUTOANSWER, R.string.account_autoanswer_label, true));
        preference.add(new PreferenceEntry(CONFIG_LOCAL_INTERFACE, R.string.account_local_interface_label));
        preference.add(new PreferenceEntry(CONFIG_PUBLISHED_SAMEAS_LOCAL, R.string.account_published_same_as_local_label, true));
        preference.add(new PreferenceEntry(CONFIG_LOCAL_PORT, R.string.account_local_port_label));
        preference.add(new PreferenceEntry(CONFIG_PUBLISHED_PORT, R.string.account_published_port_label));
        preference.add(new PreferenceEntry(CONFIG_PUBLISHED_ADDRESS, R.string.account_published_address_label));
        preference.add(new PreferenceEntry(CONFIG_DISPLAY_NAME, R.string.account_displayname_label));
        preference.add(new PreferenceEntry(CONFIG_STUN_SERVER, R.string.account_stun_server_label));
        preference.add(new PreferenceEntry(CONFIG_STUN_ENABLE, R.string.account_stun_enable_label, true));

        return preference;
    }

    public AccountDetailAdvanced()
    {
        privateArray = getPreferenceEntries();
    }

    public AccountDetailAdvanced(HashMap<String, String> pref)
    {
        privateArray = getPreferenceEntries();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }
    }

    public AccountDetailAdvanced(ArrayList<String> pref)
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
            Log.i(TAG,""+p.mValue);
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

    public void setDetailString(String key, String newValue)
    {
        for(int i = 0 ; i < privateArray.size() ; ++i) {
            PreferenceEntry p = privateArray.get(i);
            if(p.mKey.equals(key)) {
                privateArray.get(i).mValue = newValue;
            }
        }
        
    }

}
