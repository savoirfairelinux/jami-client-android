package com.savoirfairelinux.sflphone.account;

import java.util.HashMap;

import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.service.StringMap;

public class CallDetailsHandler {

    public static HashMap<String, String> convertSwigToNative(StringMap swigmap) {

        HashMap<String, String> entry = new HashMap<String, String>();

        entry.put(ServiceConstants.call.CALL_TYPE, swigmap.get(ServiceConstants.call.CALL_TYPE));
        entry.put(ServiceConstants.call.PEER_NUMBER, swigmap.get(ServiceConstants.call.PEER_NUMBER));
        entry.put(ServiceConstants.call.DISPLAY_NAME, swigmap.get(ServiceConstants.call.DISPLAY_NAME));
        entry.put(ServiceConstants.call.CALL_STATE, swigmap.get(ServiceConstants.call.CALL_STATE));
        entry.put(ServiceConstants.call.CONF_ID, swigmap.get(ServiceConstants.call.CONF_ID));
        entry.put(ServiceConstants.call.TIMESTAMP_START, swigmap.get(ServiceConstants.call.TIMESTAMP_START));
        entry.put(ServiceConstants.call.ACCOUNTID, swigmap.get(ServiceConstants.call.ACCOUNTID));

        return entry;
    }

}
