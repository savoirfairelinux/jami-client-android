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

    private static final String CONFIG_ACCOUNT_USERNAME = "Account.mUsername";
    private static final String CONFIG_ACCOUNT_PASSWORD = "Account.mPassword";
    private static final String CONFIG_ACCOUNT_REALM = "Account.mRealm";

    private String mUsername;
    private String mPassword;
    private String mRealm;

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
        mUsername = pref.get(CONFIG_ACCOUNT_USERNAME);
        mPassword = pref.get(CONFIG_ACCOUNT_PASSWORD);
        mRealm = pref.get(CONFIG_ACCOUNT_REALM);
    }
    public AccountCredentials(String username, String password, String realm) {
        setUsername(username);
        setPassword(password);
        setRealm(realm);
    }
    private AccountCredentials(Parcel in) {
        mUsername = in.readString();
        mPassword = in.readString();
        mRealm = in.readString();
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUsername);
        out.writeString(mPassword);
        out.writeString(mRealm);
    }

    public void setUsername(String val) {
        mUsername = val;
    }
    public void setPassword(String val) {
        mPassword = val;
    }

    public void setRealm(String val) {
        mRealm = val;
    }

    public String getUsername() {
        return mUsername;
    }
    public String getPassword() {
        return mPassword;
    }
    public String getRealm() {
        return mRealm;
    }

    public HashMap<String,String> getDetails() {
        HashMap<String,String> details = new HashMap<>();
        details.put(ConfigKey.ACCOUNT_USERNAME.key(), mUsername);
        details.put(ConfigKey.ACCOUNT_PASSWORD.key(), mPassword);
        details.put(ConfigKey.ACCOUNT_REALM.key(), mRealm);
        return details;
    }

    public void setDetail(ConfigKey key, String value) {
        if (key == ConfigKey.ACCOUNT_USERNAME)
            mUsername = value;
        else if (key == ConfigKey.ACCOUNT_PASSWORD)
            mPassword = value;
        else if (key == ConfigKey.ACCOUNT_REALM)
            mRealm = value;
    }

}
