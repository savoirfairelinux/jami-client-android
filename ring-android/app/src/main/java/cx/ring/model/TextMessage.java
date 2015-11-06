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
package cx.ring.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import cx.ring.history.HistoryText;

public class TextMessage implements Parcelable {

    public static String ID = "id";
    public static String ACCOUNT = "account";
    public static String CONTACT = "contact";
    public static String NUMBER = "number";
    public static String CALL = "call";
    public static String TYPE = "type";
    public static String STATE = "State";
    public static String MESSAGE = "message";
    public static String TIME = "time";
    public static String READ = "read";

    private static final String TAG = TextMessage.class.getSimpleName();

    private String mID = "";
    private String mAccount = null;
    private CallContact mContact = null;
    private String mNumber = null;
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

    /**
     * *********************
     * Construtors
     * *********************
     */

    /*public TextMessage(String account, String number, String message, boolean in) {
        mAccount = account;
        mNumber = number;
        mMessage = message;
        mTimestamp = System.currentTimeMillis();
        mType = in ? direction.INCOMING : direction.OUTGOING;
    }*/

    public TextMessage(boolean in, String message) {
        mMessage = message;
        mType = in ? direction.INCOMING : direction.OUTGOING;
        mTimestamp = System.currentTimeMillis();
    }

    public TextMessage(boolean in, String message, String number, String callid, String account) {
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
        mNumber = h.getNumber();
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
        mNumber = in.readString();
        mCallID = in.readString();
        mType = in.readInt();
        mState = in.readInt();
        mTimestamp = in.readLong();
        mMessage = in.readString();
        mRead = in.readByte() != 0;
    }

    public TextMessage(Bundle args) {
        mID = args.getString(ID);
        mAccount = args.getParcelable(ACCOUNT);
        mNumber = args.getString(NUMBER);
        mCallID = args.getString(CALL);
        mType = args.getInt(TYPE);
        mState = args.getInt(STATE);
        mContact = args.getParcelable(CONTACT);
        mMessage = args.getString(MESSAGE);
        mTimestamp = args.getLong(TIME);
        mRead = args.getByte(READ) != 0;
    }

    public String getRecordPath() {
        return "";
    }

    public int getCallType() {
        return mType;
    }

    public Bundle getBundle() {
        Bundle args = new Bundle();
        args.putString(ID, mID);
        args.putString(ACCOUNT, mAccount);
        args.putString(NUMBER, mNumber);
        args.putString(CALL, mCallID);
        args.putInt(STATE, mState);
        args.putInt(TYPE, mType);
        args.putParcelable(CONTACT, mContact);
        args.putString(MESSAGE, mMessage);
        args.putLong(TIME, mTimestamp);
        args.putByte(READ, mRead ? (byte) 1 : (byte) 0);
        return args;
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

    public void setNumber(String number) {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mID);
        out.writeString(mAccount);
        out.writeParcelable(mContact, 0);
        out.writeString(mNumber);
        out.writeString(mCallID);
        out.writeInt(mType);
        out.writeInt(mState);
        out.writeLong(mTimestamp);
        out.writeString(mMessage);
        out.writeByte(mRead ? (byte) 1 : (byte) 0);
    }

    public static final Creator<TextMessage> CREATOR = new Creator<TextMessage>() {
        public TextMessage createFromParcel(Parcel in) {
            return new TextMessage(in);
        }

        public TextMessage[] newArray(int size) {
            return new TextMessage[size];
        }
    };

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
