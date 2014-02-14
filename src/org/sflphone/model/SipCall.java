/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux>
 *          Alexandre Savard <alexandre.savard@gmail.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package org.sflphone.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class SipCall implements Parcelable {

    public static String ID = "id";
    public static String ACCOUNT = "account";
    public static String CONTACT = "contcat";
    public static String TYPE = "type";
    public static String STATE = "state";

    private static final String TAG = SipCall.class.getSimpleName();

    private String mCallID = "";
    private Account mAccount = null;
    private CallContact mContact = null;
    private boolean isRecording = false;
    private long timestampStart_ = 0;
    private long timestampEnd_ = 0;

    private int mCallType;
    private int mCallState = state.CALL_STATE_NONE;

    public SipCall(SipCall call) {
        mCallID = call.mCallID;
        mAccount = call.mAccount;
        mContact = call.mContact;
        isRecording = call.isRecording;
        timestampStart_ = call.timestampStart_;
        timestampEnd_ = call.timestampEnd_;
        mCallType = call.mCallType;
        mCallState = call.mCallState;
    }


    public boolean isSecured() {
        return false;
    }
    /**
     * *********************
     * Construtors
     * *********************
     */

    protected SipCall(Parcel in) {

        mCallID = in.readString();
        mAccount = in.readParcelable(Account.class.getClassLoader());
        mContact = in.readParcelable(CallContact.class.getClassLoader());
        isRecording = in.readByte() == 1;
        mCallType = in.readInt();
        mCallState = in.readInt();
        timestampStart_ = in.readLong();
        timestampEnd_ = in.readLong();
    }

    public SipCall(Bundle args) {
        mCallID = args.getString(ID);
        mAccount = args.getParcelable(ACCOUNT);
        mCallType = args.getInt(TYPE);
        mCallState = args.getInt(STATE);
        mContact = args.getParcelable(CONTACT);
    }

    public long getTimestampEnd_() {
        return timestampEnd_;
    }

    public String getRecordPath() {
        return "";
    }

    public int getCallType() {
        return mCallType;
    }

    public Bundle getBundle() {
        Bundle args = new Bundle();
        args.putString(SipCall.ID, mCallID);
        args.putParcelable(SipCall.ACCOUNT, mAccount);
        args.putInt(SipCall.STATE, mCallState);
        args.putInt(SipCall.TYPE, mCallType);
        args.putParcelable(SipCall.CONTACT, mContact);
        return args;
    }


    public interface direction {
        public static final int CALL_TYPE_INCOMING = 1;
        public static final int CALL_TYPE_OUTGOING = 2;
    }

    public interface state {
        public static final int CALL_STATE_NONE = 0;
        public static final int CALL_STATE_RINGING = 2;
        public static final int CALL_STATE_CURRENT = 3;
        public static final int CALL_STATE_HUNGUP = 4;
        public static final int CALL_STATE_BUSY = 5;
        public static final int CALL_STATE_FAILURE = 6;
        public static final int CALL_STATE_HOLD = 7;
        public static final int CALL_STATE_UNHOLD = 8;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {

        out.writeString(mCallID);
        out.writeParcelable(mAccount, 0);

        out.writeParcelable(mContact, 0);
        out.writeByte((byte) (isRecording ? 1 : 0));
        out.writeInt(mCallType);
        out.writeInt(mCallState);
        out.writeLong(timestampStart_);
        out.writeLong(timestampEnd_);
    }

    public static final Parcelable.Creator<SipCall> CREATOR = new Parcelable.Creator<SipCall>() {
        public SipCall createFromParcel(Parcel in) {
            return new SipCall(in);
        }

        public SipCall[] newArray(int size) {
            return new SipCall[size];
        }
    };

    public void setCallID(String callID) {
        mCallID = callID;
    }

    public String getCallId() {
        return mCallID;
    }

    public long getTimestampStart_() {
        return timestampStart_;
    }

    public void setTimestampStart_(long timestampStart_) {
        this.timestampStart_ = timestampStart_;
    }

    public void setTimestampEnd_(long timestampEnd_) {
        this.timestampEnd_ = timestampEnd_;
    }

    public void setAccount(Account account) {
        mAccount = account;
    }

    public Account getAccount() {
        return mAccount;
    }

    public String getCallTypeString() {
        switch (mCallType) {
            case direction.CALL_TYPE_INCOMING:
                return "CALL_TYPE_INCOMING";
            case direction.CALL_TYPE_OUTGOING:
                return "CALL_TYPE_OUTGOING";
            default:
                return "CALL_TYPE_UNDETERMINED";
        }
    }

    public void setCallState(int callState) {
        mCallState = callState;
    }

    public CallContact getmContact() {
        return mContact;
    }

    public void setmContact(CallContact contacts) {
        mContact = contacts;
    }

    public String getCallStateString() {

        String text_state;

        switch (mCallState) {
            case state.CALL_STATE_RINGING:
                text_state = "RINGING";
                break;
            case state.CALL_STATE_CURRENT:
                text_state = "CURRENT";
                break;
            case state.CALL_STATE_HUNGUP:
                text_state = "HUNGUP";
                break;
            case state.CALL_STATE_BUSY:
                text_state = "BUSY";
                break;
            case state.CALL_STATE_FAILURE:
                text_state = "FAILURE";
                break;
            case state.CALL_STATE_HOLD:
                text_state = "HOLD";
                break;
            case state.CALL_STATE_UNHOLD:
                text_state = "UNHOLD";
                break;
            default:
                text_state = "NULL";
        }

        return text_state;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public void printCallInfo() {
        Log.i(TAG, "CallInfo: CallID: " + mCallID);
        Log.i(TAG, "          AccountID: " + mAccount.getAccountID());
        Log.i(TAG, "          CallState: " + mCallState);
        Log.i(TAG, "          CallType: " + mCallType);
    }

    /**
     * Compare sip calls based on call ID
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof SipCall && ((SipCall) c).mCallID.contentEquals((mCallID))) {
            return true;
        }
        return false;

    }

    public boolean isOutGoing() {
        if (mCallType == direction.CALL_TYPE_OUTGOING)
            return true;
        return false;
    }

    public boolean isRinging() {
        if (mCallState == state.CALL_STATE_RINGING || mCallState == state.CALL_STATE_NONE)
            return true;

        return false;
    }

    public boolean isIncoming() {
        if (mCallType == direction.CALL_TYPE_INCOMING)
            return true;

        return false;
    }

    public void setCallState(String newState) {
        if (newState.equals("RINGING")) {
            setCallState(SipCall.state.CALL_STATE_RINGING);
        } else if (newState.equals("CURRENT")) {
            setCallState(SipCall.state.CALL_STATE_CURRENT);
        } else if (newState.equals("HUNGUP")) {
            setCallState(SipCall.state.CALL_STATE_HUNGUP);
        } else if (newState.equals("BUSY")) {
            setCallState(SipCall.state.CALL_STATE_BUSY);
        } else if (newState.equals("FAILURE")) {
            setCallState(SipCall.state.CALL_STATE_FAILURE);
        } else if (newState.equals("HOLD")) {
            setCallState(SipCall.state.CALL_STATE_HOLD);
        } else if (newState.equals("UNHOLD")) {
            setCallState(SipCall.state.CALL_STATE_CURRENT);
        } else {
            setCallState(SipCall.state.CALL_STATE_NONE);
        }

    }

    public boolean isOngoing() {
        if (mCallState == state.CALL_STATE_RINGING || mCallState == state.CALL_STATE_NONE || mCallState == state.CALL_STATE_FAILURE
                || mCallState == state.CALL_STATE_BUSY || mCallState == state.CALL_STATE_HUNGUP)
            return false;

        return true;
    }

    public boolean isOnHold() {
        return mCallState == state.CALL_STATE_HOLD;
    }

    public boolean isCurrent() {
        return mCallState == state.CALL_STATE_CURRENT;
    }


}
