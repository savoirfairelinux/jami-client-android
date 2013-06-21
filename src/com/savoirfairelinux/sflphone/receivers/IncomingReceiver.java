package com.savoirfairelinux.sflphone.receivers;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ConfigurationManagerCallback;
import com.savoirfairelinux.sflphone.service.ISipService.Stub;
import com.savoirfairelinux.sflphone.service.SipService;

public class IncomingReceiver extends BroadcastReceiver{
    
    static final String TAG = IncomingReceiver.class.getSimpleName();

    SipService callback;
    Stub mBinder;
    
    public IncomingReceiver(SipService client, Stub bind){
        callback = client;
        mBinder = bind;
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
            callBuilder.setContact(CallContact.ContactBuilder.buildUnknownContact(b.getString("From")));

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
            
        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CREATED)) {
            
            Log.i(TAG, "Received" + intent.getAction());
            Conference created = new Conference(intent.getStringExtra("confID"));
            ArrayList<String> all_participants;
            try {
                all_participants = (ArrayList<String>) mBinder.getParticipantList(intent.getStringExtra("confID"));
                for(String participant : all_participants){
                    created.getParticipants().add(callback.getCurrent_calls().get(participant));
                }
                Intent toSend = new Intent(CallManagerCallBack.CONF_CREATED);
                toSend.putExtra("newconf", created);
                callback.getCurrent_confs().put(intent.getStringExtra("confID"), created);
                callback.sendBroadcast(toSend);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            Log.i(TAG, "current_confs size " + callback.getCurrent_confs().size());
            
        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_REMOVED)) {
            
            Log.i(TAG, "Received" + intent.getAction());
            Conference toDestroy = callback.getCurrent_confs().get(intent.getStringExtra("confID"));
            for(int i = 0; i < toDestroy.getParticipants().size() ; ++i){
                callback.getCurrent_calls().put(toDestroy.getParticipants().get(i).getCallId(), toDestroy.getParticipants().get(i));
            }
            callback.getCurrent_confs().remove(intent.getStringExtra("confID"));
            Toast.makeText(callback, "Removed conf ", Toast.LENGTH_SHORT).show();
            
        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CHANGED)) {
            
            Log.i(TAG, "Received" + intent.getAction());
            callback.getCurrent_confs().get(intent.getStringExtra("confID")).setState(intent.getStringExtra("State"));
            Toast.makeText(callback, "Changing conf state: "+ intent.getStringExtra("State"), Toast.LENGTH_SHORT).show();
        }

    }
}
