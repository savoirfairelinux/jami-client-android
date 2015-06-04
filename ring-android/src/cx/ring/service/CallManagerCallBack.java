package cx.ring.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import cx.ring.client.CallActivity;
import cx.ring.model.account.Account;
import cx.ring.utils.SwigNativeConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.model.SipMessage;

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

    static public final String RTCP_REPORT_RECEIVED = "on_rtcp_report_received";


    public CallManagerCallBack(SipService context) {
        super();
        mService = context;
    }

    @Override
    public void callStateChanged(String callID, String newState, int detail_code) {
        Log.d(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");

        Conference toUpdate = mService.findConference(callID);

        if (toUpdate == null) {
            return;
        }

        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("CallID", callID);
        intent.putExtra("State", newState);
        intent.putExtra("DetailCode", detail_code);

        if (newState.equals("RINGING")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_RINGING);
        } else if (newState.equals("CURRENT")) {
            if (toUpdate.isRinging()) {
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
            Ringservice.hangUp(callID);
        } else if (newState.equals("HOLD")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_HOLD);
        } else if (newState.equals("UNHOLD")) {
            toUpdate.setCallState(callID, SipCall.state.CALL_STATE_CURRENT);
        }
        intent.putExtra("conference", toUpdate);
        mService.sendBroadcast(intent);
    }


    @Override
    public void incomingCall(String accountID, String callID, String from) {
        Log.d(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        try {
            StringMap details = Ringservice.getAccountDetails(accountID);
            VectMap credentials = Ringservice.getCredentials(accountID);
            Account acc = new Account(accountID, details.toNative(), credentials.toNative());

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

            Conference toAdd;
            if (acc.useSecureLayer()) {
               SecureSipCall secureCall = new SecureSipCall(newCall);
                toAdd = new Conference(secureCall);
            } else {
                toAdd = new Conference(newCall);
            }

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
    public void conferenceCreated(final String confID) {
        Log.w(TAG, "CONFERENCE CREATED:" + confID);
        Intent intent = new Intent(CONF_CREATED);
        Conference created = new Conference(confID);

        StringVect all_participants = Ringservice.getParticipantList(confID);
        Log.w(TAG, "all_participants:" + all_participants.size());
        for (int i = 0; i < all_participants.size(); ++i) {
            if (mService.getConferences().get(all_participants.get(i)) != null) {
                created.addParticipant(mService.getConferences().get(all_participants.get(i)).getCallById(all_participants.get(i)));
                mService.getConferences().remove(all_participants.get(i));
            } else {
                for (Map.Entry<String, Conference> stringConferenceEntry : mService.getConferences().entrySet()) {
                    Conference tmp = stringConferenceEntry.getValue();
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
    public void incomingMessage(String ID, String from, String msg) {
        Log.w(TAG, "on_incoming_message:" + msg);
        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("CallID", ID);
        intent.putExtra("From", from);
        intent.putExtra("Msg", msg);

        if (mService.getConferences().get(ID) != null) {
            mService.getConferences().get(ID).addSipMessage(new SipMessage(true, msg));
            intent.putExtra("conference", mService.getConferences().get(ID));
        } else {
            for (Map.Entry<String, Conference> stringConferenceEntry : mService.getConferences().entrySet()) {
                Conference tmp = stringConferenceEntry.getValue();
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
    public void conferenceRemoved(String confID) {
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
    public void conferenceChanged(String confID, String state) {
        Log.i(TAG, "on_conference_state_changed:");
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);


        Log.i(TAG, "Received:" + intent.getAction());
        Log.i(TAG, "State:" + state);

        Conference toModify = mService.getConferences().get(confID);
        toModify.setCallState(confID, state);

        ArrayList<String> newParticipants = SwigNativeConverter.convertSwigToNative(Ringservice.getParticipantList(intent.getStringExtra("confID")));

        if (toModify.getParticipants().size() < newParticipants.size()) {
            // We need to add the new participant to the conf
            for (String newParticipant : newParticipants) {
                if (toModify.getCallById(newParticipant) == null) {
                    mService.addCallToConference(toModify.getId(), newParticipant);
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
    public void recordPlaybackFilepath(String id, String filename) {
        Intent intent = new Intent();
        intent.putExtra("callID", id);
        intent.putExtra("file", filename);
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureSdesOn(String callID) {
        Log.i(TAG, "on_secure_sdes_on");
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(true);
    }

    @Override
    public void secureSdesOff(String callID) {
        Log.i(TAG, "on_secure_sdes_off");
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(false);
    }

    @Override
    public void secureZrtpOn(String callID, String cipher) {
        Log.i(TAG, "on_secure_zrtp_on");
        Intent intent = new Intent(ZRTP_ON);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(true);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureZrtpOff(String callID) {
        Log.i(TAG, "on_secure_zrtp_off");
        Intent intent = new Intent(ZRTP_OFF);
        intent.putExtra("callID", callID);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        // Security can be off because call was hung up
        if (call == null)
            return;

        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void showSAS(String callID, String sas, int verified) {
        Log.i(TAG, "on_show_sas:" + sas);
        Intent intent = new Intent(DISPLAY_SAS);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setSAS(sas);
        call.sasConfirmedByZrtpLayer(verified);

        intent.putExtra("callID", callID);
        intent.putExtra("SAS", sas);
        intent.putExtra("verified", verified);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNotSuppOther(String callID) {
        Log.i(TAG, "on_zrtp_not_supported");
        Intent intent = new Intent(ZRTP_NOT_SUPPORTED);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNegotiationFailed(String callID, String reason, String severity) {
        Log.i(TAG, "on_zrtp_negociation_failed");
        Intent intent = new Intent(ZRTP_NEGOTIATION_FAILED);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void onRtcpReportReceived(String callID, IntegerMap stats) {
        Log.i(TAG, "on_rtcp_report_received");
        Intent intent = new Intent(RTCP_REPORT_RECEIVED);
        mService.sendBroadcast(intent);
    }

}
