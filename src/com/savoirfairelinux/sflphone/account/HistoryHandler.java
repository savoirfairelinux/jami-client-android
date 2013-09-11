package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.VectMap;
import com.savoirfairelinux.sflphone.service.StringMap;

public class HistoryHandler {
    private static final String TAG = HistoryHandler.class.getSimpleName();

    private static String tryToGet(StringMap smap, String key) {
        if (smap.has_key(key)) {
            return smap.get(key);
        } else {
            return "";
        }
    }

    public static ArrayList<HashMap<String, String>> convertSwigToNative(VectMap swigmap) {

        ArrayList<HashMap<String, String>> nativemap = new ArrayList<HashMap<String, String>>();

//        Log.w(TAG, "size history " + swigmap.size());

        for (int i = 0; i < swigmap.size(); ++i) {
            HashMap<String, String> entry = new HashMap<String, String>();

//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_ACCOUNT_ID_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_CALLID_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_CONFID_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_DISPLAY_NAME_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_PEER_NUMBER_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_RECORDING_PATH_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_STATE_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_TIMESTAMP_START_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY));
//            Log.i(TAG, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_AUDIO_CODEC_KEY));


            entry.put(ServiceConstants.HISTORY_ACCOUNT_ID_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_ACCOUNT_ID_KEY));
            entry.put(ServiceConstants.HISTORY_CALLID_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_CALLID_KEY));
            entry.put(ServiceConstants.HISTORY_CONFID_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_CONFID_KEY));
            entry.put(ServiceConstants.HISTORY_DISPLAY_NAME_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_DISPLAY_NAME_KEY));
            entry.put(ServiceConstants.HISTORY_PEER_NUMBER_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_PEER_NUMBER_KEY));
            entry.put(ServiceConstants.HISTORY_RECORDING_PATH_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_RECORDING_PATH_KEY));
            entry.put(ServiceConstants.HISTORY_STATE_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_STATE_KEY));
            entry.put(ServiceConstants.HISTORY_TIMESTAMP_START_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_TIMESTAMP_START_KEY));
            entry.put(ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY));
            entry.put(ServiceConstants.HISTORY_AUDIO_CODEC_KEY, tryToGet(swigmap.get(i), ServiceConstants.HISTORY_AUDIO_CODEC_KEY));

            nativemap.add(entry);
        }

        return nativemap;
    }
}
