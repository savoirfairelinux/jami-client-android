/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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


import java.util.Map;

import cx.ring.utils.Log;

public class SipCall {
    private static final String TAG = SipCall.class.getSimpleName();

    private String mCallID = "";
    private String mAccount = "";
    private CallContact mContact = null;
    private Uri mNumber = null;
    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isRecording = false;
    private long timestampStart = 0;
    private long timestampEnd = 0;
    private boolean missed = true;

    private int mCallType;
    private int mCallState = State.NONE;

    private String videoSource = null;

    public SipCall(String id, String account, Uri number, int direction) {
        mCallID = id;
        mAccount = account;
        mNumber = number;
        mCallType = direction;
    }

    public SipCall(String id, String account, String number, int direction) {
        this(id, account, new Uri(number), direction);
    }

    public SipCall(SipCall call) {
        mCallID = call.mCallID;
        mAccount = call.mAccount;
        mContact = call.mContact;
        mNumber = call.mNumber;
        isPeerHolding = call.isPeerHolding;
        isAudioMuted = call.isAudioMuted;
        isVideoMuted = call.isVideoMuted;
        isRecording = call.isRecording;
        missed = call.missed;
        timestampStart = call.timestampStart;
        timestampEnd = call.timestampEnd;
        mCallType = call.mCallType;
        mCallState = call.mCallState;
    }

    /**
     * *********************
     * Construtors
     * *********************
     */

    public SipCall(String callId, Map<String, String> call_details) {
        this(callId,
                call_details.get("ACCOUNTID"),
                call_details.get("PEER_NUMBER"),
                Integer.parseInt(call_details.get("CALL_TYPE")));
        mCallState = stateFromString(call_details.get("CALL_STATE"));
        setDetails(call_details);
    }

    public String getRecordPath() {
        return "";
    }

    public int getCallType() {
        return mCallType;
    }


    public int getCallState() {
        return mCallState;
    }

    public void setDetails(Map<String, String> details) {
        isPeerHolding = "true".equals(details.get("PEER_HOLDING"));
        isAudioMuted = "true".equals(details.get("AUDIO_MUTED"));
        isVideoMuted = "true".equals(details.get("VIDEO_MUTED"));
        videoSource = details.get("VIDEO_SOURCE");
    }

    public long getDuration() {
        return isMissed() ? 0 : timestampEnd - timestampStart;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void muteVideo(boolean mute) {
        isVideoMuted = mute;
    }

    public boolean isVideoMuted() {
        return isVideoMuted;
    }

    public interface Direction {
        int INCOMING = 0;
        int OUTGOING = 1;
    }

    public interface State {
        int NONE = 0;
        int INCOMING = 1;
        int CONNECTING = 2;
        int RINGING = 3;
        int CURRENT = 4;
        int HUNGUP = 5;
        int BUSY = 6;
        int FAILURE = 7;
        int HOLD = 8;
        int UNHOLD = 9;
        int INACTIVE = 10;
        int OVER = 11;
    }

    public void setCallID(String callID) {
        mCallID = callID;
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

    public void setAccount(String account) {
        mAccount = account;
    }

    public String getAccount() {
        return mAccount;
    }

    public void setCallState(int callState) {
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

    public String getNumber() {
        return mNumber.getUriString();
    }

    public Uri getNumberUri() {
        return mNumber;
    }

    public String stateToString() {
        return stateToString(mCallState);
    }

    public static String stateToString(int state) {
        switch (state) {
            case State.INCOMING:
                return "INCOMING";
            case State.CONNECTING:
                return "CONNECTING";
            case State.RINGING:
                return "RINGING";
            case State.CURRENT:
                return "CURRENT";
            case State.HUNGUP:
                return "HUNGUP";
            case State.BUSY:
                return "BUSY";
            case State.FAILURE:
                return "FAILURE";
            case State.HOLD:
                return "HOLD";
            case State.UNHOLD:
                return "UNHOLD";
            case State.OVER:
                return "OVER";
            case State.NONE:
            default:
                return "NONE";
        }
    }

    public static int stateFromString(String state) {
        switch (state) {
            case "INCOMING":
                return State.INCOMING;
            case "CONNECTING":
                return State.CONNECTING;
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

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public void printCallInfo() {
        Log.i(TAG, "CallInfo: CallID: " + mCallID);
        Log.i(TAG, "          AccountID: " + mAccount);
        Log.i(TAG, "          CallState: " + stateToString());
        Log.i(TAG, "          CallType: " + mCallType);
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
        return mCallState == State.CONNECTING || mCallState == State.RINGING || mCallState == State.NONE;
    }

    public boolean isIncoming() {
        return mCallType == Direction.INCOMING;
    }

    public boolean isOngoing() {
        return !(mCallState == State.CONNECTING || mCallState == State.RINGING || mCallState == State.NONE || mCallState == State.FAILURE
                || mCallState == State.BUSY || mCallState == State.HUNGUP || mCallState == State.INCOMING || mCallState == State.OVER);

    }

    public boolean isOnHold() {
        return mCallState == State.HOLD;
    }

    public boolean isCurrent() {
        return mCallState == State.CURRENT;
    }


}
