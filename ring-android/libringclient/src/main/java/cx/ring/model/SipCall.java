/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@gmail.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.model;

import java.util.HashMap;
import java.util.Map;

import cx.ring.utils.ProfileChunk;
import cx.ring.utils.VCardUtils;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class SipCall {

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
    public final static String KEY_CONF_ID = "CONF_ID";

    private final String mCallID;
    private final String mAccount;
    private CallContact mContact = null;
    private Uri mNumber = null;
    private String mConfId = null;
    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isRecording = false;
    private boolean isAudioOnly = false;
    private long timestampStart = 0;
    private long timestampEnd = 0;
    private boolean missed = true;
    private String mAudioCodec;
    private String mVideoCodec;

    private Direction mCallType;
    private State mCallState = State.NONE;

    private ProfileChunk mProfileChunk = null;


    public SipCall(String id, String account, Uri number, Direction direction) {
        mCallID = id;
        mAccount = account;
        mNumber = number;
        mCallType = direction;
    }

    public SipCall(String id, String account, String number, Direction direction) {
        this(id, account, new Uri(number), direction);
    }

    /**
     * *********************
     * Constructors
     * *********************
     */

    public SipCall(String callId, Map<String, String> call_details) {
        this(callId,
                call_details.get(KEY_ACCOUNT_ID),
                call_details.get(KEY_PEER_NUMBER),
                Direction.fromInt(Integer.parseInt(call_details.get(KEY_CALL_TYPE))));
        mCallState = State.fromString(call_details.get(KEY_CALL_STATE));
        setDetails(call_details);
    }

    public String getRecordPath() {
        return "";
    }

    public Direction getCallType() {
        return mCallType;
    }

    public State getCallState() {
        return mCallState;
    }

    public void setDetails(Map<String, String> details) {
        isPeerHolding = "true".equals(details.get(KEY_PEER_HOLDING));
        isAudioMuted = "true".equals(details.get(KEY_AUDIO_MUTED));
        isVideoMuted = "true".equals(details.get(KEY_VIDEO_MUTED));
        isAudioOnly = "true".equals(details.get(KEY_AUDIO_ONLY));
        mAudioCodec = details.get(KEY_AUDIO_CODEC);
        mVideoCodec = details.get(KEY_VIDEO_CODEC);
        mConfId = details.get(KEY_CONF_ID);
    }

    public boolean isConferenceParticipant() {
        return mConfId != null;
    }

    public long getDuration() {
        return isMissed() ? 0 : timestampEnd - timestampStart;
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

    public String getConfId() {
        return mConfId;
    }

    public void setConfId(String confId) {
        mConfId = confId;
    }

    public enum Direction {
        INCOMING(0),
        OUTGOING(1);

        private int value;
        Direction(int v) {
            value = v;
        }

        int getValue() {
            return value;
        }

        public static Direction fromInt(int i) {
            return i == INCOMING.value ? INCOMING : OUTGOING;
        }
    }

    public enum State {
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

        public static State fromString(String state) {
            switch (state) {
                case "SEARCHING":
                    return SEARCHING;
                case "CONNECTING":
                    return CONNECTING;
                case "INCOMING":
                case "RINGING":
                    return RINGING;
                case "CURRENT":
                    return CURRENT;
                case "HUNGUP":
                    return HUNGUP;
                case "BUSY":
                    return BUSY;
                case "FAILURE":
                    return FAILURE;
                case "HOLD":
                    return HOLD;
                case "UNHOLD":
                    return UNHOLD;
                case "INACTIVE":
                    return INACTIVE;
                case "OVER":
                    return OVER;
                case "NONE":
                default:
                    return NONE;
            }
        }

    }

    public String getCallId() {
        return mCallID;
    }

    public long getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(long timestampStart) {
        this.timestampStart = timestampStart;
    }

    public long getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    public String getAccount() {
        return mAccount;
    }

    public void setCallState(State callState) {
        mCallState = callState;
        if (mCallState == State.CURRENT)
            missed = false;
    }

    public boolean isMissed() {
        return missed;
    }

    public void setContact(CallContact c) {
        mContact = c;
    }

    public CallContact getContact() {
        return mContact;
    }

    public void setNumber(String n) {
        mNumber = new Uri(n);
    }

    public void setNumber(Uri n) {
        mNumber = n;
    }

    public boolean isAudioOnly() {
        return isAudioOnly;
    }

    public String getNumber() {
        return mNumber.getUriString();
    }

    public Uri getNumberUri() {
        return mNumber;
    }

    /**
     * Compare sip calls based on call ID
     */
    @Override
    public boolean equals(Object c) {
        return c instanceof SipCall && ((SipCall) c).mCallID.contentEquals((mCallID));
    }

    public boolean isOutGoing() {
        return mCallType == Direction.OUTGOING;
    }

    public boolean isRinging() {
        return mCallState == State.CONNECTING || mCallState == State.RINGING || mCallState == State.NONE || mCallState == State.SEARCHING;
    }

    public boolean isIncoming() {
        return mCallType == Direction.INCOMING;
    }

    public boolean isOnGoing() {
        return mCallState == State.CURRENT || mCallState == State.HOLD || mCallState == State.UNHOLD;
    }

    public boolean isCurrent() {
        return mCallState == State.CURRENT;
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

}
