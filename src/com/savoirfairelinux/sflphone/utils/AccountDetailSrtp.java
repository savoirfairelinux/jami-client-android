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

public class AccountDetailSrtp implements AccountDetail{

    public static final String CONFIG_SRTP_ENABLE = "SRTP.enable";
    public static final String CONFIG_SRTP_KEY_EXCHANGE = "SRTP.keyExchange";
    public static final String CONFIG_SRTP_ENCRYPTION_ALGO = "SRTP.encryptionAlgorithm";  // Provided by ccRTP,0=NULL,1=AESCM,2=AESF8
    public static final String CONFIG_SRTP_RTP_FALLBACK = "SRTP.rtpFallback";
    public static final String CONFIG_ZRTP_HELLO_HASH = "ZRTP.helloHashEnable";
    public static final String CONFIG_ZRTP_DISPLAY_SAS = "ZRTP.displaySAS";
    public static final String CONFIG_ZRTP_NOT_SUPP_WARNING = "ZRTP.notSuppWarning";
    public static final String CONFIG_ZRTP_DISPLAY_SAS_ONCE = "ZRTP.displaySasOnce";

    private HashMap<String, AccountDetail.PreferenceEntry> privateMap;

    public AccountDetailSrtp()
    {
        privateMap = new HashMap<String, AccountDetail.PreferenceEntry>();

        privateMap.put(CONFIG_SRTP_ENABLE,
                       new PreferenceEntry(CONFIG_SRTP_ENABLE, R.string.account_srtp_enabled_label));
        privateMap.put(CONFIG_SRTP_KEY_EXCHANGE,
                       new PreferenceEntry(CONFIG_SRTP_KEY_EXCHANGE, R.string.account_srtp_exchange_label));
        privateMap.put(CONFIG_SRTP_ENCRYPTION_ALGO,
                       new PreferenceEntry(CONFIG_SRTP_ENCRYPTION_ALGO, R.string.account_encryption_algo_label));
        privateMap.put(CONFIG_SRTP_RTP_FALLBACK,
                       new PreferenceEntry(CONFIG_SRTP_RTP_FALLBACK, R.string.account_srtp_fallback_label));
        privateMap.put(CONFIG_ZRTP_HELLO_HASH,
                       new PreferenceEntry(CONFIG_ZRTP_HELLO_HASH, R.string.account_hello_hash_enable_label));
        privateMap.put(CONFIG_ZRTP_DISPLAY_SAS,
                       new PreferenceEntry(CONFIG_ZRTP_DISPLAY_SAS, R.string.account_display_sas_label));
        privateMap.put(CONFIG_ZRTP_NOT_SUPP_WARNING,
                       new PreferenceEntry(CONFIG_ZRTP_NOT_SUPP_WARNING, R.string.account_not_supported_warning_label));
        privateMap.put(CONFIG_ZRTP_DISPLAY_SAS_ONCE,
                       new PreferenceEntry(CONFIG_ZRTP_DISPLAY_SAS_ONCE, R.string.account_display_sas_once_label));
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
