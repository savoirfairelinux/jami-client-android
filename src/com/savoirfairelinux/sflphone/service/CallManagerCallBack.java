package com.savoirfairelinux.sflphone.service;

import android.util.Log;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";

    @Override
    public void on_new_call_created(String accountID, String callID, String to) {
        Log.d(TAG, "on_new_call_created(" + accountID + ", " + callID + ", " + to + ")");
    }

    @Override
    public void on_call_state_changed(String callID, String state) {
        Log.d(TAG, "on_call_state_changed(" + callID + ", " + state + ")");
    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");
    }
}
