package cx.ring.service;

import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

import cx.ring.daemon.Callback;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.StringMap;
import cx.ring.model.SipCall;
import cx.ring.utils.ProfileChunk;
import cx.ring.utils.VCardUtils;

public class CallManagerCallBack extends Callback {

    private static final String TAG = "CallManagerCallBack";
    private DRingService mService;
    private ProfileChunk mProfileChunk;

    static public final String CALL_STATE_CHANGED = "call-State-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";
    static public final String VCARD_COMPLETED = "vcard-completed";
    static public final String CONF_CREATED = "conf_created";
    static public final String CONF_REMOVED = "conf_removed";
    static public final String CONF_CHANGED = "conf_changed";
    static public final String RECORD_STATE_CHANGED = "record_state";

    static public final String RTCP_REPORT_RECEIVED = "on_rtcp_report_received";


    public CallManagerCallBack(DRingService context) {
        super();
        mService = context;
    }

    @Override
    public void callStateChanged(String callID, String newState, int detail_code) {
        Log.w(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");
        if (newState.equals(SipCall.stateToString(SipCall.State.INCOMING)) ||
                newState.equals(SipCall.stateToString(SipCall.State.OVER))) {
            this.mProfileChunk = null;
        }
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("call", callID);
        intent.putExtra("state", newState);
        intent.putExtra("detail_code", detail_code);
        mService.sendBroadcast(intent);
    }

    @Override
    public void incomingCall(String accountID, String callID, String from) {
        Log.w(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");
        Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
        toSend.putExtra("call", callID);
        toSend.putExtra("account", accountID);
        toSend.putExtra("from", from);
        toSend.putExtra("resuming", false);
        mService.sendBroadcast(toSend);
    }

    @Override
    public void conferenceCreated(final String confID) {
        Log.w(TAG, "CONFERENCE CREATED:" + confID);
        Intent intent = new Intent(CONF_CREATED);
        intent.putExtra("conference", confID);
        mService.sendBroadcast(intent);
    }

    @Override
    public void incomingMessage(String call_id, String from, StringMap messages) {
        String msg = null;
        final String textPlainMime = "text/plain";
        final String ringProfileVCardMime = "x-ring/ring.profile.vcard";

        if (messages != null) {

            String origin = messages.keys().toString().replace("[", "");
            origin = origin.replace("]", "");
            String[] elements = origin.split(";");

            HashMap<String, String> messageKeyValue = VCardUtils.parseMimeAttributes(elements);

            if (messageKeyValue != null && messageKeyValue.containsKey(VCardUtils.VCARD_KEY_MIME_TYPE) &&
                    messageKeyValue.get(VCardUtils.VCARD_KEY_MIME_TYPE).equals(textPlainMime)) {
                msg = messages.getRaw(textPlainMime).toJavaString();
            } else if (messageKeyValue != null && messageKeyValue.containsKey(VCardUtils.VCARD_KEY_MIME_TYPE) &&
                    messageKeyValue.get(VCardUtils.VCARD_KEY_MIME_TYPE).equals(ringProfileVCardMime)) {
                int part = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_PART));
                int nbPart = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_OF));
                if (mProfileChunk == null) {
                    mProfileChunk = new ProfileChunk(nbPart);
                }
                if (messages.keys() != null && messages.keys().size() > 0) {
                    String content = messages.getRaw(messages.keys().get(0)).toJavaString();
                    mProfileChunk.addPartAtIndex(content, part);
                }
                if (mProfileChunk.isProfileComplete()) {
                    Log.d(TAG, "Complete Profile: " + mProfileChunk.getCompleteProfile());
                    String splitFrom[] = from.split("@");
                    if (splitFrom.length == 2) {
                        String filename = splitFrom[0] + ".vcf";
                        VCardUtils.savePeerProfileToDisk(mProfileChunk.getCompleteProfile(),
                                filename,
                                this.mService.getApplicationContext().getFilesDir());

                        Intent intent = new Intent(VCARD_COMPLETED);
                        intent.putExtra("filename", filename);
                        mService.sendBroadcast(intent);
                    }
                }
            } else if (messages.has_key(textPlainMime)) {
                msg = messages.getRaw(textPlainMime).toJavaString();
            }
        }

        if (msg == null) {
            return;
        }

        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("txt", msg);
        intent.putExtra("from", from);
        intent.putExtra("call", call_id);
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
    public void onRtcpReportReceived(String callID, IntegerMap stats) {
        Log.i(TAG, "on_rtcp_report_received");
        Intent intent = new Intent(RTCP_REPORT_RECEIVED);
        mService.sendBroadcast(intent);
    }

}
