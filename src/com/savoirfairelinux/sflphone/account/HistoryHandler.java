package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.StringMap;
import com.savoirfairelinux.sflphone.service.VectMap;

public class HistoryHandler {
    private static final String TAG = HistoryHandler.class.getSimpleName();

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

        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_TYPE, R.string.account_type_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ALIAS, R.string.account_alias_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ENABLE, R.string.account_enabled_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, R.string.account_hostname_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERNAME, R.string.account_username_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ROUTESET, R.string.account_routeset_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_PASSWORD, R.string.account_password_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REALM, R.string.account_realm_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM, R.string.account_useragent_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERAGENT, R.string.account_autoanswer_label));

        return basicDetailKeys;
    }

    public static ArrayList<PreferenceEntry> getAdvancedDetailsKeys() {
        ArrayList<PreferenceEntry> advancedDetailKeys = new ArrayList<PreferenceEntry>();

        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, R.string.account_registration_exp_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS, R.string.account_registration_status_label));
        advancedDetailKeys
                .add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, R.string.account_registration_code_label));
        advancedDetailKeys
                .add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, R.string.account_registration_state_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_CREDENTIAL_NUMBER, R.string.account_credential_count_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE, R.string.account_config_dtmf_type_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_PATH, R.string.account_ringtone_path_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_ENABLED, R.string.account_ringtone_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED, R.string.account_keep_alive_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER, R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_INTERFACE, R.string.account_local_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_INTERFACE, R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL, R.string.account_published_same_as_local_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_PORT, R.string.account_local_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_PORT, R.string.account_published_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_ADDRESS, R.string.account_published_address_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_DISPLAY_NAME, R.string.account_displayname_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_SERVER, R.string.account_stun_server_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_ENABLE, R.string.account_stun_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENABLE, R.string.account_srtp_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE, R.string.account_srtp_exchange_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO, R.string.account_encryption_algo_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK, R.string.account_srtp_fallback_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_HELLO_HASH, R.string.account_hello_hash_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS, R.string.account_display_sas_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING, R.string.account_not_supported_warning_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE, R.string.account_display_sas_once_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_LISTENER_PORT, R.string.account_listener_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_ENABLE, R.string.account_tls_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CA_LIST_FILE, R.string.account_tls_certificate_list_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE, R.string.account_tls_certificate_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE, R.string.account_tls_private_key_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PASSWORD, R.string.account_tls_password_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_METHOD, R.string.account_tls_method_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CIPHERS, R.string.account_tls_ciphers_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_SERVER_NAME, R.string.account_tls_server_name_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_SERVER, R.string.account_tls_verify_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT, R.string.account_tls_verify_client_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE,
                R.string.account_tls_require_client_certificat_label));
        advancedDetailKeys
                .add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, R.string.account_tls_negotiation_timeout_sec));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC,
                R.string.account_tls_negotiation_timeout_msec));

        return advancedDetailKeys;
    }

    public static StringMap convertFromNativeToSwig(HashMap<String, String> nativemap) {
        StringMap swigmap = new StringMap();

        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ALIAS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_USERNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
        swigmap.set(ServiceConstants.CONFIG_ACCOUNT_PASSWORD, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_PASSWORD));
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

    public static ArrayList<HashMap<String, String>> convertSwigToNative(VectMap swigmap) {

        ArrayList<HashMap<String, String>> nativemap = new ArrayList<HashMap<String, String>>();

        Log.w(TAG, "size history " + swigmap.size());

        for (int i = 0; i < swigmap.size(); ++i) {
            HashMap<String, String> entry = new HashMap<String, String>();

            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_ACCOUNT_ID_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_CALLID_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_CONFID_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_DISPLAY_NAME_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_PEER_NUMBER_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_RECORDING_PATH_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_STATE_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_TIMESTAMP_START_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY));
            Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_AUDIO_CODEC_KEY));
            try {
                if(swigmap.get(i).get(ServiceConstants.HISTORY_MISSED_STRING) != null)
                    Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_MISSED_STRING));
                if(swigmap.get(i).get(ServiceConstants.HISTORY_INCOMING_STRING) != null)
                    Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_INCOMING_STRING));
                if(swigmap.get(i).get(ServiceConstants.HISTORY_OUTGOING_STRING) != null)
                    Log.i(TAG, swigmap.get(i).get(ServiceConstants.HISTORY_OUTGOING_STRING));
                
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            entry.put(ServiceConstants.HISTORY_ACCOUNT_ID_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_ACCOUNT_ID_KEY));
            entry.put(ServiceConstants.HISTORY_CALLID_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_CALLID_KEY));
            entry.put(ServiceConstants.HISTORY_CONFID_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_CONFID_KEY));
            entry.put(ServiceConstants.HISTORY_DISPLAY_NAME_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_DISPLAY_NAME_KEY));
            entry.put(ServiceConstants.HISTORY_PEER_NUMBER_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_PEER_NUMBER_KEY));
            entry.put(ServiceConstants.HISTORY_RECORDING_PATH_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_RECORDING_PATH_KEY));
            entry.put(ServiceConstants.HISTORY_STATE_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_STATE_KEY));
            entry.put(ServiceConstants.HISTORY_TIMESTAMP_START_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_TIMESTAMP_START_KEY));
            entry.put(ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY));
            entry.put(ServiceConstants.HISTORY_AUDIO_CODEC_KEY, swigmap.get(i).get(ServiceConstants.HISTORY_AUDIO_CODEC_KEY));
            
            try {
                if(swigmap.get(i).get(ServiceConstants.HISTORY_MISSED_STRING) != null)
                    entry.put(ServiceConstants.HISTORY_MISSED_STRING, swigmap.get(i).get(ServiceConstants.HISTORY_MISSED_STRING)); 
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            try {
                if(swigmap.get(i).get(ServiceConstants.HISTORY_INCOMING_STRING) != null)
                    entry.put(ServiceConstants.HISTORY_INCOMING_STRING, swigmap.get(i).get(ServiceConstants.HISTORY_INCOMING_STRING));
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            try {
                if(swigmap.get(i).get(ServiceConstants.HISTORY_OUTGOING_STRING) != null)
                    entry.put(ServiceConstants.HISTORY_OUTGOING_STRING, swigmap.get(i).get(ServiceConstants.HISTORY_OUTGOING_STRING));
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
           
            
            
            
            
            
            nativemap.add(entry);
        }

        return nativemap;
    }
}