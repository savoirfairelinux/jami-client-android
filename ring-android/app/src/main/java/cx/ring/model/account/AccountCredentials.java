/**
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class AccountCredentials implements Parcelable {

    @SuppressWarnings("unused")
    private static final String TAG = AccountCredentials.class.getSimpleName();

    private static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    private static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    private static final String CONFIG_ACCOUNT_REALM = "Account.realm";

    private String username;
    private String password;
    private String realm;

    public static final Creator<AccountCredentials> CREATOR = new Creator<AccountCredentials>() {
        @Override
        public AccountCredentials createFromParcel(Parcel in) {
            return new AccountCredentials(in);
        }

        @Override
        public AccountCredentials[] newArray(int size) {
            return new AccountCredentials[size];
        }
    };

    public AccountCredentials(Map<String, String> pref) {
        username = pref.get(CONFIG_ACCOUNT_USERNAME);
        password = pref.get(CONFIG_ACCOUNT_PASSWORD);
        realm = pref.get(CONFIG_ACCOUNT_REALM);
    }
    public AccountCredentials(String username, String password, String realm) {
        setUsername(username);
        setPassword(password);
        setRealm(realm);
    }
    private AccountCredentials(Parcel in) {
        username = in.readString();
        password = in.readString();
        realm = in.readString();
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(username);
        out.writeString(password);
        out.writeString(realm);
    }

    public void setUsername(String val) {
        username = val;
    }
    public void setPassword(String val) {
        password = val;
    }

    public void setRealm(String val) {
        realm = val;
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getRealm() {
        return realm;
    }

    public HashMap<String,String> getDetails() {
        HashMap<String,String> d = new HashMap<>();
        d.put(ConfigKey.ACCOUNT_USERNAME.key(), username);
        d.put(ConfigKey.ACCOUNT_PASSWORD.key(), password);
        d.put(ConfigKey.ACCOUNT_REALM.key(), realm);
        return d;
    }

    public void setDetail(ConfigKey k, String value) {
        if (k == ConfigKey.ACCOUNT_USERNAME)
            username = value;
        else if (k == ConfigKey.ACCOUNT_PASSWORD)
            password = value;
        else if (k == ConfigKey.ACCOUNT_REALM)
            realm = value;
    }

}
