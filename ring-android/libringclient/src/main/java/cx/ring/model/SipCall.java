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

import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
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

    private final String mCallID;
    private final String mAccount;
    private CallContact mContact = null;
    private Uri mNumber = null;
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

    private int mCallType;
    private State mCallState = State.NONE;

    private ProfileChunk mProfileChunk = null;


    public SipCall(String id, String account, Uri number, int direction) {
        mCallID = id;
        mAccount = account;
        mNumber = number;
        mCallType = direction;
    }

    public SipCall(String id, String account, String number, int direction) {
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
                Integer.parseInt(call_details.get(KEY_CALL_TYPE)));
        mCallState = stateFromString(call_details.get(KEY_CALL_STATE));
        setDetails(call_details);
    }

    public String getRecordPath() {
        return "";
    }

    public int getCallType() {
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
    public interface Direction {
        int INCOMING = 0;
        int OUTGOING = 1;
    }

    public enum State {
        NONE(0),
        SEARCHING(1),
        CONNECTING(2),
        RINGING(3),
        CURRENT(4),
        HUNGUP(5),
        BUSY(6),
        FAILURE(7),
        HOLD(8),
        UNHOLD(9),
        INACTIVE(10),
        OVER(11);

        private final int value;
        State(int value){
            this.value = value;
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

    public static State stateFromString(String state) {
        switch (state) {
            case "SEARCHING":
                return State.SEARCHING;
            case "CONNECTING":
                return State.CONNECTING;
            case "INCOMING":
            case "RINGING":
                return State.RINGING;
            case "CURRENT":
                return State.CURRENT;
            case "HUNGUP":
                return State.HUNGUP;
            case "BUSY":
                return State.BUSY;
            case "FAILURE":
                return State.FAILURE;
            case "HOLD":
                return State.HOLD;
            case "UNHOLD":
                return State.UNHOLD;
            case "INACTIVE":
                return State.INACTIVE;
            case "OVER":
                return State.OVER;
            case "NONE":
            default:
                return State.NONE;
        }
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
