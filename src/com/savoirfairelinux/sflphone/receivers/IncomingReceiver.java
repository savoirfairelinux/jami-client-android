package com.savoirfairelinux.sflphone.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ConfigurationManagerCallback;
import com.savoirfairelinux.sflphone.service.SipService;

public class IncomingReceiver extends BroadcastReceiver{
    
    static final String TAG = IncomingReceiver.class.getSimpleName();

    SipService callback;
    
    public IncomingReceiver(SipService client){
        callback = client;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED)) {
            Log.i(TAG, "Received" + intent.getAction());
            callback.sendBroadcast(intent);
        } else if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {
            Log.i(TAG, "Received" + intent.getAction());
            callback.sendBroadcast(intent);
        } else if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_TEXT)) {
            Log.i(TAG, "Received" + intent.getAction());
            callback.sendBroadcast(intent);
        } else if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_CALL)) {
            Bundle b = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newcall");

            SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
            callBuilder.startCallCreation(b.getString("CallID")).setAccountID(b.getString("AccountID"))
                    .setCallState(SipCall.state.CALL_STATE_RINGING).setCallType(SipCall.state.CALL_TYPE_INCOMING);
            callBuilder.addContact(CallContact.ContactBuilder.buildUnknownContact(b.getString("From")));

            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            try {
                SipCall newCall = callBuilder.build();
                toSend.putExtra("newcall", newCall);
                callback.getCurrent_calls().put(newCall.getCallId(), newCall);
                callback.sendBroadcast(toSend);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

        } else if (intent.getAction().contentEquals(CallManagerCallBack.CALL_STATE_CHANGED)) {

            Bundle b = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
            String newState = b.getString("State");
            if (newState.equals("INCOMING")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_INCOMING);
            } else if (newState.equals("RINGING")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_RINGING);
            } else if (newState.equals("CURRENT")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_CURRENT);
            } else if (newState.equals("HUNGUP")) {
                callback.getCurrent_calls().remove(b.getString("CallID"));
            } else if (newState.equals("BUSY")) {
                callback.getCurrent_calls().remove(b.getString("CallID"));
            } else if (newState.equals("FAILURE")) {
                callback.getCurrent_calls().remove(b.getString("CallID"));
            } else if (newState.equals("HOLD")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_HOLD);
            } else if (newState.equals("UNHOLD")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_CURRENT);
            } else {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_NONE);
            }

            callback.sendBroadcast(intent);
        } else if (intent.getAction().contentEquals(CallManagerCallBack.NEW_CALL_CREATED)) {
            Log.i(TAG, "Received" + intent.getAction());
        }

    }
}
