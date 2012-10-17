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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;

public class AccountDetailTls implements AccountDetail {
 
    public static final String CONFIG_TLS_LISTENER_PORT = "TLS.listenerPort";
    public static final String CONFIG_TLS_ENABLE = "TLS.enable";
    public static final String CONFIG_TLS_CA_LIST_FILE = "TLS.certificateListFile";
    public static final String CONFIG_TLS_CERTIFICATE_FILE = "TLS.certificateFile";
    public static final String CONFIG_TLS_PRIVATE_KEY_FILE = "TLS.privateKeyFile";
    public static final String CONFIG_TLS_PASSWORD = "TLS.password";
    public static final String CONFIG_TLS_METHOD = "TLS.method";
    public static final String CONFIG_TLS_CIPHERS = "TLS.ciphers";
    public static final String CONFIG_TLS_SERVER_NAME = "TLS.serverName";
    public static final String CONFIG_TLS_VERIFY_SERVER = "TLS.verifyServer";
    public static final String CONFIG_TLS_VERIFY_CLIENT = "TLS.verifyClient";
    public static final String CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE = "TLS.requireClientCertificate";
    public static final String CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC = "TLS.negotiationTimeoutSec";
    public static final String CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC = "TLS.negotiationTimemoutMsec";

    private HashMap<String, AccountDetail.PreferenceEntry> privateMap;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries()
    {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<AccountDetail.PreferenceEntry>();

        preference.add(new PreferenceEntry(CONFIG_TLS_LISTENER_PORT, R.string.account_listener_port_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_ENABLE, R.string.account_tls_enabled_label, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_CA_LIST_FILE, R.string.account_tls_certificate_list_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_CERTIFICATE_FILE, R.string.account_tls_certificate_file_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_PRIVATE_KEY_FILE, R.string.account_tls_private_key_file_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_PASSWORD, R.string.account_tls_password_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_METHOD, R.string.account_tls_method_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_CIPHERS, R.string.account_tls_ciphers_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_SERVER_NAME, R.string.account_tls_server_name_label));
        preference.add(new PreferenceEntry(CONFIG_TLS_VERIFY_SERVER, R.string.account_tls_verify_label, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_VERIFY_CLIENT, R.string.account_tls_verify_client_label, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, R.string.account_tls_require_client_certificat_label, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, R.string.account_tls_negotiation_timeout_sec));
        preference.add(new PreferenceEntry(CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, R.string.account_tls_negotiation_timeout_msec));

        return preference;
    }

    public AccountDetailTls()
    {
        privateMap = new HashMap<String, AccountDetail.PreferenceEntry>();

        privateMap.put(CONFIG_TLS_LISTENER_PORT,
                       new PreferenceEntry(CONFIG_TLS_LISTENER_PORT, R.string.account_listener_port_label));
        privateMap.put(CONFIG_TLS_ENABLE,
                       new PreferenceEntry(CONFIG_TLS_ENABLE, R.string.account_tls_enabled_label, true));
        privateMap.put(CONFIG_TLS_CA_LIST_FILE,
                       new PreferenceEntry(CONFIG_TLS_CA_LIST_FILE, R.string.account_tls_certificate_list_label));
        privateMap.put(CONFIG_TLS_CERTIFICATE_FILE,
                       new PreferenceEntry(CONFIG_TLS_CERTIFICATE_FILE, R.string.account_tls_certificate_file_label));
        privateMap.put(CONFIG_TLS_PRIVATE_KEY_FILE,
                       new PreferenceEntry(CONFIG_TLS_PRIVATE_KEY_FILE, R.string.account_tls_private_key_file_label));
        privateMap.put(CONFIG_TLS_PASSWORD,
                       new PreferenceEntry(CONFIG_TLS_PASSWORD, R.string.account_tls_password_label));
        privateMap.put(CONFIG_TLS_METHOD,
                       new PreferenceEntry(CONFIG_TLS_METHOD, R.string.account_tls_method_label));
        privateMap.put(CONFIG_TLS_CIPHERS,
                       new PreferenceEntry(CONFIG_TLS_CIPHERS, R.string.account_tls_ciphers_label));
        privateMap.put(CONFIG_TLS_SERVER_NAME,
                       new PreferenceEntry(CONFIG_TLS_SERVER_NAME, R.string.account_tls_server_name_label));
        privateMap.put(CONFIG_TLS_VERIFY_SERVER,
                       new PreferenceEntry(CONFIG_TLS_VERIFY_SERVER, R.string.account_tls_verify_label, true));
        privateMap.put(CONFIG_TLS_VERIFY_CLIENT,
                       new PreferenceEntry(CONFIG_TLS_VERIFY_CLIENT, R.string.account_tls_verify_client_label, true));
        privateMap.put(CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE,
                       new PreferenceEntry(CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, R.string.account_tls_require_client_certificat_label, true));
        privateMap.put(CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC,
                       new PreferenceEntry(CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, R.string.account_tls_negotiation_timeout_sec));
        privateMap.put(CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC,
                       new PreferenceEntry(CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, R.string.account_tls_negotiation_timeout_msec));
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
