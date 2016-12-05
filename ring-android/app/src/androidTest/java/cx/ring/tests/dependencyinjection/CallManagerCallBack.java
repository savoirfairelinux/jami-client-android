package cx.ring.tests.dependencyinjection;

import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import cx.ring.model.DaemonEvent;
import cx.ring.utils.ProfileChunk;

public class CallManagerCallBack implements Observer {

    private static final String TAG = CallManagerCallBack.class.getName();

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

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof DaemonEvent)) {
            return;
        }

        DaemonEvent event = (DaemonEvent) arg;
        switch (event.getEventType()) {
            case CALL_STATE_CHANGED:
                break;
            case INCOMING_CALL:
                break;
            case INCOMING_MESSAGE:
                break;
            case CONFERENCE_CREATED:
                break;
            case CONFERENCE_CHANGED:
                break;
            case CONFERENCE_REMOVED:
                break;
            case RECORD_PLAYBACK_FILEPATH:
                // todo
                break;
            case RTCP_REPORT_RECEIVED:
                break;
            default:
                Log.i(TAG, "Unkown daemon event");
                break;
        }
    }

}
