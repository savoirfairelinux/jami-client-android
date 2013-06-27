package com.savoirfairelinux.sflphone.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";
    private Context mContext; 

    static public final String NEW_CALL_CREATED = "new-call-created"; 
    static public final String CALL_STATE_CHANGED = "call-state-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";
    static public final String CONF_CREATED = "conf_created";
    static public final String CONF_REMOVED = "conf_removed";
    static public final String CONF_CHANGED = "conf_changed";
    static public final String RECORD_STATE_CHANGED = "record_state";


    public CallManagerCallBack(Context context) {
        mContext = context;
    }

    @Override
    public void on_new_call_created(String accountID, String callID, String to) {
        Log.d(TAG, "on_new_call_created(" + accountID + ", " + callID + ", " + to + ")");

    }

    @Override
    public void on_call_state_changed(String callID, String state) {
        Log.d(TAG, "on_call_state_changed(" + callID + ", " + state + ")");
        Bundle bundle = new Bundle();
        bundle.putString("CallID", callID);
        bundle.putString("State", state);
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putString("CallID", callID);
        bundle.putString("From", from);
        Intent intent = new Intent(INCOMING_CALL);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newcall", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    @Override
    public void on_transfer_state_changed(String result){
        Log.w(TAG,"TRANSFER STATE CHANGED:"+result);
    }
    
    @Override
    public void on_conference_created(String confID){
        Log.w(TAG,"CONFERENCE CREATED:"+confID);
        Intent intent = new Intent(CONF_CREATED);
        intent.putExtra("confID", confID);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    @Override
    public void on_incoming_message(String ID, String from, String msg){
        Log.w(TAG,"on_incoming_message:"+msg);
        Bundle bundle = new Bundle();

        bundle.putString("CallID", ID);
        bundle.putString("From", from);
        bundle.putString("Msg", msg);
        Intent intent = new Intent(INCOMING_TEXT); 
        intent.putExtra("com.savoirfairelinux.sflphone.service.newtext", bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    @Override
    public void on_conference_removed(String confID){
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("confID", confID);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    @Override
    public void on_conference_state_changed(String confID, String state){
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    
    @Override
    public void on_record_playback_filepath(String id, String filename){
        Intent intent = new Intent(RECORD_STATE_CHANGED);
        intent.putExtra("id", id);
        intent.putExtra("file", filename);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

}
