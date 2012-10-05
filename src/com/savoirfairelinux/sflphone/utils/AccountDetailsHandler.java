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

import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.StringMap;

import java.util.Map;
import java.util.HashMap;

public class AccountDetailsHandler {

    public static StringMap convertFromNativeToSwig(HashMap<String, String> nativemap)
    {
        StringMap swigmap = new StringMap();

        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ALIAS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_USERNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ROUTESET, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));

        swigmap.set(ServiceConstants.CONFIG_LOCAL_INTERFACE, nativemap.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
        swigmap.set(ServiceConstants.CONFIG_STUN_SERVER, nativemap.get(ServiceConstants.CONFIG_STUN_SERVER));
        swigmap.set(ServiceConstants.CONFIG_TLS_ENABLE, nativemap.get(ServiceConstants.CONFIG_TLS_ENABLE));
        swigmap.set(ServiceConstants.CONFIG_SRTP_ENABLE, nativemap.get(ServiceConstants.CONFIG_SRTP_ENABLE));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_TYPE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_TYPE));

        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ENABLE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ENABLE));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));

        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE));
        swigmap.set(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED, nativemap.get(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED));
        swigmap.set(ServiceConstants.CONFIG_LOCAL_PORT, nativemap.get(ServiceConstants.CONFIG_LOCAL_PORT));
        swigmap.set(ServiceConstants.CONFIG_PUBLISHED_ADDRESS, nativemap.get(ServiceConstants.CONFIG_PUBLISHED_ADDRESS));

        swigmap.set(ServiceConstants.CONFIG_PUBLISHED_PORT, nativemap.get(ServiceConstants.CONFIG_PUBLISHED_PORT));
        swigmap.set(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL, nativemap.get(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL));
        swigmap.set(ServiceConstants.CONFIG_RINGTONE_ENABLED, nativemap.get(ServiceConstants.CONFIG_RINGTONE_ENABLED));
        swigmap.set(ServiceConstants.CONFIG_RINGTONE_PATH, nativemap.get(ServiceConstants.CONFIG_RINGTONE_PATH));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_USERAGENT, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_USERAGENT));
            
        swigmap.set(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE, nativemap.get(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE));
        swigmap.set(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK, nativemap.get(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK));
        swigmap.set(ServiceConstants.CONFIG_STUN_ENABLE, nativemap.get(ServiceConstants.CONFIG_STUN_ENABLE));
        swigmap.set(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE, nativemap.get(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE));
        swigmap.set(ServiceConstants.CONFIG_TLS_CA_LIST_FILE, nativemap.get(ServiceConstants.CONFIG_TLS_CA_LIST_FILE));

        swigmap.set(ServiceConstants.CONFIG_TLS_CIPHERS, nativemap.get(ServiceConstants.CONFIG_TLS_CIPHERS));
        swigmap.set(ServiceConstants.CONFIG_TLS_LISTENER_PORT, nativemap.get(ServiceConstants.CONFIG_TLS_LISTENER_PORT));
        swigmap.set(ServiceConstants.CONFIG_TLS_METHOD, nativemap.get(ServiceConstants.CONFIG_TLS_METHOD));
        swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
        swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));

        swigmap.set(ServiceConstants.CONFIG_TLS_PASSWORD, nativemap.get(ServiceConstants.CONFIG_TLS_PASSWORD));
        swigmap.set(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE, nativemap.get(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE));
        swigmap.set(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, nativemap.get(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE));
        swigmap.set(ServiceConstants.CONFIG_TLS_SERVER_NAME, nativemap.get(ServiceConstants.CONFIG_TLS_SERVER_NAME));
        swigmap.set(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT, nativemap.get(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT));

        swigmap.set(ServiceConstants.CONFIG_TLS_VERIFY_SERVER, nativemap.get(ServiceConstants.CONFIG_TLS_VERIFY_SERVER));
        swigmap.set(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS, nativemap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS));
        swigmap.set(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE, nativemap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
        swigmap.set(ServiceConstants.CONFIG_ZRTP_HELLO_HASH, nativemap.get(ServiceConstants.CONFIG_ZRTP_HELLO_HASH));
        swigmap.set(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING, nativemap.get(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING));

        return swigmap;
    } 

    public static HashMap<String, String> convertSwigToNative(StringMap swigmap) {

        HashMap<String, String> nativemap = new HashMap<String, String>();

        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ALIAS, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_USERNAME, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ROUTESET, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
            
        nativemap.put(ServiceConstants.CONFIG_LOCAL_INTERFACE, swigmap.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
        nativemap.put(ServiceConstants.CONFIG_STUN_SERVER, swigmap.get(ServiceConstants.CONFIG_STUN_SERVER));
        nativemap.put(ServiceConstants.CONFIG_TLS_ENABLE, swigmap.get(ServiceConstants.CONFIG_TLS_ENABLE));
        nativemap.put(ServiceConstants.CONFIG_SRTP_ENABLE, swigmap.get(ServiceConstants.CONFIG_SRTP_ENABLE));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_TYPE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_TYPE));
            
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ENABLE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ENABLE));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));

        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE));
        nativemap.put(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED, swigmap.get(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED));
        nativemap.put(ServiceConstants.CONFIG_LOCAL_PORT, swigmap.get(ServiceConstants.CONFIG_LOCAL_PORT));
        nativemap.put(ServiceConstants.CONFIG_PUBLISHED_ADDRESS, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_ADDRESS));

        nativemap.put(ServiceConstants.CONFIG_PUBLISHED_PORT, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_PORT));
        nativemap.put(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL));
        nativemap.put(ServiceConstants.CONFIG_RINGTONE_ENABLED, swigmap.get(ServiceConstants.CONFIG_RINGTONE_ENABLED));
        nativemap.put(ServiceConstants.CONFIG_RINGTONE_PATH, swigmap.get(ServiceConstants.CONFIG_RINGTONE_PATH));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_USERAGENT, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_USERAGENT));
            
        nativemap.put(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE, swigmap.get(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE));
        nativemap.put(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK, swigmap.get(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK));
        nativemap.put(ServiceConstants.CONFIG_STUN_ENABLE, swigmap.get(ServiceConstants.CONFIG_STUN_ENABLE));
        nativemap.put(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE));
        nativemap.put(ServiceConstants.CONFIG_TLS_CA_LIST_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_CA_LIST_FILE));

        nativemap.put(ServiceConstants.CONFIG_TLS_CIPHERS, swigmap.get(ServiceConstants.CONFIG_TLS_CIPHERS));
        nativemap.put(ServiceConstants.CONFIG_TLS_LISTENER_PORT, swigmap.get(ServiceConstants.CONFIG_TLS_LISTENER_PORT));
        nativemap.put(ServiceConstants.CONFIG_TLS_METHOD, swigmap.get(ServiceConstants.CONFIG_TLS_METHOD));
        nativemap.put(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, swigmap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
        nativemap.put(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, swigmap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));

        nativemap.put(ServiceConstants.CONFIG_TLS_PASSWORD, swigmap.get(ServiceConstants.CONFIG_TLS_PASSWORD));
        nativemap.put(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE));
        nativemap.put(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, swigmap.get(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE));
        nativemap.put(ServiceConstants.CONFIG_TLS_SERVER_NAME, swigmap.get(ServiceConstants.CONFIG_TLS_SERVER_NAME));
        nativemap.put(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT, swigmap.get(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT));

        nativemap.put(ServiceConstants.CONFIG_TLS_VERIFY_SERVER, swigmap.get(ServiceConstants.CONFIG_TLS_VERIFY_SERVER));
        nativemap.put(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS, swigmap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS));
        nativemap.put(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE, swigmap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
        nativemap.put(ServiceConstants.CONFIG_ZRTP_HELLO_HASH, swigmap.get(ServiceConstants.CONFIG_ZRTP_HELLO_HASH));
        nativemap.put(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING, swigmap.get(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING));

        /*
        nativemap.put(ServiceConstants.CONFIG_CREDENTIAL_NUMBER, swigmap.get(ServiceConstants.CONFIG_CREDENTIAL_NUMBER));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_PASSWORD, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_PASSWORD));
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REALM));
        */

        /*
        nativemap.put(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM));
        nativemap.put(ServiceConstants.CONFIG_INTERFACE, swigmap.get(ServiceConstants.CONFIG_INTERFACE));
        nativemap.put(ServiceConstants.CONFIG_DEFAULT_INTERFACE, swigmap.get(ServiceConstants.CONFIG_DEFAULT_INTERFACE));
        nativemap.put(ServiceConstants.CONFIG_DISPLAY_NAME, swigmap.get(ServiceConstants.CONFIG_DISPLAY_NAME));
        nativemap.put(ServiceConstants.CONFIG_DEFAULT_ADDRESS, swigmap.get(ServiceConstants.CONFIG_DEFAULT_ADDRESS));
        nativemap.put(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO, swigmap.get(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO));
        */

        return nativemap;
    }
}; 
