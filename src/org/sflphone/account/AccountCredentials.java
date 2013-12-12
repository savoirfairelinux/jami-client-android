package org.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.R;

import android.util.Log;

public class AccountCredentials implements AccountDetail {

    private static final String TAG = AccountCredentials.class.getSimpleName();

    public static final String CONFIG_ACCOUNT_USERNAME = "Account.username";
    public static final String CONFIG_ACCOUNT_PASSWORD = "Account.password";
    public static final String CONFIG_ACCOUNT_REALM = "Account.realm";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries() {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<AccountDetail.PreferenceEntry>();

        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_USERNAME));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_PASSWORD));
        preference.add(new PreferenceEntry(CONFIG_ACCOUNT_REALM));

        return preference;
    }

    public AccountCredentials() {
        privateArray = getPreferenceEntries();
    }

    public AccountCredentials(HashMap<String, String> pref) {
        privateArray = getPreferenceEntries();

        for (AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }

    }

    public AccountCredentials(ArrayList<String> pref) {
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
            map.put(p.mKey, p.mValue);
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