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

public class AccountDetailTls implements AccountDetail {

    private static final String TAG = "AccountDetailTls";
    public static final String BUNDLE_TAG = "TlsPreferenceArrayList";

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

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries() {
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

    public AccountDetailTls() {
        privateArray = getPreferenceEntries();
    }

    public AccountDetailTls(HashMap<String, String> pref) {
        privateArray = getPreferenceEntries();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }
    }

    public AccountDetailTls(ArrayList<String> pref) {
        privateArray = getPreferenceEntries();

        if (pref.size() != privateArray.size()) {
            Log.i(TAG, "Error list are not of equal size");
        } else {
            int index = 0;
            for (String s : pref) {
                privateArray.get(index).mValue = s;
                index++;
            }
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<String>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<String, String>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            if (p.mValue == null) {
                map.put(p.mKey, "");
            } else {
                map.put(p.mKey, p.mValue);
            }
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

    public boolean getDetailBoolean(String key) {
        for (AccountDetail.PreferenceEntry p : privateArray) {
            if (p.mKey.equals(key)) {
                return p.mValue.contentEquals("true");
            }
        }
        return false;
    }
}
