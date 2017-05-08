package cx.ring.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cx.ring.daemon.IntegerMap;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class CallManagerCallBack implements Observer<ServiceEvent> {

    private static final String TAG = CallManagerCallBack.class.getName();

    private Context mContext;

    static public final String INCOMING_CALL = "incoming-call";

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
    public void update(Observable o, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case INCOMING_CALL:
                incomingCall(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.FROM, String.class)
                );
                break;
            case CONFERENCE_CREATED:
                conferenceCreated(event.getEventInput(ServiceEvent.EventInput.CONF_ID, String.class));
                break;
            case CONFERENCE_CHANGED:
                conferenceChanged(
                        event.getEventInput(ServiceEvent.EventInput.CONF_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, String.class)
                );
                break;
            case CONFERENCE_REMOVED:
                conferenceRemoved(event.getEventInput(ServiceEvent.EventInput.CONF_ID, String.class));
                break;
            case RECORD_PLAYBACK_FILEPATH:
                // todo
                break;
            case RTCP_REPORT_RECEIVED:
                onRtcpReportReceived(
                        event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATS, IntegerMap.class));
                break;
            default:
                Log.i(TAG, "Unkown daemon event");
                break;
        }
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
