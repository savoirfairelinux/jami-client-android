package cx.ring.service;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;

import cx.ring.history.HistoryText;
import cx.ring.model.TextMessage;

public class CallManagerCallBack extends Callback {

    private static final String TAG = "CallManagerCallBack";
    private DRingService mService;

    static public final String CALL_STATE_CHANGED = "call-State-changed";
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


    public CallManagerCallBack(DRingService context) {
        super();
        mService = context;
    }

    @Override
    public void callStateChanged(String callID, String newState, int detail_code) {
        Log.w(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("call", callID);
        intent.putExtra("state", newState);
        intent.putExtra("detail_code", detail_code);
        try {
            intent.putExtra("details", (HashMap)mService.mBinder.getCallDetails(callID));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mService.sendBroadcast(intent);
    }


    @Override
    public void incomingCall(String accountID, String callID, String from) {
        Log.w(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        try {
            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            toSend.putExtra("call", callID);
            toSend.putExtra("account", accountID);
            toSend.putExtra("from", from);
            toSend.putExtra("resuming", false);
            mService.sendBroadcast(toSend);

            mService.mMediaManager.obtainAudioFocus(true);
            mService.mMediaManager.startRing("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void conferenceCreated(final String confID) {
        Log.w(TAG, "CONFERENCE CREATED:" + confID);
        Intent intent = new Intent(CONF_CREATED);
        intent.putExtra("conference", confID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void incomingMessage(String id, String from, StringMap messages) {
        Log.w(TAG, "on_incoming_message:" + messages);

        String msg = messages.get("text/plain");
        if (msg == null)
            return;

        TextMessage message = new TextMessage(true, msg, from, id, null/*, conf.hasMultipleParticipants() ? null : conf.getParticipants().get(0).getAccount()*/);

        mService.mHistoryManager.insertNewTextMessage(new HistoryText(message));

        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("call", id);
        intent.putExtra("from", from);
        intent.putExtra("txt", message);
        //intent.putExtra("conference", conf);
        mService.sendBroadcast(intent);
    }

    @Override
    public void conferenceRemoved(String confID) {
        Log.i(TAG, "on_conference_removed:");
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("conference", confID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void conferenceChanged(String confID, String state) {
        Log.i(TAG, "on_conference_state_changed:");
        Log.i(TAG, "State:" + state);

        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("conference", confID);
        intent.putExtra("state", state);
        mService.sendBroadcast(intent);
    }

    @Override
    public void recordPlaybackFilepath(String id, String filename) {
        Intent intent = new Intent();
        intent.putExtra("call", id);
        intent.putExtra("file", filename);
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureSdesOn(String callID) {
        Log.i(TAG, "on_secure_sdes_on");
        /*SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(true);*/
    }

    @Override
    public void secureSdesOff(String callID) {
        Log.i(TAG, "on_secure_sdes_off");
        /*SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(false);*/
    }

    @Override
    public void secureZrtpOn(String callID, String cipher) {
        Log.i(TAG, "on_secure_zrtp_on");
        Intent intent = new Intent(ZRTP_ON);
        intent.putExtra("call", callID);
        intent.putExtra("cipher", cipher);
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureZrtpOff(String callID) {
        Log.i(TAG, "on_secure_zrtp_off");
        Intent intent = new Intent(ZRTP_OFF);
        intent.putExtra("call", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void showSAS(String callID, String sas, int verified) {
        Log.i(TAG, "on_show_sas:" + sas);
        Intent intent = new Intent(DISPLAY_SAS);
        intent.putExtra("call", callID);
        intent.putExtra("sas", sas);
        intent.putExtra("verified", verified);
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNotSuppOther(String callID) {
        Log.i(TAG, "on_zrtp_not_supported");
        Intent intent = new Intent(ZRTP_NOT_SUPPORTED);
        intent.putExtra("call", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNegotiationFailed(String callID, String reason, String severity) {
        Log.i(TAG, "on_zrtp_negociation_failed");
        Intent intent = new Intent(ZRTP_NEGOTIATION_FAILED);
        intent.putExtra("call", callID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void onRtcpReportReceived(String callID, IntegerMap stats) {
        Log.i(TAG, "on_rtcp_report_received");
        Intent intent = new Intent(RTCP_REPORT_RECEIVED);
        mService.sendBroadcast(intent);
    }

}
