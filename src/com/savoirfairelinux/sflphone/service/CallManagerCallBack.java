package com.savoirfairelinux.sflphone.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";
    private Context mContext; 

    public CallManagerCallBack(Context context) {
        mContext = context;
    }

    @Override
    public void on_new_call_created(String accountID, String callID, String to) {
        Log.d(TAG, "on_new_call_created(" + accountID + ", " + callID + ", " + to + ")");
        sendNewCallCreatedMessage(accountID, callID, to);
    }

    @Override
    public void on_call_state_changed(String callID, String state) {
        Log.d(TAG, "on_call_state_changed(" + callID + ", " + state + ")");
        sendCallStateChangedMessage(callID, state);
    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");
        sendIncomingCallMessage(accountID, callID, from);
    }

    private void sendNewCallCreatedMessage(String accountID, String callID, String to) {
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putString("CallID", callID);
        bundle.putString("To", to);
        Intent intent = new Intent("new-call-created");
        intent.putExtra("signal-name", "new-call-created");
        intent.putExtra("newcall", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void sendCallStateChangedMessage(String callID, String state) {
        Bundle bundle = new Bundle();
        bundle.putString("CallID", callID);
        bundle.putString("State", state);
        Intent intent = new Intent("call-state-changed");
        intent.putExtra("signal-name", "call-state-changed"); 
        intent.putExtra("newstate", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void sendIncomingCallMessage(String accountID, String callID, String from) {
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putString("CallID", callID);
        bundle.putString("From", from);
        Intent intent = new Intent("incoming-call");
        intent.putExtra("signal-name", "incoming-call"); 
        intent.putExtra("newcall", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
