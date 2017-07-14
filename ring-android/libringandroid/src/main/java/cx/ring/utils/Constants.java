package cx.ring.utils;


public class Constants {
    public static final String APPLICATION_ID = "cx.ring";

    public static final String KEY_CACHE_HAVE_RINGACCOUNT = "cache_haveRingAccount";
    public static final String KEY_CACHE_HAVE_SIPACCOUNT = "cache_haveSipAccount";
    public static final String ACTION_TRUST_REQUEST_ACCEPT = APPLICATION_ID + ".action.TRUST_REQUEST_ACCEPT";
    public static final String ACTION_TRUST_REQUEST_REFUSE = APPLICATION_ID + ".action.TRUST_REQUEST_REFUSE";
    public static final String ACTION_TRUST_REQUEST_BLOCK = APPLICATION_ID + ".action.TRUST_REQUEST_BLOCK";
    public static final String ACTION_CALL = APPLICATION_ID + ".action.CALL";
    public static final String ACTION_CALL_ACCEPT = APPLICATION_ID + ".action.CALL_ACCEPT";
    public static final String ACTION_CALL_REFUSE = APPLICATION_ID + ".action.CALL_REFUSE";
    public static final String ACTION_CALL_END = APPLICATION_ID + ".action.CALL_END";
    public static final String ACTION_CALL_VIEW = APPLICATION_ID + ".action.CALL_VIEW";
    public static final String ACTION_CONV_READ = APPLICATION_ID + ".action.CONV_READ";
    public static final String ACTION_CONV_ACCEPT = APPLICATION_ID + ".action.CONV_ACCEPT";
    public final static String DRING_CONNECTION_CHANGED = APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE";

    public static final String ACCOUNTS_CHANGED = APPLICATION_ID + "accounts.changed";
    public static final String ACCOUNTS_DEVICES_CHANGED = APPLICATION_ID + "accounts.devicesChanged";
    public static final String ACCOUNTS_EXPORT_ENDED = APPLICATION_ID + ".accounts.exportEnded";
    public static final String ACCOUNT_STATE_CHANGED = APPLICATION_ID + ".account.stateChanged";
    public static final String INCOMING_TEXT = APPLICATION_ID + ".message.incomingTxt";
    public static final String MESSAGE_STATE_CHANGED = APPLICATION_ID + ".message.stateChanged";
    public static final String NAME_LOOKUP_ENDED = APPLICATION_ID + ".name.lookupEnded";
    public static final String NAME_REGISTRATION_ENDED = APPLICATION_ID + ".name.registrationEnded";

    public static final String MESSAGE_STATE_CHANGED_EXTRA_ID = "id";
    public static final String MESSAGE_STATE_CHANGED_EXTRA_STATUS = "status";
    public static final String EXTRAS_NUMBER_TRUST_REQUEST_KEY = APPLICATION_ID + "numberOfTrustRequestKey";
    public static final String EXTRAS_TRUST_REQUEST_FROM_KEY = APPLICATION_ID + "trustRequestFrom";

    public static final String PREF_SYSTEM_DIALER = "pref_systemDialer";

    public static final String COPY_CALL_CONTACT_NUMBER_CLIP_LABEL =
            APPLICATION_ID + ".clipboard.contactNumber";
}
