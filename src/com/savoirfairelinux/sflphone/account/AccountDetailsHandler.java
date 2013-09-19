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
package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.StringMap;

public class AccountDetailsHandler {
//    private static final String TAG = "AccountDetailsHandler";

    public static class PreferenceEntry {
        public String mKey;
        public int mLabelId;

        public PreferenceEntry(String key, int labelId) {
            mKey = key;
            mLabelId = labelId;
        }
    }

    public static ArrayList<PreferenceEntry> getBasicDetailsKeys() {
        ArrayList<PreferenceEntry> basicDetailKeys = new ArrayList<PreferenceEntry>();

        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, R.string.account_type_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, R.string.account_alias_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, R.string.account_enabled_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, R.string.account_hostname_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, R.string.account_username_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, R.string.account_routeset_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, R.string.account_password_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_REALM, R.string.account_realm_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_REALM, R.string.account_useragent_label));
        basicDetailKeys.add(new PreferenceEntry(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, R.string.account_autoanswer_label));

        return basicDetailKeys;
    }

    public static ArrayList<PreferenceEntry> getAdvancedDetailsKeys() {
        ArrayList<PreferenceEntry> advancedDetailKeys = new ArrayList<PreferenceEntry>();

        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, R.string.account_registration_exp_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, R.string.account_registration_status_label));
        advancedDetailKeys
                .add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, R.string.account_registration_code_label));
        advancedDetailKeys
                .add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, R.string.account_registration_state_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_CREDENTIAL_NUMBER, R.string.account_credential_count_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, R.string.account_config_dtmf_type_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, R.string.account_ringtone_path_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED, R.string.account_ringtone_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED, R.string.account_keep_alive_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER, R.string.account_autoanswer_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, R.string.account_local_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL, R.string.account_published_same_as_local_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_LOCAL_PORT, R.string.account_local_port_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT, R.string.account_published_port_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS, R.string.account_published_address_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_DISPLAY_NAME, R.string.account_displayname_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_STUN_SERVER, R.string.account_stun_server_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailAdvanced.CONFIG_STUN_ENABLE, R.string.account_stun_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_SRTP_ENABLE, R.string.account_srtp_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, R.string.account_srtp_exchange_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_SRTP_ENCRYPTION_ALGO, R.string.account_encryption_algo_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK, R.string.account_srtp_fallback_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH, R.string.account_hello_hash_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS, R.string.account_display_sas_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING, R.string.account_not_supported_warning_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE, R.string.account_display_sas_once_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_LISTENER_PORT, R.string.account_listener_port_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_ENABLE, R.string.account_tls_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE, R.string.account_tls_certificate_list_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE, R.string.account_tls_certificate_file_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE, R.string.account_tls_private_key_file_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_PASSWORD, R.string.account_tls_password_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_METHOD, R.string.account_tls_method_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_CIPHERS, R.string.account_tls_ciphers_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_SERVER_NAME, R.string.account_tls_server_name_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER, R.string.account_tls_verify_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT, R.string.account_tls_verify_client_label));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE,
                R.string.account_tls_require_client_certificat_label));
        advancedDetailKeys
                .add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, R.string.account_tls_negotiation_timeout_sec));
        advancedDetailKeys.add(new PreferenceEntry(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC,
                R.string.account_tls_negotiation_timeout_msec));

        return advancedDetailKeys;
    }

    public static StringMap convertFromNativeToSwig(HashMap<String, String> nativemap) {
        StringMap swigmap = new StringMap();

        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_TYPE));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE));
        swigmap.set(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, nativemap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT));
        
        
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
        swigmap.set(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, nativemap.get(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE));
        swigmap.set(AccountDetailAdvanced.CONFIG_STUN_SERVER, nativemap.get(AccountDetailAdvanced.CONFIG_STUN_SERVER));
        
        // swigmap.set(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
        
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS));
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER));
        swigmap.set(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, nativemap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE));
        swigmap.set(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED, nativemap.get(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED));
        swigmap.set(AccountDetailAdvanced.CONFIG_LOCAL_PORT, nativemap.get(AccountDetailAdvanced.CONFIG_LOCAL_PORT));
        swigmap.set(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS, nativemap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS));
        swigmap.set(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT, nativemap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT));
        swigmap.set(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL, nativemap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL));
        swigmap.set(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED, nativemap.get(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED));
        swigmap.set(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, nativemap.get(AccountDetailAdvanced.CONFIG_RINGTONE_PATH));
        swigmap.set(AccountDetailAdvanced.CONFIG_STUN_ENABLE, nativemap.get(AccountDetailAdvanced.CONFIG_STUN_ENABLE));

        
        swigmap.set(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, nativemap.get(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
        swigmap.set(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK, nativemap.get(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK));
        swigmap.set(AccountDetailSrtp.CONFIG_SRTP_ENABLE, nativemap.get(AccountDetailSrtp.CONFIG_SRTP_ENABLE));
        swigmap.set(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS, nativemap.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS));
        swigmap.set(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE, nativemap.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
        swigmap.set(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH, nativemap.get(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH));
        swigmap.set(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING, nativemap.get(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING));

        swigmap.set(AccountDetailTls.CONFIG_TLS_CIPHERS, nativemap.get(AccountDetailTls.CONFIG_TLS_CIPHERS));
        swigmap.set(AccountDetailTls.CONFIG_TLS_LISTENER_PORT, nativemap.get(AccountDetailTls.CONFIG_TLS_LISTENER_PORT));
        swigmap.set(AccountDetailTls.CONFIG_TLS_METHOD, nativemap.get(AccountDetailTls.CONFIG_TLS_METHOD));
        // swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
        // swigmap.set(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, nativemap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));
        swigmap.set(AccountDetailTls.CONFIG_TLS_ENABLE, nativemap.get(AccountDetailTls.CONFIG_TLS_ENABLE));
        swigmap.set(AccountDetailTls.CONFIG_TLS_PASSWORD, nativemap.get(AccountDetailTls.CONFIG_TLS_PASSWORD));
        swigmap.set(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE, nativemap.get(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE));
        swigmap.set(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, nativemap.get(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE));
        swigmap.set(AccountDetailTls.CONFIG_TLS_SERVER_NAME, nativemap.get(AccountDetailTls.CONFIG_TLS_SERVER_NAME));
        swigmap.set(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT, nativemap.get(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT));
        swigmap.set(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE, nativemap.get(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE));
        swigmap.set(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE, nativemap.get(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE));
        swigmap.set(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER, nativemap.get(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER));
        

        return swigmap;
    }

    public static HashMap<String, String> convertSwigToNative(StringMap swigmap) {

        HashMap<String, String> nativemap = new HashMap<String, String>();

        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_TYPE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT));

        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
        nativemap.put(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, swigmap.get(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE));
        nativemap.put(AccountDetailAdvanced.CONFIG_STUN_SERVER, swigmap.get(AccountDetailAdvanced.CONFIG_STUN_SERVER));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE));
        nativemap.put(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED, swigmap.get(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED));
        nativemap.put(AccountDetailAdvanced.CONFIG_LOCAL_PORT, swigmap.get(AccountDetailAdvanced.CONFIG_LOCAL_PORT));
        nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS));
        nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT));
        nativemap.put(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL, swigmap.get(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL));
        nativemap.put(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED, swigmap.get(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED));
        nativemap.put(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, swigmap.get(AccountDetailAdvanced.CONFIG_RINGTONE_PATH));
        nativemap.put(AccountDetailAdvanced.CONFIG_STUN_ENABLE, swigmap.get(AccountDetailAdvanced.CONFIG_STUN_ENABLE));
        
        
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
        nativemap.put(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, swigmap.get(AccountDetailTls.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
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
        
        /*
         * nativemap.put(ServiceConstants.CONFIG_CREDENTIAL_NUMBER, swigmap.get(ServiceConstants.CONFIG_CREDENTIAL_NUMBER));
         * nativemap.put(ServiceConstants.CONFIG_ACCOUNT_PASSWORD, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_PASSWORD));
         * nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REALM));
         */

        /*
         * nativemap.put(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM));
         * nativemap.put(ServiceConstants.CONFIG_INTERFACE, swigmap.get(ServiceConstants.CONFIG_INTERFACE));
         * nativemap.put(ServiceConstants.CONFIG_DEFAULT_INTERFACE, swigmap.get(ServiceConstants.CONFIG_DEFAULT_INTERFACE));
         * nativemap.put(ServiceConstants.CONFIG_DISPLAY_NAME, swigmap.get(ServiceConstants.CONFIG_DISPLAY_NAME));
         * nativemap.put(ServiceConstants.CONFIG_DEFAULT_ADDRESS, swigmap.get(ServiceConstants.CONFIG_DEFAULT_ADDRESS));
         * nativemap.put(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO, swigmap.get(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO));
         */

        return nativemap;
    }
};
