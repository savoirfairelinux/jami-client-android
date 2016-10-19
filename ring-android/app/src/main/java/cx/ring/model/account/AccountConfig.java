/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccountConfig {

    private static final String TAG = AccountConfig.class.getSimpleName();

    public static final String TRUE_STR = "true";
    public static final String FALSE_STR = "false";
    public static final String ACCOUNT_TYPE_RING = "RING";
    public static final String ACCOUNT_TYPE_SIP = "SIP";

    public static final String STATE_REGISTERED                 = "REGISTERED";
    public static final String STATE_READY                      = "READY";
    public static final String STATE_UNREGISTERED               = "UNREGISTERED";
    public static final String STATE_TRYING                     = "TRYING";
    public static final String STATE_ERROR                      = "ERROR";
    public static final String STATE_ERROR_GENERIC              = "ERROR_GENERIC";
    public static final String STATE_ERROR_AUTH                 = "ERROR_AUTH";
    public static final String STATE_ERROR_NETWORK              = "ERROR_NETWORK";
    public static final String STATE_ERROR_HOST                 = "ERROR_HOST";
    public static final String STATE_ERROR_CONF_STUN            = "ERROR_CONF_STUN";
    public static final String STATE_ERROR_EXIST_STUN           = "ERROR_EXIST_STUN";
    public static final String STATE_ERROR_SERVICE_UNAVAILABLE  = "ERROR_SERVICE_UNAVAILABLE";
    public static final String STATE_ERROR_NOT_ACCEPTABLE       = "ERROR_NOT_ACCEPTABLE";
    public static final String STATE_REQUEST_TIMEOUT            = "Request Timeout";
    public static final String STATE_INITIALIZING               = "INITIALIZING";
    public static final String STATE_NEED_MIGRATION             = "ERROR_NEED_MIGRATION";

    private final Map<ConfigKey, String> vals;

    public AccountConfig() {
        vals = new HashMap<>();
    }

    public AccountConfig(Map<String, String> v) {
        if (v != null) {
            vals = new HashMap<>(v.size());
            for (Map.Entry<String, String> e : v.entrySet()) {
                ConfigKey k = ConfigKey.fromString(e.getKey());
                if (k == null)
                    Log.w(TAG, "Can't find key: " + e.getKey());
                else
                    vals.put(k, e.getValue());
            }
        } else {
            vals = new HashMap<>();
        }
    }

    public String get(ConfigKey key) {
        return vals.get(key);
    }

    public boolean getBool(ConfigKey key) {
        return TRUE_STR.equals(get(key));
    }

    public HashMap<String, String> getAll() {
        HashMap<String, String> details = new HashMap<>(vals.size());
        for (Map.Entry<ConfigKey, String> e : vals.entrySet()) {
            details.put(e.getKey().key(), e.getValue());
        }
        return details;
    }

    void put(ConfigKey key, String value) {
        vals.put(key, value);
    }

    void put(ConfigKey key, boolean v) {
        vals.put(key, v ? TRUE_STR : FALSE_STR);
    }

    public Set<ConfigKey> getKeys() {
        return vals.keySet();
    }

    public Set<Map.Entry<ConfigKey, String>> getEntries() {
        return vals.entrySet();
    }

}
