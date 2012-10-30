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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;

public class AccountDetailSrtp implements AccountDetail{

    private static final String TAG = "AccountDetailSrtp";
    public static final String BUNDLE_TAG = "SrtpPreferenceArrayList";

    public static final String CONFIG_SRTP_ENABLE = "SRTP.enable";
    public static final String CONFIG_SRTP_KEY_EXCHANGE = "SRTP.keyExchange";
    public static final String CONFIG_SRTP_ENCRYPTION_ALGO = "SRTP.encryptionAlgorithm";  // Provided by ccRTP,0=NULL,1=AESCM,2=AESF8
    public static final String CONFIG_SRTP_RTP_FALLBACK = "SRTP.rtpFallback";
    public static final String CONFIG_ZRTP_HELLO_HASH = "ZRTP.helloHashEnable";
    public static final String CONFIG_ZRTP_DISPLAY_SAS = "ZRTP.displaySAS";
    public static final String CONFIG_ZRTP_NOT_SUPP_WARNING = "ZRTP.notSuppWarning";
    public static final String CONFIG_ZRTP_DISPLAY_SAS_ONCE = "ZRTP.displaySasOnce";

    private ArrayList<AccountDetail.PreferenceEntry> privateArray;

    public static ArrayList<AccountDetail.PreferenceEntry> getPreferenceEntries()
    {
        ArrayList<AccountDetail.PreferenceEntry> preference = new ArrayList<AccountDetail.PreferenceEntry>();

        preference.add(new PreferenceEntry(CONFIG_SRTP_ENABLE, R.string.account_srtp_enabled_label, true));
        preference.add(new PreferenceEntry(CONFIG_SRTP_KEY_EXCHANGE, R.string.account_srtp_exchange_label, true));
        preference.add(new PreferenceEntry(CONFIG_SRTP_ENCRYPTION_ALGO, R.string.account_encryption_algo_label, true));
        preference.add(new PreferenceEntry(CONFIG_SRTP_RTP_FALLBACK, R.string.account_srtp_fallback_label, true));
        preference.add(new PreferenceEntry(CONFIG_ZRTP_HELLO_HASH, R.string.account_hello_hash_enable_label, true));
        preference.add(new PreferenceEntry(CONFIG_ZRTP_DISPLAY_SAS, R.string.account_display_sas_label, true));
        preference.add(new PreferenceEntry(CONFIG_ZRTP_NOT_SUPP_WARNING, R.string.account_not_supported_warning_label, true));
        preference.add(new PreferenceEntry(CONFIG_ZRTP_DISPLAY_SAS_ONCE, R.string.account_display_sas_once_label, true));

        return preference; 
    }

    public AccountDetailSrtp()
    {
        privateArray = getPreferenceEntries();
    }

    public AccountDetailSrtp(HashMap<String, String> pref)
    {
        privateArray = getPreferenceEntries();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            p.mValue = pref.get(p.mKey);
        }
    }

    public AccountDetailSrtp(ArrayList<String> pref)
    {
        privateArray = getPreferenceEntries();

        if(pref.size() != privateArray.size()) {
            Log.i(TAG, "Error list are not of equal size");
        }
        else {
            int index = 0;
            for(String s : pref) {
                privateArray.get(index).mValue = s;
                index++;
            }
        }
    }

    public ArrayList<AccountDetail.PreferenceEntry> getDetailValues()
    {
        return privateArray;
    }

    public ArrayList<String> getValuesOnly()
    {
        ArrayList<String> valueList = new ArrayList<String>();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            valueList.add(p.mValue);
        }

        return valueList;
    }

    public HashMap<String, String> getDetailsHashMap()
    {
        HashMap<String, String> map = new HashMap<String, String>();

        for(AccountDetail.PreferenceEntry p : privateArray) {
            map.put(p.mKey, p.mValue);
        }

        return map;
    }

    public String getDetailString(String key)
    {
        String value = "";

        for(AccountDetail.PreferenceEntry p : privateArray) {
            if(p.mKey.equals(key)) {
                value = p.mValue;
                return value;
            }
        }

        return value;
    }

    public void setDetailString(int position, String newValue)
    {
        privateArray.get(position).mValue = newValue;
    }


    public boolean getDetailBoolean()
    {
        return true;
    }
}
