package org.sflphone.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.client.CallActivity;
import org.sflphone.model.*;
import org.sflphone.utils.SwigNativeConverter;

import java.util.ArrayList;
import java.util.HashMap;
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
    static public final String ZRTP_NEGOTIATION_FAILED = "zrtp_nego_failed";
    static public final String ZRTP_NOT_SUPPORTED = "zrtp_not_supported";


    public CallManagerCallBack(SipService context) {
        mService = context;
    }

    @Override
    public void on_call_state_changed(String callID, String newState) {
        Log.d(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");

        Conference toUpdate = findConference(callID);

        if (toUpdate == null) {
            return;
        }

        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("CallID", callID);
        intent.putExtra("State", newState);

        if (newState.equals("RINGING")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_RINGING);
        } else if (newState.equals("CURRENT")) {
            if(toUpdate.isRinging()){
                toUpdate.getCallById(callID).setTimestampStart_(System.currentTimeMillis());
            }
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_CURRENT);
        } else if (newState.equals("HUNGUP")) {
            Log.d(TAG, "Hanging up " + callID);
            SipCall call = toUpdate.getCallById(callID);
            if (!toUpdate.hasMultipleParticipants()) {
                if (toUpdate.isRinging() && toUpdate.isIncoming()) {
                    mService.mNotificationManager.publishMissedCallNotification(mService.getConferences().get(callID));
                }
                toUpdate.setCallState(callID, SipCall.state.CALL_STATE_HUNGUP);
                mService.mHistoryManager.insertNewEntry(toUpdate);
                mService.getConferences().remove(toUpdate.getId());
                Log.e(TAG, "Conferences :"+ mService.getConferences().size());
                Log.e(TAG, "toUpdate.getParticipants() :"+ toUpdate.getParticipants().size());
            } else {
                toUpdate.setCallState(callID, SipCall.state.CALL_STATE_HUNGUP);
                mService.mHistoryManager.insertNewEntry(call);
            }
        } else if (newState.equals("BUSY")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_BUSY);
            mService.getConferences().remove(toUpdate.getId());
        } else if (newState.equals("FAILURE")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_FAILURE);
            mService.getConferences().remove(toUpdate.getId());
        } else if (newState.equals("HOLD")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_HOLD);
        } else if (newState.equals("UNHOLD")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_CURRENT);
        }
        intent.putExtra("conference", toUpdate);
        mService.sendBroadcast(intent);
    }

    private Conference findConference(String callID) {
        Conference result = null;
        if (mService.getConferences().get(callID) != null) {
            result = mService.getConferences().get(callID);
        } else {
            Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                for (SipCall c : tmp.getParticipants()) {
                    if (c.getCallId().contentEquals(callID)) {
                        result = tmp;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void on_incoming_call(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        try {
            StringMap details = mService.getConfigurationManagerJNI().getAccountDetails(accountID);
            VectMap credentials = mService.getConfigurationManagerJNI().getCredentials(accountID);
            Account acc = new Account(accountID, SwigNativeConverter.convertAccountToNative(details), SwigNativeConverter.convertCredentialsToNative(credentials));

            Bundle args = new Bundle();
            args.putString(SipCall.ID, callID);
            args.putParcelable(SipCall.ACCOUNT, acc);
            args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_RINGING);
            args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_INCOMING);

            CallContact unknow = CallContact.ContactBuilder.buildUnknownContact(from);
            args.putParcelable(SipCall.CONTACT, unknow);

            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            toSend.setClass(mService, CallActivity.class);
            toSend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            SipCall newCall = new SipCall(args);

            newCall.setTimestampStart_(System.currentTimeMillis());

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
        intent.putExtra("conference", created);
        intent.putExtra("confID", created.getId());
        mService.getConferences().put(created.getId(), created);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_incoming_message(String ID, String from, String msg) {
        Log.w(TAG, "on_incoming_message:" + msg);
        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("CallID", ID);
        intent.putExtra("From", from);
        intent.putExtra("Msg", msg);

        if (mService.getConferences().get(ID) != null) {
            mService.getConferences().get(ID).addSipMessage(new SipMessage(true, msg));
            intent.putExtra("conference", mService.getConferences().get(ID));
        } else {
            Iterator<Map.Entry<String, Conference>> it = mService.getConferences().entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                for (SipCall c : tmp.getParticipants()) {
                    if (c.getCallId().contentEquals(ID)) {
                        mService.getConferences().get(tmp.getId()).addSipMessage(new SipMessage(true, msg));
                        intent.putExtra("conference", tmp);
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
        intent.putExtra("conference", mService.getConferences().get(confID));
        mService.getConferences().remove(confID);
        mService.sendBroadcast(intent);

    }

    @Override
    public void on_conference_state_changed(String confID, String state) {
        Log.i(TAG, "on_conference_state_changed:");
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);


        Log.i(TAG, "Received:" + intent.getAction());
        Log.i(TAG, "State:" + state);

        Conference toModify = mService.getConferences().get(confID);
        toModify.setCallState(confID, state);

        ArrayList<String> newParticipants = SwigNativeConverter.convertSwigToNative(mService.getCallManagerJNI().getParticipantList(intent.getStringExtra("confID")));

        if (toModify.getParticipants().size() < newParticipants.size()) {
            // We need to add the new participant to the conf
            for (int i = 0; i < newParticipants.size(); ++i) {
                if (toModify.getCallById(newParticipants.get(i)) == null) {
                    mService.addCallToConference(toModify.getId(), newParticipants.get(i));
                }
            }
        } else if (toModify.getParticipants().size() > newParticipants.size()) {
            Log.i(TAG, "toModify.getParticipants().size() > newParticipants.size()");
            for (SipCall participant : toModify.getParticipants()) {
                if (!newParticipants.contains(participant.getCallId())) {
                    mService.detachCallFromConference(toModify.getId(), participant);
                    break;
                }
            }
        }

        mService.sendBroadcast(intent);
    }

    @Override
    public void on_record_playback_filepath(String id, String filename) {
        Intent intent = new Intent(RECORD_STATE_CHANGED);
        intent.putExtra("callID", id);
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
        Bundle secureArgs = new Bundle();
        HashMap<String, String> details = SwigNativeConverter.convertAccountToNative(mService.getConfigurationManagerJNI().getAccountDetails(call.getAccount().getAccountID()));
        secureArgs.putBoolean(SecureSipCall.DISPLAY_SAS, details.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS).contentEquals("true"));
        secureArgs.putBoolean(SecureSipCall.DISPLAY_SAS_ONCE, details.get(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE).contentEquals("true"));
        secureArgs.putBoolean(SecureSipCall.DISPLAY_WARNING_ZRTP_NOT_SUPPORTED, details.get(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING).contentEquals("true"));
        SecureSipCall replace = new SecureSipCall(call, secureArgs);
        mService.replaceCall(replace);

        Intent intent = new Intent(ZRTP_ON);
        intent.putExtra("callID", replace.getCallId());
        intent.putExtra("conference", findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_secure_zrtp_off(String callID) {
        Log.i(TAG, "on_secure_zrtp_off");
        SipCall call = mService.getCallById(callID);

        if (call != null && call instanceof SecureSipCall) {
            SipCall replace = new SipCall(call.getBundle());
            mService.replaceCall(replace);
            Intent intent = new Intent(ZRTP_OFF);
            intent.putExtra("callID", callID);
            intent.putExtra("conference", findConference(callID));
            mService.sendBroadcast(intent);
        }

    }

    @Override
    public void on_show_sas(String callID, String sas, boolean verified) {
        Log.i(TAG, "on_show_sas:" + sas);
        Log.i(TAG, "SAS Verified:" + verified);

        Intent intent = new Intent(DISPLAY_SAS);
        intent.putExtra("callID", callID);
        intent.putExtra("SAS", sas);
        intent.putExtra("verified", verified);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        intent.putExtra("conference", findConference(callID));

        call.setSAS(sas);
        call.setConfirmedSAS(verified);

        mService.sendBroadcast(intent);
    }

    @Override
    public void on_zrtp_not_supported(String callID) {
        Log.i(TAG, "on_zrtp_not_supported");
        Intent intent = new Intent(ZRTP_NOT_SUPPORTED);
        intent.putExtra("callID", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void on_zrtp_negociation_failed(String callID, String reason, String severity) {
        Log.i(TAG, "on_zrtp_negociation_failed");
        Intent intent = new Intent(ZRTP_NEGOTIATION_FAILED);
        intent.putExtra("callID", callID);
        intent.putExtra("reason", reason);
        intent.putExtra("severity", severity);
        mService.sendBroadcast(intent);
    }

}
