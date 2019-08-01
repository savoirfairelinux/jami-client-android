package cx.ring.model;


import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cx.ring.utils.ProfileChunk;
import cx.ring.utils.VCardUtils;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class SipCall extends Interaction {

    public final static String KEY_ACCOUNT_ID = "ACCOUNTID";
    public final static String KEY_AUDIO_ONLY = "AUDIO_ONLY";
    public final static String KEY_CALL_TYPE = "CALL_TYPE";
    public final static String KEY_CALL_STATE = "CALL_STATE";
    public final static String KEY_PEER_NUMBER = "PEER_NUMBER";
    public final static String KEY_PEER_HOLDING = "PEER_HOLDING";
    public final static String KEY_AUDIO_MUTED = "PEER_NUMBER";
    public final static String KEY_VIDEO_MUTED = "VIDEO_MUTED";
    public final static String KEY_AUDIO_CODEC = "AUDIO_CODEC";
    public final static String KEY_VIDEO_CODEC = "VIDEO_CODEC";
    public final static String KEY_DURATION = "duration";

    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isRecording = false;
    private boolean isAudioOnly = false;

    // todo is missed

    private CallStatus mCallStatus = CallStatus.NONE;

    private long timestampEnd = 0;
    private boolean missed = true;
    private String mAudioCodec;
    private String mVideoCodec;
    private CallContact mContact;
    private String contactNumber;

    private ProfileChunk mProfileChunk = null;


    public SipCall(String daemonId, String author, ConversationHistory conversation, int direction) {
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
        mAuthor = direction == Direction.INCOMING ? author : null;
        mConversation = conversation;
        mIsIncoming = direction == Direction.INCOMING;
        mTimestamp = System.currentTimeMillis();
        mType = InteractionType.CALL.toString();
        mIsRead = 1;
    }

    public SipCall(Interaction interaction) {
        mId = interaction.getId();
        mAuthor = interaction.getAuthor();
        mConversation = interaction.getConversation();
        mIsIncoming = interaction.isIncoming();
        mTimestamp = interaction.getTimestamp();
        mType = InteractionType.CALL.toString();
        mDaemonId = interaction.getDaemonId();
        mIsRead = interaction.isRead() ? 1 : 0;
        mAccount = interaction.getAccount();
        mExtraFlag = fromJson(interaction.getExtraFlag());
        missed = getDuration() == 0;
        mIsRead = 1;
    }

    public SipCall(String daemonId, String author, int direction) {
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
        mIsIncoming = direction == Direction.INCOMING;
        mAuthor = direction == Direction.INCOMING ? new Uri(author).getUri() : null;
        contactNumber = new Uri(author).getUri();
        mTimestamp = System.currentTimeMillis();
        mType = InteractionType.CALL.toString();
        mIsRead = 1;
    }

    public SipCall(String daemonId, Map<String, String> call_details) {
        this(daemonId, call_details.get(KEY_PEER_NUMBER), Integer.parseInt(call_details.get(KEY_CALL_TYPE)));
        setAccount(call_details.get(KEY_ACCOUNT_ID));
        setCallState(CallStatus.fromString(call_details.get(KEY_CALL_STATE)));
        setDetails(call_details);
    }

    public static CallStatus stateFromString(String state) {
        switch (state) {
            case "SEARCHING":
                return CallStatus.SEARCHING;
            case "CONNECTING":
                return CallStatus.CONNECTING;
            case "INCOMING":
            case "RINGING":
                return CallStatus.RINGING;
            case "CURRENT":
                return CallStatus.CURRENT;
            case "HUNGUP":
                return CallStatus.HUNGUP;
            case "BUSY":
                return CallStatus.BUSY;
            case "FAILURE":
                return CallStatus.FAILURE;
            case "HOLD":
                return CallStatus.HOLD;
            case "UNHOLD":
                return CallStatus.UNHOLD;
            case "INACTIVE":
                return CallStatus.INACTIVE;
            case "OVER":
                return CallStatus.OVER;
            case "NONE":
            default:
                return CallStatus.NONE;
        }
    }

    public void setDetails(Map<String, String> details) {
        isPeerHolding = "true".equals(details.get(KEY_PEER_HOLDING));
        isAudioMuted = "true".equals(details.get(KEY_AUDIO_MUTED));
        isVideoMuted = "true".equals(details.get(KEY_VIDEO_MUTED));
        isAudioOnly = "true".equals(details.get(KEY_AUDIO_ONLY));
        mAudioCodec = details.get(KEY_AUDIO_CODEC);
        mVideoCodec = details.get(KEY_VIDEO_CODEC);
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public Long getDuration() {
        return toJson(mExtraFlag).get(KEY_DURATION) == null ? 0 : toJson(mExtraFlag).get(KEY_DURATION).getAsLong();
    }

    public void setDuration(Long value) {
        JsonObject jsonObject = getExtraFlag();
        jsonObject.addProperty(KEY_DURATION, value);
        mExtraFlag = fromJson(jsonObject);
    }

    public String getDurationString() {
        Long mDuration = getDuration() / 1000;
        if (mDuration < 60) {
            return String.format(Locale.getDefault(), "%02d secs", mDuration);
        }

        if (mDuration < 3600) {
            return String.format(Locale.getDefault(), "%02d mins %02d secs", (mDuration % 3600) / 60, (mDuration % 60));
        }

        return String.format(Locale.getDefault(), "%d h %02d mins %02d secs", mDuration / 3600, (mDuration % 3600) / 60, (mDuration % 60));
    }

    public long getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd = timestampEnd;
        if (timestampEnd != 0 && !isMissed())
            setDuration(timestampEnd - mTimestamp);
    }

    public CallContact getContact() {
        return mContact;
    }

    public void setContact(CallContact c) {
        mContact = c;
    }

    public boolean isMissed() {
        return missed;
    }

    public boolean isAudioOnly() {
        return isAudioOnly;
    }


    public void muteVideo(boolean mute) {
        isVideoMuted = mute;
    }

    public String getVideoCodec() {
        return mVideoCodec;
    }

    public String getAudioCodec() {
        return mAudioCodec;
    }

    public void setCallState(CallStatus callStatus) {
        mCallStatus = callStatus;
        if (callStatus == CallStatus.CURRENT) {
            missed = false;
            mStatus = InteractionStatus.SUCCEEDED.toString();
        } else if (isRinging() || isOnGoing()) {
            mStatus = InteractionStatus.SUCCEEDED.toString();
        } else if (mCallStatus == CallStatus.FAILURE) {
            mStatus = InteractionStatus.FAILED.toString();
        }
    }

    public CallStatus getCallStatus() {
        return mCallStatus;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public boolean isRinging() {
        return mCallStatus == CallStatus.CONNECTING || mCallStatus == CallStatus.RINGING || mCallStatus == CallStatus.NONE || mCallStatus == CallStatus.SEARCHING;
    }

    public boolean isOnGoing() {
        return mCallStatus == CallStatus.CURRENT || mCallStatus == CallStatus.HOLD || mCallStatus == CallStatus.UNHOLD;
    }

    public void setIsIncoming(int direction) {
        mIsIncoming = direction == Direction.INCOMING;
    }

    public VCard appendToVCard(Map<String, String> messages) {
        for (Map.Entry<String, String> message : messages.entrySet()) {
            HashMap<String, String> messageKeyValue = VCardUtils.parseMimeAttributes(message.getKey());
            String mimeType = messageKeyValue.get(VCardUtils.VCARD_KEY_MIME_TYPE);
            if (!VCardUtils.MIME_RING_PROFILE_VCARD.equals(mimeType)) {
                continue;
            }
            int part = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_PART));
            int nbPart = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_OF));
            if (null == mProfileChunk) {
                mProfileChunk = new ProfileChunk(nbPart);
            }
            mProfileChunk.addPartAtIndex(message.getValue(), part);
            if (mProfileChunk.isProfileComplete()) {
                VCard ret = Ezvcard.parse(mProfileChunk.getCompleteProfile()).first();
                mProfileChunk = null;
                return ret;
            }
        }
        return null;
    }

    public enum CallStatus {
        NONE,
        SEARCHING,
        CONNECTING,
        RINGING,
        CURRENT,
        HUNGUP,
        BUSY,
        FAILURE,
        HOLD,
        UNHOLD,
        INACTIVE,
        OVER;

        static CallStatus fromString(String str) {
            for (CallStatus status : values()) {
                if (status.name().equals(str)) {
                    return status;
                }
            }
            return NONE;
        }
    }

    public interface Direction {
        int INCOMING = 0;
        int OUTGOING = 1;
    }


}
