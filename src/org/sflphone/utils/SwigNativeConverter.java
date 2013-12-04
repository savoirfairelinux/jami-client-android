package org.sflphone.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;
import org.sflphone.service.ServiceConstants;
import org.sflphone.service.StringMap;
import org.sflphone.service.VectMap;

public class SwigNativeConverter {

    /**
     * Native to Swig
     */

    public static StringMap convertFromNativeToSwig(HashMap<String, String> nativemap) {
        StringMap swigmap = new StringMap();

        Set<String> keys = nativemap.keySet();
        for (String key : keys) {
            swigmap.set(key, nativemap.get(key));
        }
        return swigmap;
    }

    public static VectMap convertFromNativeToSwig(List creds) {
        ArrayList<HashMap<String, String>> todecode = (ArrayList<HashMap<String, String>>) creds;
        VectMap toReturn = new VectMap();

        HashMap<String, String> nativeEntry;

        for (int i = 0; i < todecode.size(); ++i) {
            nativeEntry = todecode.get(i);
            StringMap entry = new StringMap();
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, nativeEntry.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, nativeEntry.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
            entry.set(AccountDetailBasic.CONFIG_ACCOUNT_REALM, nativeEntry.get(AccountDetailBasic.CONFIG_ACCOUNT_REALM));
            toReturn.add(entry);
        }
        return toReturn;
    }

    /**
     * Swig to Native
     */
    public static ArrayList<HashMap<String, String>> convertHistoryToNative(VectMap swigmap) {
        ArrayList<HashMap<String, String>> nativemap = new ArrayList<HashMap<String, String>>();

        for (int i = 0; i < swigmap.size(); ++i) {
            HashMap<String, String> entry = new HashMap<String, String>();

            entry.put(ServiceConstants.history.ACCOUNT_ID_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.ACCOUNT_ID_KEY));
            entry.put(ServiceConstants.history.CALLID_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.CALLID_KEY));
            entry.put(ServiceConstants.history.CONFID_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.CONFID_KEY));
            entry.put(ServiceConstants.history.DISPLAY_NAME_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.DISPLAY_NAME_KEY));
            entry.put(ServiceConstants.history.PEER_NUMBER_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.PEER_NUMBER_KEY));
            entry.put(ServiceConstants.history.RECORDING_PATH_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.RECORDING_PATH_KEY));
            entry.put(ServiceConstants.history.STATE_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.STATE_KEY));
            entry.put(ServiceConstants.history.TIMESTAMP_START_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.TIMESTAMP_START_KEY));
            entry.put(ServiceConstants.history.TIMESTAMP_STOP_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.TIMESTAMP_STOP_KEY));
            entry.put(ServiceConstants.history.AUDIO_CODEC_KEY, tryToGet(swigmap.get(i), ServiceConstants.history.AUDIO_CODEC_KEY));

            nativemap.add(entry);
        }

        return nativemap;
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
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_TYPE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT));
        nativemap.put(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER, swigmap.get(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER));

        nativemap
                .put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
        nativemap.put(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, swigmap.get(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE));
        nativemap.put(AccountDetailAdvanced.CONFIG_STUN_SERVER, swigmap.get(AccountDetailAdvanced.CONFIG_STUN_SERVER));
        nativemap.put(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX, swigmap.get(AccountDetailAdvanced.CONFIG_ACCOUNT_MAILBOX));
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
            StringMap entry = new StringMap();
            HashMap<String, String> nativeEntry = new HashMap<String, String>();
            entry = map.get(i);
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD));
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));
            nativeEntry.put(AccountDetailBasic.CONFIG_ACCOUNT_REALM, entry.get(AccountDetailBasic.CONFIG_ACCOUNT_REALM));
            toReturn.add(nativeEntry);
        }
        return toReturn;
    }

}
