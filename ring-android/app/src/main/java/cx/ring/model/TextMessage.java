/*
 *  Copyright (C) 2004-2015 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@gmail.com>
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

import android.os.Parcel;

import cx.ring.history.HistoryText;

public class TextMessage
{
    private static final String TAG = TextMessage.class.getSimpleName();

    private String mID = "";
    private String mAccount = null;
    private CallContact mContact = null;
    private SipUri mNumber = null;
    private long mTimestamp = 0;

    private int mType;
    private int mState = state.NONE;
    private String mMessage;
    private String mCallID = "";

    private boolean mRead = false;

    public TextMessage(TextMessage msg) {
        mID = msg.mID;
        mAccount = msg.mAccount;
        mContact = msg.mContact;
        mNumber = msg.mNumber;
        mTimestamp = msg.mTimestamp;
        mType = msg.mType;
        mState = msg.mState;
        mMessage = msg.mMessage;
        mCallID = msg.mCallID;
        mRead = msg.mRead;
    }

    public TextMessage(boolean in, String message) {
        mMessage = message;
        mType = in ? direction.INCOMING : direction.OUTGOING;
        mTimestamp = System.currentTimeMillis();
    }

    public TextMessage(boolean in, String message, SipUri number, String callid, String account) {
        mAccount = account;
        mNumber = number;
        mMessage = message;
        mTimestamp = System.currentTimeMillis();
        mCallID = callid;
        mType = in ? direction.INCOMING : direction.OUTGOING;
    }

    public TextMessage(HistoryText h) {
        mID = h.id;
        mAccount = h.getAccountID();
        mNumber = new SipUri(h.getNumber());
        mTimestamp = h.getDate().getTime();
        mType = h.isIncoming() ? direction.INCOMING : direction.OUTGOING;
        mMessage = h.getMessage();
        mCallID = h.getCallId();
        mRead = h.isRead();
    }

    protected TextMessage(Parcel in) {
        mID = in.readString();
        mAccount = in.readString();
        mContact = in.readParcelable(CallContact.class.getClassLoader());
        mNumber = in.readParcelable(SipUri.class.getClassLoader());
        mCallID = in.readString();
        mType = in.readInt();
        mState = in.readInt();
        mTimestamp = in.readLong();
        mMessage = in.readString();
        mRead = in.readByte() != 0;
    }

    public String getRecordPath() {
        return "";
    }

    public int getCallType() {
        return mType;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setContact(CallContact contact) {
        mContact = contact;
    }

    public void setCallId(String callId) {
        this.mCallID = callId;
    }

    public String getCallId() {
        return mCallID;
    }

    public void setNumber(SipUri number) {
        this.mNumber = number;
    }

    public void read() {
        mRead = true;
    }

    public interface direction {
        int INCOMING = 1;
        int OUTGOING = 2;
    }

    public interface state {
        int NONE = 0;
    }

    public void setID(String id) {
        mID = id;
    }

    public String getId() {
        return mID;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
    }

    public void setAccount(String account) {
        mAccount = account;
    }

    public String getAccount() {
        return mAccount;
    }

    public String getTypeString() {
        switch (mType) {
            case direction.INCOMING:
                return "INCOMING";
            case direction.OUTGOING:
                return "OUTGOING";
            default:
                return "UNDETERMINED";
        }
    }

    public void setState(int callState) {
        mState = callState;
    }

    public CallContact getContact() {
        return mContact;
    }

    public String getNumber() {
        return mNumber.getRawUriString();
    }
    public SipUri getNumberUri() {
        return mNumber;
    }

    public String getStateString() {
        String text_state;
        switch (mState) {
            case state.NONE:
                text_state = "NONE";
                break;
            default:
                text_state = "NULL";
        }
        return text_state;
    }

    /**
     * Compare sip calls based on call ID
     */
    @Override
    public boolean equals(Object c) {
        return c instanceof TextMessage && ((TextMessage) c).mID.contentEquals((mID));
    }

    public boolean isOutgoing() {
        return mType == direction.OUTGOING;
    }

    public boolean isIncoming() {
        return mType == direction.INCOMING;
    }

    public boolean isRead() {
        return mRead;
    }

    public void setRead(boolean read) {
        mRead = read;
    }

}
