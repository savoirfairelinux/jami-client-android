package org.sflphone.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.sflphone.client.CallActivity;
import org.sflphone.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CallManagerCallBack extends Callback {
    
    private static final String TAG = "CallManagerCallBack";
    private final ISipService.Stub mBinder;
    private  SipService mService;

    static public final String CALL_STATE_CHANGED = "call-state-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";
    static public final String CONF_CREATED = "conf_created";
    static public final String CONF_REMOVED = "conf_removed";
    static public final String CONF_CHANGED = "conf_changed";
    static public final String RECORD_STATE_CHANGED = "record_state";


    public CallManagerCallBack(SipService context, ISipService.Stub bind) {
        mService = context;
        mBinder = bind;
    }

    @Override
    public void on_call_state_changed(String callID, String newState) {
       Log.d(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");
        Bundle bundle = new Bundle();
        bundle.putString("CallID", callID);
        bundle.putString("State", newState);
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);

        /*try {
            if (mService.getCurrentCalls().get(callID) != null && mBinder.isConferenceParticipant(callID)) {
                mService.getCurrentCalls().remove(callID);
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }*/


        if (newState.equals("INCOMING")) {
            mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_INCOMING);
        } else if (newState.equals("RINGING")) {
            try {
                mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_RINGING);
            } catch (NullPointerException e) {
                if (mService.getCurrentCalls() == null) {
                    return;
                }
                if (mService.getCurrentCalls().get(callID) == null) {
                    Log.e(TAG, "call for " + callID + " is null");
                    return;
                }
            }

        } else if (newState.equals("CURRENT")) {
            if (mService.getCurrentCalls().get(callID) != null) {
                mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_CURRENT);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getCurrentConfs().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID))
                            c.setCallState(SipCall.state.CALL_STATE_CURRENT);
                    }
                }
            }

        } else if (newState.equals("HUNGUP")) {

            Log.d(TAG, "Hanging up " + callID);
            if (mService.getCurrentCalls().get(callID) != null) {
                if (mService.getCurrentCalls().get(callID).isRinging()
                        && mService.getCurrentCalls().get(callID).isIncoming())
                    mService.notificationManager.publishMissedCallNotification(mService.getCurrentCalls().get(callID));
                mService.getCurrentCalls().remove(callID);
            } else {
                ArrayList<Conference> it = new ArrayList<Conference>(mService.getCurrentConfs().values());

                boolean found = false;
                int i = 0;
                while (!found && i < it.size()) {
                    Conference tmp = it.get(i);

                    for (int j = 0; j < tmp.getParticipants().size(); ++j) {
                        if (tmp.getParticipants().get(j).getCallId().contentEquals(callID)) {
                            mService.getCurrentConfs().get(tmp.getId()).getParticipants().remove(tmp.getParticipants().get(j));
                            found = true;
                        }

                    }
                    ++i;

                }
            }

            mService.sendBroadcast(intent);

        } else if (newState.equals("BUSY")) {
            mService.getCurrentCalls().remove(callID);
        } else if (newState.equals("FAILURE")) {
            mService.getCurrentCalls().remove(callID);
        } else if (newState.equals("HOLD")) {
            if (mService.getCurrentCalls().get(callID) != null) {
                mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_HOLD);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getCurrentConfs().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID))
                            c.setCallState(SipCall.state.CALL_STATE_HOLD);
                    }
                }
            }
        } else if (newState.equals("UNHOLD")) {

            if (mService.getCurrentCalls().get(callID) != null) {
                mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_CURRENT);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getCurrentConfs().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID))
                            c.setCallState(SipCall.state.CALL_STATE_CURRENT);
                    }
                }
            }
        } else {
            mService.getCurrentCalls().get(callID).setCallState(SipCall.state.CALL_STATE_NONE);
        }


        Log.d(TAG, "Hanging up " + callID);
        mService.sendBroadcast(intent);

    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
        try {
            HashMap<String, String> details = (HashMap<String, String>) mBinder.getAccountDetails(accountID);
            ArrayList<HashMap<String, String>> credentials = (ArrayList<HashMap<String, String>>) mBinder
                    .getCredentials(accountID);
            Account acc = new Account(accountID, details, credentials);
            callBuilder.startCallCreation(callID).setAccount(acc).setCallState(SipCall.state.CALL_STATE_RINGING)
                    .setCallType(SipCall.state.CALL_TYPE_INCOMING);
            callBuilder.setContact(CallContact.ContactBuilder.buildUnknownContact(from));

            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            toSend.setClass(mService, CallActivity.class);
            toSend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            SipCall newCall = callBuilder.build();
            toSend.putExtra("newcall", newCall);
            HashMap<String, String> callDetails = (HashMap<String, String>) mBinder.getCallDetails(callID);

            newCall.setTimestamp_start(Long.parseLong(callDetails.get(ServiceConstants.call.TIMESTAMP_START)));
            mService.getCurrentCalls().put(newCall.getCallId(), newCall);

            Bundle bundle = new Bundle();
            Conference tmp = new Conference("-1");

            tmp.getParticipants().add(newCall);

            bundle.putParcelable("conference", tmp);
            toSend.putExtra("resuming", false);
            toSend.putExtras(bundle);
            mService.startActivity(toSend);
            mService.mediaManager.startRing("");
            mService.mediaManager.obtainAudioFocus(true);
        } catch (RemoteException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void on_transfer_state_changed(String result){
        Log.w(TAG,"TRANSFER STATE CHANGED:"+result);
    }
    
    @Override
    public void on_conference_created(String confID){
        Log.w(TAG,"CONFERENCE CREATED:"+confID);
        Intent intent = new Intent(CONF_CREATED);
        Conference created = new Conference(confID);

        try {
            ArrayList<String> all_participants = (ArrayList<String>) mBinder.getParticipantList(intent.getStringExtra("confID"));
            for (String participant : all_participants) {
                created.getParticipants().add(mService.getCurrentCalls().get(participant));
                mService.getCurrentCalls().remove(participant);
            }
            intent.putExtra("newconf", created);
            mService.getCurrentConfs().put(confID, created);
            mService.sendBroadcast(intent);
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
        Log.i(TAG, "current_confs size " + mService.getCurrentConfs().size());
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


        if (mService.getCurrentCalls().get(ID) != null) {
            mService.getCurrentCalls().get(ID).addSipMessage(new SipMessage(true, msg));
        } else if (mService.getCurrentConfs().get(ID) != null) {
            mService.getCurrentConfs().get(ID).addSipMessage(new SipMessage(true, msg));
        } else
            return;

        mService.sendBroadcast(intent);
    }
    
    @Override
    public void on_conference_removed(String confID){
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("confID", confID);

        Conference toDestroy = mService.getCurrentConfs().get(confID);
        for (int i = 0; i < toDestroy.getParticipants().size(); ++i) {
            mService.getCurrentCalls().put(toDestroy.getParticipants().get(i).getCallId(), toDestroy.getParticipants().get(i));
        }
        mService.getCurrentConfs().remove(confID);
        mService.sendBroadcast(intent);

    }
    
    @Override
    public void on_conference_state_changed(String confID, String state){
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);
        ArrayList<String> all_participants;

        try {
            all_participants = (ArrayList<String>) mBinder.getParticipantList(intent.getStringExtra("confID"));
            for (String participant : all_participants) {
                if (mService.getCurrentConfs().get(confID).getParticipants().size() < all_participants.size()
                        && mService.getCurrentCalls().get(participant) != null) { // We need to add the new participant to the conf
                    mService.getCurrentConfs().get(confID).getParticipants()
                            .add(mService.getCurrentCalls().get(participant));
                    mService.getCurrentCalls().remove(participant);
                    mService.getCurrentConfs().get(confID).setState(intent.getStringExtra("State"));
                    mService.sendBroadcast(intent);
                    return;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Received" + intent.getAction());
        if (mService.getCurrentConfs().get(confID) != null) {
            mService.getCurrentConfs().get(confID).setState(intent.getStringExtra("State"));
            mService.sendBroadcast(intent);
        }
    }
    
    @Override
    public void on_record_playback_filepath(String id, String filename){
        Intent intent = new Intent(RECORD_STATE_CHANGED);
        intent.putExtra("id", id);
        intent.putExtra("file", filename);
        LocalBroadcastManager.getInstance(mService).sendBroadcast(intent);
    }

}
