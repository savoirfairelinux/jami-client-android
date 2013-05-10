package com.savoirfairelinux.sflphone.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";
    private Context mContext; 

    static public final String SIGNAL_NAME = "signal-name";
    static public final String NEW_CALL_CREATED = "new-call-created"; 
    static public final String CALL_STATE_CHANGED = "call-state-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";

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
    
    @Override
    public void on_transfer_state_changed(String result){
        Log.w(TAG,"TRANSFER STATE CHANGED:"+result);
    }
    
    @Override
    public void on_conference_created(String confID){
        Log.w(TAG,"CONFERENCE CREATED:"+confID);
    }
    
    @Override
    public void on_incoming_message(String ID, String from, String msg){
        Log.w(TAG,"on_incoming_message:"+msg);
        sendIncomingTextMessage(ID, from, msg);
    }
    
    @Override
    public void on_conference_removed(String confID){
        Log.w(TAG,"CONFERENCE REMOVED:"+confID);
    }
    
    @Override
    public void on_conference_state_changed(String confID, String state){
        Log.w(TAG,"CONFERENCE STATE CHANGED:"+confID);
    }

    private void sendNewCallCreatedMessage(String accountID, String callID, String to) {
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putString("CallID", callID);
        bundle.putString("To", to);
        Intent intent = new Intent(NEW_CALL_CREATED);
        intent.putExtra(SIGNAL_NAME, NEW_CALL_CREATED);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newcall", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void sendCallStateChangedMessage(String callID, String state) {
        Bundle bundle = new Bundle();
        bundle.putString("CallID", callID);
        bundle.putString("State", state);
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra(SIGNAL_NAME, CALL_STATE_CHANGED); 
        intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void sendIncomingCallMessage(String accountID, String callID, String from) {
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putString("CallID", callID);
        bundle.putString("From", from);
        Intent intent = new Intent(INCOMING_CALL);
        intent.putExtra(SIGNAL_NAME, INCOMING_CALL); 
        intent.putExtra("com.savoirfairelinux.sflphone.service.newcall", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    private void sendIncomingTextMessage(String id, String from, String msg) {
        Bundle bundle = new Bundle();

        bundle.putString("CallID", id);
        bundle.putString("From", from);
        bundle.putString("Msg", msg);
        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra(SIGNAL_NAME, INCOMING_TEXT); 
        intent.putExtra("com.savoirfairelinux.sflphone.service.newtext", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        
    }
}
