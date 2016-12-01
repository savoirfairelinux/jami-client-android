package cx.ring.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.StringMap;
import cx.ring.model.DaemonEvent;
import cx.ring.model.SipCall;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.ProfileChunk;
import cx.ring.utils.VCardUtils;

public class CallManagerCallBack implements Observer<DaemonEvent> {

    private static final String TAG = CallManagerCallBack.class.getName();

    private Context mContext;
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


    public CallManagerCallBack(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void update(Observable o, DaemonEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case CALL_STATE_CHANGED:
                callStateChanged(
                        event.getEventInput(DaemonEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.STATE, String.class),
                        event.getEventInput(DaemonEvent.EventInput.DETAIL_CODE, Integer.class)
                );
                break;
            case INCOMING_CALL:
                incomingCall(
                        event.getEventInput(DaemonEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.FROM, String.class)
                );
                break;
            case INCOMING_MESSAGE:
                incomingMessage(
                        event.getEventInput(DaemonEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.FROM, String.class),
                        event.getEventInput(DaemonEvent.EventInput.MESSAGES, StringMap.class)
                );
                break;
            case CONFERENCE_CREATED:
                conferenceCreated(event.getEventInput(DaemonEvent.EventInput.CONF_ID, String.class));
                break;
            case CONFERENCE_CHANGED:
                conferenceChanged(
                        event.getEventInput(DaemonEvent.EventInput.CONF_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.STATE, String.class)
                );
                break;
            case CONFERENCE_REMOVED:
                conferenceRemoved(event.getEventInput(DaemonEvent.EventInput.CONF_ID, String.class));
                break;
            case RECORD_PLAYBACK_FILEPATH:
                // todo
                break;
            case RTCP_REPORT_RECEIVED:
                onRtcpReportReceived(
                        event.getEventInput(DaemonEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.STATS, IntegerMap.class));
                break;
            default:
                Log.i(TAG, "Unkown daemon event");
                break;
        }
    }

    private void callStateChanged(String callId, String newState, int detailCode) {
        if (newState.equals(SipCall.stateToString(SipCall.State.INCOMING)) ||
                newState.equals(SipCall.stateToString(SipCall.State.OVER))) {
            this.mProfileChunk = null;
        }
        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("call", callId);
        intent.putExtra("state", newState);
        intent.putExtra("detail_code", detailCode);
        mContext.sendBroadcast(intent);
    }

    private void incomingCall(String accountId, String callId, String from) {
        Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
        toSend.putExtra("call", callId);
        toSend.putExtra("account", accountId);
        toSend.putExtra("from", from);
        toSend.putExtra("resuming", false);
        mContext.sendBroadcast(toSend);
    }

    private void conferenceCreated(final String confId) {
        Intent intent = new Intent(CONF_CREATED);
        intent.putExtra("conference", confId);
        mContext.sendBroadcast(intent);
    }

    private void incomingMessage(String callId, String from, StringMap messages) {
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
                                mContext.getApplicationContext().getFilesDir());

                        Intent intent = new Intent(VCARD_COMPLETED);
                        intent.putExtra("filename", filename);
                        mContext.sendBroadcast(intent);
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
        intent.putExtra("call", callId);
        mContext.sendBroadcast(intent);
    }

    private void conferenceRemoved(String confId) {
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("conference", confId);
        mContext.sendBroadcast(intent);
    }

    private void conferenceChanged(String confId, String state) {
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("conference", confId);
        intent.putExtra("state", state);
        mContext.sendBroadcast(intent);
    }

    private void recordPlaybackFilepath(String id, String filename) {
        Intent intent = new Intent();
        intent.putExtra("call", id);
        intent.putExtra("file", filename);
        mContext.sendBroadcast(intent);
    }

    private void onRtcpReportReceived(String callId, IntegerMap stats) {
        Intent intent = new Intent(RTCP_REPORT_RECEIVED);
        mContext.sendBroadcast(intent);
    }
}
