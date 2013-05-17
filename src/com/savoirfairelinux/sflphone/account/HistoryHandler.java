package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.VectMap;

public class HistoryHandler {
    private static final String TAG = HistoryHandler.class.getSimpleName();


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