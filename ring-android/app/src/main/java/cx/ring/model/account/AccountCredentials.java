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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AccountCredentials implements AccountDetail, Parcelable {

    @SuppressWarnings("unused")
    private static final String TAG = AccountCredentials.class.getSimpleName();

    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";

    private final ArrayList<AccountDetail.PreferenceEntry> privateArray = getPreferenceEntries();

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

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries() {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<>(3);
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_USERNAME));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_PASSWORD));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REALM));
        return preference;
    }

    public AccountCredentials(Map<String, String> pref) {
        for (AccountDetail.PreferenceEntry p : privateArray)
            p.mValue = pref.get(p.mKey);
    }
    public AccountCredentials(String username, String password, String realm) {
        setUsername(username);
        setPassword(password);
        setRealm(realm);
    }
    private AccountCredentials(Parcel in) {
        for (AccountDetail.PreferenceEntry p : privateArray)
            p.mValue = in.readString();
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        for (AccountDetail.PreferenceEntry p : privateArray)
            out.writeString(p.mValue);
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues() {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly() {
        ArrayList<String> valueList = new ArrayList<>(privateArray.size());
        for (AccountDetail.PreferenceEntry p : privateArray)
            valueList.add(p.mValue);
        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap() {
        HashMap<String, String> map = new HashMap<>(privateArray.size());
        for (AccountDetail.PreferenceEntry p : privateArray)
            map.put(p.mKey, p.mValue);
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
            if (p.mKey.equals(key))
                p.mValue = newValue;
        }
    }

    public void setUsername(String val) {
        setDetailString(CONFIG_ACCOUNT_USERNAME, val);
    }
    public void setPassword(String val) {
        setDetailString(CONFIG_ACCOUNT_PASSWORD, val);
    }
    public void setRealm(String val) {
        setDetailString(CONFIG_ACCOUNT_REALM, val);
    }

    public String getUsername() {
        return getDetailString(CONFIG_ACCOUNT_USERNAME);
    }
    public String getPassword() {
        return getDetailString(CONFIG_ACCOUNT_PASSWORD);
    }
    public String getRealm() {
        return getDetailString(CONFIG_ACCOUNT_REALM);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AccountCredentials)
            return ((AccountCredentials) other).getDetailsHashMap().get(CONFIG_ACCOUNT_USERNAME)
                    .contentEquals(getDetailString(CONFIG_ACCOUNT_USERNAME))
                    && ((AccountCredentials) other).getDetailsHashMap().get(CONFIG_ACCOUNT_PASSWORD)
                            .contentEquals(getDetailString(CONFIG_ACCOUNT_PASSWORD))
                    && ((AccountCredentials) other).getDetailsHashMap().get(CONFIG_ACCOUNT_REALM)
                            .contentEquals(getDetailString(CONFIG_ACCOUNT_REALM));

        return false;
    }

}