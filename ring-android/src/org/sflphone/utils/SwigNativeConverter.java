/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.sflphone.model.account.AccountDetailAdvanced;
import org.sflphone.model.account.AccountDetailBasic;
import org.sflphone.model.account.AccountDetailSrtp;
import org.sflphone.model.account.AccountDetailTls;
import org.sflphone.service.ServiceConstants;
import org.sflphone.service.StringMap;
import org.sflphone.service.StringVect;
import org.sflphone.service.VectMap;

public class SwigNativeConverter {

    /**
     * Native to Swig
     */

    public static StringMap convertFromNativeToSwig(HashMap<String, String> nativemap) {
        StringMap swigmap = new StringMap();

        Set<String> keys = nativemap.keySet();
        for (String key : keys) {
            if (nativemap.get(key) == null) {
                swigmap.set(key, "");
            } else {
                swigmap.set(key, nativemap.get(key));
            }
        }
        return swigmap;
    }

    public static VectMap convertFromNativeToSwig(List creds) {
        ArrayList<HashMap<String, String>> todecode = (ArrayList<HashMap<String, String>>) creds;
        VectMap toReturn = new VectMap();

        for (HashMap<String, String> aTodecode : todecode) {
            StringMap entry = new StringMap();
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, aTodecode.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, aTodecode.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_REALM, aTodecode.get(AccountDetailBasic.CONFIG_ACCOUNT_REALM));
            toReturn.add(entry);
        }
        return toReturn;
    }

    private static String tryToGet(StringMap smap, String key) {
        if (smap.has_key(key)) {
            return smap.get(key);
        } else {
            return "";
        }
    }

    public static HashMap<String, String> convertAccountToNative(StringMap swigmap) {
        HashMap<String, String> nativemap = new HashMap<String, String>();

        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_TYPE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER));

		if (swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_TYPE).equals("SIP"))
		{

            nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET));
			nativemap
				.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
			nativemap.put(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, swigmap.get(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE));
			nativemap.put(AccountDetailAdvanced.CONFIG_STUN_SERVER, swigmap.get(AccountDetailAdvanced.CONFIG_STUN_SERVER));
			nativemap
				.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS));
			nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE,
				swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
			nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC,
				swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));
			nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE));
			nativemap.put(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED, swigmap.get(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED));
			nativemap.put(AccountDetailAdvanced.CONFIG_LOCAL_PORT, swigmap.get(AccountDetailAdvanced.CONFIG_LOCAL_PORT));
			nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS));
			nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT));
			nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL));
			nativemap.put(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED, swigmap.get(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED));
			nativemap.put(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, swigmap.get(AccountDetailAdvanced.CONFIG_RINGTONE_PATH));
			nativemap.put(AccountDetailAdvanced.CONFIG_STUN_ENABLE, swigmap.get(AccountDetailAdvanced.CONFIG_STUN_ENABLE));
			nativemap.put(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MAX, swigmap.get(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MAX));
			nativemap.put(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MIN, swigmap.get(AccountDetailAdvanced.CONFIG_AUDIO_PORT_MIN));

			nativemap.put(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, swigmap.get(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
			nativemap.put(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK, swigmap.get(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK));
			nativemap.put(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS, swigmap.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS));
			nativemap.put(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE, swigmap.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
			nativemap.put(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH, swigmap.get(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH));
			nativemap.put(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING, swigmap.get(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING));
			nativemap.put(AccountDetailSrtp.CONFIG_SRTP_ENABLE, swigmap.get(AccountDetailSrtp.CONFIG_SRTP_ENABLE));

			nativemap.put(AccountDetailTls.CONFIG_TLS_CIPHERS, swigmap.get(AccountDetailTls.CONFIG_TLS_CIPHERS));
			nativemap.put(AccountDetailTls.CONFIG_TLS_LISTENER_PORT, swigmap.get(AccountDetailTls.CONFIG_TLS_LISTENER_PORT));
			nativemap.put(AccountDetailTls.CONFIG_TLS_METHOD, swigmap.get(AccountDetailTls.CONFIG_TLS_METHOD));
			nativemap.put(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, swigmap.get(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));
			nativemap.put(AccountDetailTls.CONFIG_TLS_PASSWORD, swigmap.get(AccountDetailTls.CONFIG_TLS_PASSWORD));
			nativemap.put(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE, swigmap.get(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE));
			nativemap.put(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, swigmap.get(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE));
			nativemap.put(AccountDetailTls.CONFIG_TLS_SERVER_NAME, swigmap.get(AccountDetailTls.CONFIG_TLS_SERVER_NAME));
			nativemap.put(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT, swigmap.get(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT));
			nativemap.put(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER, swigmap.get(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER));
			nativemap.put(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE, swigmap.get(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE));
			nativemap.put(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE, swigmap.get(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE));
			nativemap.put(AccountDetailTls.CONFIG_TLS_ENABLE, swigmap.get(AccountDetailTls.CONFIG_TLS_ENABLE));
		}

        return nativemap;
    }

    public static HashMap<String, String> convertCallDetailsToNative(StringMap swigmap) {

        HashMap<String, String> entry = new HashMap<String, String>();

        entry.put(ServiceConstants.call.CALL_TYPE, tryToGet(swigmap, ServiceConstants.call.CALL_TYPE));
        entry.put(ServiceConstants.call.PEER_NUMBER, tryToGet(swigmap, ServiceConstants.call.PEER_NUMBER));
        entry.put(ServiceConstants.call.DISPLAY_NAME, tryToGet(swigmap, ServiceConstants.call.DISPLAY_NAME));
        entry.put(ServiceConstants.call.CALL_STATE, tryToGet(swigmap, ServiceConstants.call.CALL_STATE));
        entry.put(ServiceConstants.call.CONF_ID, tryToGet(swigmap, ServiceConstants.call.CONF_ID));
        entry.put(ServiceConstants.call.TIMESTAMP_START, tryToGet(swigmap, ServiceConstants.call.TIMESTAMP_START));
        entry.put(ServiceConstants.call.ACCOUNTID, tryToGet(swigmap, ServiceConstants.call.ACCOUNTID));

        return entry;
    }

    public static ArrayList<HashMap<String, String>> convertCredentialsToNative(VectMap map) {

        ArrayList<HashMap<String, String>> toReturn = new ArrayList<HashMap<String, String>>();

        for (int i = 0; i < map.size(); ++i) {
            StringMap entry;
            HashMap<String, String> nativeEntry = new HashMap<String, String>();
            entry = map.get(i);
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_REALM, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_REALM));
            toReturn.add(nativeEntry);
        }
        return toReturn;
    }

    public static ArrayList<String> convertSwigToNative(StringVect vector) {
        ArrayList<String> toReturn = new ArrayList<String>();
        for (int i = 0; i < vector.size(); ++i) {
            toReturn.add(vector.get(i));
        }
        return toReturn;
    }
}
