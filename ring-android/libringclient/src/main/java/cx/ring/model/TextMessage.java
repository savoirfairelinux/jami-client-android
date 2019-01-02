/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

public class TextMessage implements ConversationElement {

    private long mID = 0;
    private String mAccount = null;
    private CallContact mContact = null;
    private Uri mNumber = null;
    private long mTimestamp = 0;

    private int mType;
    private Status mState = Status.UNKNOWN;
    private String mMessage;
    private String mCallID = "";

    private boolean mRead = false;
    private boolean mNotified = false;

    public Status getStatus() {
        return mState;
    }

    public enum Status {
        UNKNOWN, SENDING, SENT, READ, FAILURE;

        static Status fromString(String str) {
            for (Status s : values()) {
                if (s.name().equals(str)) {
                    return s;
                }
            }
            return UNKNOWN;
        }

        static Status fromInt(int n) {
            try {
                return values()[n];
            } catch (ArrayIndexOutOfBoundsException e) {
                return UNKNOWN;
            }
        }
    }

    public TextMessage(boolean in, String message, Uri number, String callid, String account) {
        mAccount = account;
        mNumber = number;
        mMessage = message;
        mTimestamp = System.currentTimeMillis();
        mCallID = callid;
        mType = in ? direction.INCOMING : direction.OUTGOING;
    }

    public TextMessage(HistoryText h) {
        if (h == null) {
            throw new IllegalArgumentException("not able to create TextMessage from null HistoryText");
        }
        mID = h.id;
        mAccount = h.getAccountID();
        mNumber = new Uri(h.getNumber());
        mTimestamp = h.getDate();
        mType = h.isIncoming() ? direction.INCOMING : direction.OUTGOING;
        mMessage = h.getMessage();
        mCallID = h.getCallId();
        mRead = h.isRead();
        mState = h.getStatus();
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

    public void setNumber(Uri number) {
        this.mNumber = number;
    }

    public void read() {
        mRead = true;
    }

    public void setStatus(int status) {
        mState = Status.fromInt(status);
    }

    public void setStatus(Status status) {
        mState = status;
    }

    public interface direction {
        int INCOMING = 1;
        int OUTGOING = 2;
    }

    public void setID(long id) {
        mID = id;
    }

    public long getId() {
        return mID;
    }

    @Override
    public CEType getType() {
        return CEType.TEXT;
    }

    @Override
    public long getDate() {
        return mTimestamp;
    }

    @Override
    public Uri getContactNumber() {
        return getNumberUri();
    }

    public void setAccount(String account) {
        mAccount = account;
    }

    public String getAccount() {
        return mAccount;
    }

    public void setState(int callState) {
        mState = Status.fromInt(callState);
    }

    public CallContact getContact() {
        return mContact;
    }

    public String getNumber() {
        return mNumber == null ? null : mNumber.getRawUriString();
    }

    public Uri getNumberUri() {
        return mNumber;
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

    public boolean isNotified() {
        return mNotified;
    }

    public void setNotified(boolean noti) {
        mNotified = noti;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TextMessage that = (TextMessage) o;
        return mID == that.mID;
    }
}
