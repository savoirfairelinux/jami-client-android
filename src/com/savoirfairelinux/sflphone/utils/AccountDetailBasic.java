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

public class AccountDetailBasic implements AccountDetail {

    public static final String CONFIG_ACCOUNT_TYPE = "Account.type";
    public static final String CONFIG_ACCOUNT_ALIAS = "Account.alias";
    public static final String CONFIG_ACCOUNT_ENABLE = "Account.enable";
    public static final String CONFIG_ACCOUNT_HOSTNAME = "Account.hostname";
    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_ROUTESET = "Account.routeset";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";
    public static final String CONFIG_ACCOUNT_DEFAULT_REALM = "*";
    public static final String CONFIG_ACCOUNT_USERAGENT = "Account.useragent";

    private HashMap<String, AccountDetail.PreferenceEntry> privateMap;

    public AccountDetailBasic()
    {
        privateMap = new HashMap<String, AccountDetail.PreferenceEntry>();

        privateMap.put(CONFIG_ACCOUNT_TYPE,
                       new PreferenceEntry(CONFIG_ACCOUNT_TYPE, R.string.account_type_label));
        privateMap.put(CONFIG_ACCOUNT_ALIAS,
                       new PreferenceEntry(CONFIG_ACCOUNT_ALIAS, R.string.account_alias_label));
        privateMap.put(CONFIG_ACCOUNT_ENABLE,
                       new PreferenceEntry(CONFIG_ACCOUNT_ENABLE, R.string.account_enabled_label));
        privateMap.put(CONFIG_ACCOUNT_HOSTNAME,
                       new PreferenceEntry(CONFIG_ACCOUNT_HOSTNAME, R.string.account_hostname_label));
        privateMap.put(CONFIG_ACCOUNT_USERNAME,
                       new PreferenceEntry(CONFIG_ACCOUNT_USERNAME, R.string.account_username_label));
        privateMap.put(CONFIG_ACCOUNT_ROUTESET,
                       new PreferenceEntry(CONFIG_ACCOUNT_ROUTESET, R.string.account_routeset_label));
        privateMap.put(CONFIG_ACCOUNT_PASSWORD,
                       new PreferenceEntry(CONFIG_ACCOUNT_PASSWORD, R.string.account_password_label));
        privateMap.put(CONFIG_ACCOUNT_REALM,
                       new PreferenceEntry(CONFIG_ACCOUNT_REALM, R.string.account_realm_label));
        privateMap.put(CONFIG_ACCOUNT_DEFAULT_REALM,
                       new PreferenceEntry(CONFIG_ACCOUNT_DEFAULT_REALM, R.string.account_useragent_label));
        privateMap.put(CONFIG_ACCOUNT_USERAGENT,
                       new PreferenceEntry(CONFIG_ACCOUNT_USERAGENT, R.string.account_autoanswer_label));
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
