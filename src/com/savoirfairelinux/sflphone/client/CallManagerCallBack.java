package com.savoirfairelinux.sflphone.client;

import android.util.Log;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");
    }
}
