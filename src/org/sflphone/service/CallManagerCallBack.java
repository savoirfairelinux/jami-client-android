package org.sflphone.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.sflphone.client.CallActivity;
import org.sflphone.model.*;
import org.sflphone.utils.SwigNativeConverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class CallManagerCallBack extends Callback {

    private static final String TAG = "CallManagerCallBack";
    private SipService mService;

    static public final String CALL_STATE_CHANGED = "call-state-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";
    static public final String CONF_CREATED = "conf_created";
    static public final String CONF_REMOVED = "conf_removed";
    static public final String CONF_CHANGED = "conf_changed";
    static public final String RECORD_STATE_CHANGED = "record_state";

    static public final String ZRTP_ON = "secure_zrtp_on";
    static public final String ZRTP_OFF = "secure_zrtp_off";
    static public final String DISPLAY_SAS = "display_sas";


    public CallManagerCallBack(SipService context) {
        mService = context;
    }

    @Override
    public void on_call_state_changed(String callID, String newState) {
        Log.d(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");
        Bundle bundle = new Bundle();
        bundle.putString("CallID", callID);
        bundle.putString("State", newState);
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);

        if (newState.equals("RINGING")) {
            try {
                mService.getConferences().get(callID).setCallState(callID, SipCall.state.CALL_STATE_RINGING);
            } catch (NullPointerException e) {
                if (mService.getConferences() == null) {
                    return;
                }
                if (mService.getConferences().get(callID) == null) {
                    Log.e(TAG, "call for " + callID + " is null");
                    return;
                }
            }

        } else if (newState.equals("CURRENT")) {
            if (mService.getConferences().get(callID) != null) {
                mService.getConferences().get(callID).setCallState(callID, SipCall.state.CALL_STATE_CURRENT);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
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
            if (mService.getConferences().get(callID) != null) {
                if (mService.getConferences().get(callID).isRinging()
                        && mService.getConferences().get(callID).isIncoming())
                    mService.mNotificationManager.publishMissedCallNotification(mService.getConferences().get(callID));

                mService.mHistoryManager.insertNewEntry(mService.getConferences().get(callID));
                mService.getConferences().remove(callID);
            } else {

                Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID)) {
                            mService.mHistoryManager.insertNewEntry(c);
                            mService.getConferences().get(tmp.getId()).removeParticipant(c);
                            break;
                        }
                    }
                }
            }

        } else if (newState.equals("BUSY")) {
            mService.getConferences().remove(callID);
        } else if (newState.equals("FAILURE")) {
            mService.getConferences().remove(callID);
        } else if (newState.equals("HOLD")) {
            if (mService.getConferences().get(callID) != null) {
                mService.getConferences().get(callID).setCallState(callID, SipCall.state.CALL_STATE_HOLD);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID))
                            c.setCallState(SipCall.state.CALL_STATE_HOLD);
                    }
                }
            }
        } else if (newState.equals("UNHOLD")) {

            if (mService.getConferences().get(callID) != null) {
                mService.getConferences().get(callID).setCallState(callID, SipCall.state.CALL_STATE_CURRENT);
            } else {
                // Check if call is in a conference
                Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(callID))
                            c.setCallState(SipCall.state.CALL_STATE_CURRENT);
                    }
                }
            }
        } else {
            mService.getConferences().get(callID).setCallState(callID, SipCall.state.CALL_STATE_NONE);
        }
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();
        try {
            StringMap details = mService.getConfigurationManagerJNI().getAccountDetails(accountID);
            VectMap credentials = mService.getConfigurationManagerJNI().getCredentials(accountID);
            Account acc = new Account(accountID, SwigNativeConverter.convertAccountToNative(details), SwigNativeConverter.convertCredentialsToNative(credentials));
            callBuilder.startCallCreation(callID).setAccount(acc).setCallState(SipCall.state.CALL_STATE_RINGING)
                    .setCallType(SipCall.direction.CALL_TYPE_INCOMING);
            callBuilder.setContact(CallContact.ContactBuilder.buildUnknownContact(from));

            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            toSend.setClass(mService, CallActivity.class);
            toSend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            SipCall newCall = callBuilder.build();
            StringMap callDetails = mService.getCallManagerJNI().getCallDetails(callID);

            newCall.setTimestampStart_(Long.parseLong(callDetails.get(ServiceConstants.call.TIMESTAMP_START)));

            Conference toAdd = new Conference(newCall);

            mService.getConferences().put(toAdd.getId(), toAdd);

            Bundle bundle = new Bundle();

            bundle.putParcelable("conference", toAdd);
            toSend.putExtra("resuming", false);
            toSend.putExtras(bundle);
            mService.startActivity(toSend);
            mService.mMediaManager.startRing("");
            mService.mMediaManager.obtainAudioFocus(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void on_transfer_state_changed(String result) {
        Log.w(TAG, "TRANSFER STATE CHANGED:" + result);
    }

    @Override
    public void on_conference_created(final String confID) {
        Log.w(TAG, "CONFERENCE CREATED:" + confID);
        Intent intent = new Intent(CONF_CREATED);
        Conference created = new Conference(confID);

        StringVect all_participants = mService.getCallManagerJNI().getParticipantList(confID);
        Log.w(TAG, "all_participants:" + all_participants.size());
        for (int i = 0; i < all_participants.size(); ++i) {
            if (mService.getConferences().get(all_participants.get(i)) != null) {
                created.addParticipant(mService.getConferences().get(all_participants.get(i)).getCallById(all_participants.get(i)));
                mService.getConferences().remove(all_participants.get(i));
            } else {
                Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
                while (it.hasNext()) {
                    Conference tmp = it.next().getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(all_participants.get(i))) {
                            created.addParticipant(c);
                            mService.getConferences().get(tmp.getId()).removeParticipant(c);
                        }
                    }
                }
            }
        }
        intent.putExtra("newconf", created);
        mService.getConferences().put(created.getId(), created);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_incoming_message(String ID, String from, String msg) {
        Log.w(TAG, "on_incoming_message:" + msg);
        Bundle bundle = new Bundle();

        bundle.putString("CallID", ID);
        bundle.putString("From", from);
        bundle.putString("Msg", msg);
        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("com.savoirfairelinux.sflphone.service.newtext", bundle);


        if (mService.getConferences().get(ID) != null) {
            mService.getConferences().get(ID).addSipMessage(new SipMessage(true, msg));
        } else {
            Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                for (SipCall c : tmp.getParticipants()) {
                    if (c.getCallId().contentEquals(ID)) {
                        mService.getConferences().get(tmp.getId()).addSipMessage(new SipMessage(true, msg));
                    }
                }
            }

        }

        mService.sendBroadcast(intent);
    }

    @Override
    public void on_conference_removed(String confID) {
        Log.i(TAG, "on_conference_removed:");
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("confID", confID);

        Conference toReInsert = mService.getConferences().get(confID);
        for (SipCall call : toReInsert.getParticipants()) {
            mService.getConferences().put(call.getCallId(), new Conference(call));
        }
        mService.getConferences().remove(confID);
        mService.sendBroadcast(intent);

    }

    @Override
    public void on_conference_state_changed(String confID, String state) {
        Log.i(TAG, "on_conference_state_changed:");
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);
        mService.getConferences().get(confID).setCallState(confID, state);

        Log.i(TAG, "Received:" + intent.getAction());
        Log.i(TAG, "State:" + state);

        Conference toModify = mService.getConferences().get(confID);

        ArrayList<String> newParticipants = SwigNativeConverter.convertSwigToNative(mService.getCallManagerJNI().getParticipantList(intent.getStringExtra("confID")));

        if (toModify.getParticipants().size() < newParticipants.size()) {
            // We need to add the new participant to the conf
            for (int i = 0; i < newParticipants.size(); ++i) {
                if(toModify.getCallById(newParticipants.get(i))==null){
                    mService.addCallToConference(toModify.getId(), newParticipants.get(i));
                }
            }
        } else if (toModify.getParticipants().size() > newParticipants.size()) {
            Log.i(TAG, "toModify.getParticipants().size() > newParticipants.size()");
            for (SipCall participant : toModify.getParticipants()) {
                if (!newParticipants.contains(participant.getCallId())) {
                    mService.detachCallFromConference(toModify.getId(), participant);
                }
            }
        }

        mService.sendBroadcast(intent);
    }

    @Override
    public void on_record_playback_filepath(String id, String filename) {
        Intent intent = new Intent(RECORD_STATE_CHANGED);
        intent.putExtra("id", id);
        intent.putExtra("file", filename);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_secure_sdes_on(String callID) {
        Log.i(TAG, "on_secure_sdes_on");
    }

    @Override
    public void on_secure_sdes_off(String callID) {
        Log.i(TAG, "on_secure_sdes_off");
    }

    @Override
    public void on_secure_zrtp_on(String callID, String cipher) {
        Log.i(TAG, "on_secure_zrtp_on");
        SipCall call = mService.getCallById(callID);
        call.setSecured(true);
        Intent intent = new Intent(ZRTP_ON);
        intent.putExtra("callID", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_secure_zrtp_off(String callID) {
        Log.i(TAG, "on_secure_zrtp_off");
        SipCall call = mService.getCallById(callID);
        call.setSecured(false);
        Intent intent = new Intent(ZRTP_OFF);
        intent.putExtra("callID", callID);
        mService.sendBroadcast(intent);

    }

    @Override
    public void on_show_sas(String callID, String sas, boolean verified) {
        Log.i(TAG, "on_show_sas:"+ sas);
        Log.i(TAG, "SAS Verified:"+ verified);
        SipCall call = mService.getCallById(callID);
        call.setSAS(sas);
        call.setConfirmedSAS(verified);
        Intent intent = new Intent(DISPLAY_SAS);
        intent.putExtra("callID", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_zrtp_not_supported(String callID) {
        Log.i(TAG, "on_zrtp_not_supported");
    }

    @Override
    public void on_zrtp_negociation_failed(String callID, String reason, String severity) {
        Log.i(TAG, "on_zrtp_negociation_failed");
    }

}
