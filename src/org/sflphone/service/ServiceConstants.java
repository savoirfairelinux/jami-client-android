package org.sflphone.service;

public final class ServiceConstants {

    public static final String INTENT_SIP_SERVICE = "com.savoirfairelinux.sflphone.service.SipService";
    public static final String EXTRA_OUTGOING_ACTIVITY = "outgoing_activity";

    public interface history {
        public static final String ACCOUNT_ID_KEY = "accountid";
        public static final String CALLID_KEY = "callid";
        public static final String CONFID_KEY = "confid";
        public static final String DISPLAY_NAME_KEY = "display_name";
        public static final String PEER_NUMBER_KEY = "peer_number";
        public static final String RECORDING_PATH_KEY = "recordfile";
        public static final String STATE_KEY = "state";
        public static final String TIMESTAMP_START_KEY = "timestamp_start";
        public static final String TIMESTAMP_STOP_KEY = "timestamp_stop";
        public static final String AUDIO_CODEC_KEY = "audio_codec";
        public static final String VIDEO_CODEC_KEY = "video_codec";

        public static final String MISSED_STRING = "missed";
        public static final String INCOMING_STRING = "incoming";
        public static final String OUTGOING_STRING = "outgoing";
    }

    public interface call {
        public static final String CALL_TYPE = "CALL_TYPE";
        public static final String PEER_NUMBER = "PEER_NUMBER";
        public static final String DISPLAY_NAME = "DISPLAY_NAME";
        public static final String CALL_STATE = "CALL_STATE";
        public static final String CONF_ID = "CONF_ID";
        public static final String TIMESTAMP_START = "TIMESTAMP_START";
        public static final String ACCOUNTID = "ACCOUNTID";
    }
}
