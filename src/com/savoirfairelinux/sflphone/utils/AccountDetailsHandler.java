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

import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.StringMap;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AccountDetailsHandler {
    private static final String TAG = "AccountDetailsHandler";

    public static class PreferenceEntry
    {
        public String mKey;
        public int mLabelId;

        public PreferenceEntry(String key, int labelId)
        {
            mKey = key;
            mLabelId = labelId;
        }
    }

    public static ArrayList<PreferenceEntry> getBasicDetailsKeys()
    {
        ArrayList<PreferenceEntry> basicDetailKeys = new ArrayList<PreferenceEntry>();

        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_TYPE,
                            R.string.account_type_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ALIAS,
                            R.string.account_alias_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ENABLE,
                            R.string.account_enabled_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME,
                            R.string.account_hostname_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERNAME,
                            R.string.account_username_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ROUTESET,
                            R.string.account_routeset_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_PASSWORD,
                            R.string.account_password_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REALM,
                            R.string.account_realm_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM,
                            R.string.account_useragent_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERAGENT,
                            R.string.account_autoanswer_label));

        return basicDetailKeys;
    }

    public static ArrayList<PreferenceEntry> getAdvancedDetailsKeys()
    {
        ArrayList<PreferenceEntry> advancedDetailKeys = new ArrayList<PreferenceEntry>();

        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE,
                            R.string.account_registration_exp_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS,
                            R.string.account_registration_status_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE,
                            R.string.account_registration_code_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC,
                            R.string.account_registration_state_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_CREDENTIAL_NUMBER,
                            R.string.account_credential_count_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE,
                            R.string.account_config_dtmf_type_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_PATH,
                            R.string.account_ringtone_path_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_ENABLED,
                            R.string.account_ringtone_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED,
                            R.string.account_keep_alive_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER,
                            R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_INTERFACE,
                            R.string.account_local_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_INTERFACE,
                            R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL,
                            R.string.account_published_same_as_local_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_PORT,
                            R.string.account_local_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_PORT,
                            R.string.account_published_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_ADDRESS,
                            R.string.account_published_address_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_DISPLAY_NAME,
                            R.string.account_displayname_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_SERVER,
                            R.string.account_stun_server_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_ENABLE,
                            R.string.account_stun_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENABLE,
                            R.string.account_srtp_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE,
                            R.string.account_srtp_exchange_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO,
                            R.string.account_encryption_algo_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK,
                            R.string.account_srtp_fallback_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_HELLO_HASH,
                            R.string.account_hello_hash_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS,
                            R.string.account_display_sas_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING,
                            R.string.account_not_supported_warning_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE,
                            R.string.account_display_sas_once_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_LISTENER_PORT,
                            R.string.account_listener_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_ENABLE,
                            R.string.account_tls_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CA_LIST_FILE,
                            R.string.account_tls_certificate_list_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE,
                            R.string.account_tls_certificate_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE,
                            R.string.account_tls_private_key_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PASSWORD,
                            R.string.account_tls_password_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_METHOD,
                            R.string.account_tls_method_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CIPHERS,
                            R.string.account_tls_ciphers_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_SERVER_NAME,
                            R.string.account_tls_server_name_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_SERVER,
                            R.string.account_tls_verify_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT,
                            R.string.account_tls_verify_client_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE,
                            R.string.account_tls_require_client_certificat_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC,
                            R.string.account_tls_negotiation_timeout_sec));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC,
                            R.string.account_tls_negotiation_timeout_msec));

        return advancedDetailKeys; 
    }

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

        // swigmap.set(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
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
        // swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
        // swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));

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
