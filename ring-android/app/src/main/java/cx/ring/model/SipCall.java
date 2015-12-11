/*
 *  Copyright (C) 2004-2015 Savoir-faire Linux Inc.
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.model;

import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import cx.ring.service.LocalService;

public class SipCall
{
    private static final String TAG = SipCall.class.getSimpleName();
    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "calls");

    private String mCallID = "";
    private String mAccount = "";
    private CallContact mContact = null;
    private SipUri mNumber = null;
    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isRecording = false;
    private long timestampStart_ = 0;
    private long timestampEnd_ = 0;

    private int mCallType;
    private int mCallState = State.NONE;

    public SipCall(String id, String account, SipUri number, int direction) {
        mCallID = id;
        mAccount = account;
        mNumber = number;
        mCallType = direction;
    }
    public SipCall(String id, String account, String number, int direction) {
        this(id, account, new SipUri(number), direction);
    }

    public SipCall(SipCall call) {
        mCallID = call.mCallID;
        mAccount = call.mAccount;
        mContact = call.mContact;
        mNumber = call.mNumber;
        isPeerHolding = call.isPeerHolding;
        isAudioMuted = call.isAudioMuted;
        isRecording = call.isRecording;
        timestampStart_ = call.timestampStart_;
        timestampEnd_ = call.timestampEnd_;
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
        isPeerHolding = call_details.get("PEER_HOLDING").contentEquals("true");
        isAudioMuted = call_details.get("AUDIO_MUTED").contentEquals("true");
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

    public void setDetails(HashMap<String, String> details) {
        isPeerHolding = "true".equals(details.get("PEER_HOLDING"));
        isAudioMuted = "true".equals(details.get("AUDIO_MUTED"));
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
        return timestampStart_;
    }

    public void setTimestampStart(long timestampStart) {
        this.timestampStart_ = timestampStart;
    }

    public long getTimestampEnd() {
        return timestampEnd_;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd_ = timestampEnd;
    }

    public void setAccount(String account) {
        mAccount = account;
    }

    public String getAccount() {
        return mAccount;
    }

    public void setCallState(int callState) {
        mCallState = callState;
    }

    public void setContact(CallContact c) {
        mContact = c;
    }

    public CallContact getContact() {
        return mContact;
    }

    public void setNumber(String n) {
        mNumber = new SipUri(n);
    }
    public void setNumber(SipUri n) {
        mNumber = n;
    }

    public String getNumber() {
        return mNumber.getUriString();
    }
    public SipUri getNumberUri() {
        return mNumber;
    }

    public String getCallStateString() {
        return getCallStateString(mCallState);
    }

    public static String getCallStateString(int state) {
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
        Log.i(TAG, "          CallState: " + getCallStateString());
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
                || mCallState == State.BUSY || mCallState == State.HUNGUP);

    }

    public boolean isOnHold() {
        return mCallState == State.HOLD;
    }

    public boolean isCurrent() {
        return mCallState == State.CURRENT;
    }


}
