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
package com.savoirfairelinux.sflphone.utils;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.utils.AccountDetail;

import java.util.Collection;
import java.util.Set;
import java.util.HashMap;

public class AccountDetailAdvanced implements AccountDetail {

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
    public static final String CONFIG_INTERFACE = "Account.interface";
    public static final String CONFIG_PUBLISHED_SAMEAS_LOCAL = "Account.publishedSameAsLocal";
    public static final String CONFIG_LOCAL_PORT = "Account.localPort";
    public static final String CONFIG_PUBLISHED_PORT = "Account.publishedPort";
    public static final String CONFIG_PUBLISHED_ADDRESS = "Account.publishedAddress";
    public static final String CONFIG_DEFAULT_LOCAL_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_PORT = "5060";
    public static final String CONFIG_DEFAULT_PUBLISHED_SAMEAS_LOCAL = "true";
    public static final String CONFIG_DEFAULT_INTERFACE = "default";

    public static final String CONFIG_DISPLAY_NAME = "Account.displayName";
    public static final String CONFIG_DEFAULT_ADDRESS = "0.0.0.0";

    public static final String CONFIG_STUN_SERVER = "STUN.server";
    public static final String CONFIG_STUN_ENABLE = "STUN.enable";

    private HashMap<String, AccountDetail.PreferenceEntry> privateMap;

    public AccountDetailAdvanced()
    {
        privateMap = new HashMap<String, AccountDetail.PreferenceEntry>();

        privateMap.put(CONFIG_ACCOUNT_REGISTRATION_EXPIRE,
                       new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_EXPIRE, R.string.account_registration_exp_label));
        privateMap.put(CONFIG_ACCOUNT_REGISTRATION_STATUS,
                       new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATUS, R.string.account_registration_status_label));
        privateMap.put(CONFIG_ACCOUNT_REGISTRATION_STATE_CODE,
                       new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, R.string.account_registration_code_label));
        privateMap.put(CONFIG_ACCOUNT_REGISTRATION_STATE_DESC,
                       new PreferenceEntry(CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, R.string.account_registration_state_label));
        privateMap.put(CONFIG_CREDENTIAL_NUMBER,
                       new PreferenceEntry(CONFIG_CREDENTIAL_NUMBER, R.string.account_credential_count_label));
        privateMap.put(CONFIG_ACCOUNT_DTMF_TYPE,
                       new PreferenceEntry(CONFIG_ACCOUNT_DTMF_TYPE, R.string.account_config_dtmf_type_label));
        privateMap.put(CONFIG_RINGTONE_PATH,
                       new PreferenceEntry(CONFIG_RINGTONE_PATH, R.string.account_ringtone_path_label));
        privateMap.put(CONFIG_RINGTONE_ENABLED,
                       new PreferenceEntry(CONFIG_RINGTONE_ENABLED, R.string.account_ringtone_enabled_label, true));
        privateMap.put(CONFIG_KEEP_ALIVE_ENABLED,
                       new PreferenceEntry(CONFIG_KEEP_ALIVE_ENABLED, R.string.account_keep_alive_label, true));
        privateMap.put(CONFIG_ACCOUNT_AUTOANSWER,
                       new PreferenceEntry(CONFIG_ACCOUNT_AUTOANSWER, R.string.account_account_interface_label, true));
        privateMap.put(CONFIG_LOCAL_INTERFACE,
                       new PreferenceEntry(CONFIG_LOCAL_INTERFACE, R.string.account_local_interface_label));
        privateMap.put(CONFIG_INTERFACE,
                       new PreferenceEntry(CONFIG_INTERFACE, R.string.account_account_interface_label));
        privateMap.put(CONFIG_PUBLISHED_SAMEAS_LOCAL,
                       new PreferenceEntry(CONFIG_PUBLISHED_SAMEAS_LOCAL, R.string.account_published_same_as_local_label, true));
        privateMap.put(CONFIG_LOCAL_PORT,
                       new PreferenceEntry(CONFIG_LOCAL_PORT, R.string.account_local_port_label));
        privateMap.put(CONFIG_PUBLISHED_PORT,
                       new PreferenceEntry(CONFIG_PUBLISHED_PORT, R.string.account_published_port_label));
        privateMap.put(CONFIG_PUBLISHED_ADDRESS,
                       new PreferenceEntry(CONFIG_PUBLISHED_ADDRESS, R.string.account_published_address_label));
        privateMap.put(CONFIG_DISPLAY_NAME,
                       new PreferenceEntry(CONFIG_DISPLAY_NAME, R.string.account_displayname_label));
        privateMap.put(CONFIG_STUN_SERVER,
                       new PreferenceEntry(CONFIG_STUN_SERVER, R.string.account_stun_server_label));
        privateMap.put(CONFIG_STUN_ENABLE,
                       new PreferenceEntry(CONFIG_STUN_ENABLE, R.string.account_stun_enable_label, true));
    }

    public Set<String> getDetailKeys()
    {
        return privateMap.keySet();
    }

    public Collection<AccountDetail.PreferenceEntry> getDetailValues()
    {
        return privateMap.values();
    }
}
