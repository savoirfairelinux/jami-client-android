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
import java.util.HashMap;
import java.util.Map;

public class AccountDetailTls implements AccountDetail {

    private static final String TAG = "AccountDetailTls";

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

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries() {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<>();

        preference.add(new PreferenceEntry(CONFIG_TLS_LISTENER_PORT));
        preference.add(new PreferenceEntry(CONFIG_TLS_ENABLE, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_CA_LIST_FILE));
        preference.add(new PreferenceEntry(CONFIG_TLS_CERTIFICATE_FILE));
        preference.add(new PreferenceEntry(CONFIG_TLS_PRIVATE_KEY_FILE));
        preference.add(new PreferenceEntry(CONFIG_TLS_PASSWORD));
        preference.add(new PreferenceEntry(CONFIG_TLS_METHOD));
        preference.add(new PreferenceEntry(CONFIG_TLS_CIPHERS));
        preference.add(new PreferenceEntry(CONFIG_TLS_SERVER_NAME));
        preference.add(new PreferenceEntry(CONFIG_TLS_VERIFY_SERVER));
        preference.add(new PreferenceEntry(CONFIG_TLS_VERIFY_CLIENT, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, true));
        preference.add(new PreferenceEntry(CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));

        return preference;
    }

    public AccountDetailTls(Map<String, String> pref) {
        privateArray = getPreferenceEntries();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<>();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<>();

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
                return p.mValue.contentEquals(AccountDetail.TRUE_STR);
            }
        }
        return false;
    }
}
