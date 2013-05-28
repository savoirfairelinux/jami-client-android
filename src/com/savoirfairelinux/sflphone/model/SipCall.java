/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@gmail.com>
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
package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;
import java.util.Random;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.savoirfairelinux.sflphone.model.Account.AccountBuilder;
import com.savoirfairelinux.sflphone.model.CallContact.ContactBuilder;
import com.savoirfairelinux.sflphone.service.ISipService;

public class SipCall implements Parcelable {

    private static final String TAG = SipCall.class.getSimpleName();

    private String mCallID = "";
    private String mAccountID = "";
    private ArrayList<CallContact> contacts = new ArrayList<CallContact>();

    private int mCallType = state.CALL_TYPE_UNDETERMINED;
    private int mCallState = state.CALL_STATE_NONE;
    private int mMediaState = state.MEDIA_STATE_NONE;

    /************************
     * Construtors
     * 
     ***********************/

    private SipCall(Parcel in) {
        ArrayList<String> list = in.createStringArrayList();

        // Don't mess with this order!!!
        mCallID = list.get(0);
        mAccountID = list.get(1);
        // mDisplayName = list.get(2);
        // mPhone = list.get(3);
        // mEmail = list.get(4);
        // mRemoteContact = list.get(5);

        contacts = in.createTypedArrayList(CallContact.CREATOR);

        mCallType = in.readInt();
        mCallState = in.readInt();
        mMediaState = in.readInt();
    }

    // public SipCall(Intent call) {

    // }

    public SipCall(String id, String account, int call_type, int call_state, int media_state, ArrayList<CallContact> c) {
        mCallID = id;
        mAccountID = account;
        mCallType = call_type;
        mCallState = call_state;
        mMediaState = media_state;
        this.contacts = new ArrayList<CallContact>(c);
    }


    // public SipCall() {
    // }

    public interface state {
        public static final int CALL_TYPE_UNDETERMINED = 0;
        public static final int CALL_TYPE_INCOMING = 1;
        public static final int CALL_TYPE_OUTGOING = 2;

        public static final int CALL_STATE_NONE = 0;
        public static final int CALL_STATE_INCOMING = 1;
        public static final int CALL_STATE_RINGING = 2;
        public static final int CALL_STATE_CURRENT = 3;
        public static final int CALL_STATE_HUNGUP = 4;
        public static final int CALL_STATE_BUSY = 5;
        public static final int CALL_STATE_FAILURE = 6;
        public static final int CALL_STATE_HOLD = 7;
        public static final int CALL_STATE_UNHOLD = 8;

        public static final int MEDIA_STATE_NONE = 0; // No media currently
        public static final int MEDIA_STATE_ACTIVE = 1; // Media is active
        public static final int MEDIA_STATE_LOCAL_HOLD = 2; // Media is put on hold bu user
        public static final int MEDIA_STATE_REMOTE_HOLD = 3; // Media is put on hold by peer
        public static final int MEDIA_STATE_ERROR = 5; // Media is in error state
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        ArrayList<String> list = new ArrayList<String>();

        // Don't mess with this order!!!
        list.add(mCallID);
        list.add(mAccountID);

        out.writeStringList(list);
        out.writeTypedList(contacts);
        out.writeInt(mCallType);
        out.writeInt(mCallState);
        out.writeInt(mMediaState);
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

    public void setAccountID(String accountID) {
        mAccountID = accountID;
    }

    public String getAccountID() {
        return mAccountID;
    }

    public void setCallType(int callType) {
        mCallType = callType;
    }

    public int getCallType() {
        return mCallType;
    }

    public void setCallState(int callState) {
        mCallState = callState;
    }

    public int getCallStateInt() {
        return mCallState;
    }

    public String getmCallID() {
        return mCallID;
    }

    public void setmCallID(String mCallID) {
        this.mCallID = mCallID;
    }

    public String getmAccountID() {
        return mAccountID;
    }

    public void setmAccountID(String mAccountID) {
        this.mAccountID = mAccountID;
    }

    public ArrayList<CallContact> getContacts() {
        return contacts;
    }

    public void setContacts(ArrayList<CallContact> contacts) {
        this.contacts = contacts;
    }

    public int getmCallType() {
        return mCallType;
    }

    public void setmCallType(int mCallType) {
        this.mCallType = mCallType;
    }

    public int getmCallState() {
        return mCallState;
    }

    public void setmCallState(int mCallState) {
        this.mCallState = mCallState;
    }

    public int getmMediaState() {
        return mMediaState;
    }

    public void setmMediaState(int mMediaState) {
        this.mMediaState = mMediaState;
    }

    public String getCallStateString() {

        String text_state;

        switch (mCallState) {
        case state.CALL_STATE_INCOMING:
            text_state = "INCOMING";
            break;
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

    public void setMediaState(int mediaState) {
        mMediaState = mediaState;
    }

    public int getMediaState() {
        return mMediaState;
    }

    public boolean notifyServiceAnswer(ISipService service) {
        int callState = getCallStateInt();
        if ((callState != state.CALL_STATE_RINGING) && (callState != state.CALL_STATE_NONE)) {
            return false;
        }

        try {
            service.accept(mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return true;
    }

    /**
     * Perform hangup action without sending request to the service Used when SipService haved been notified that this call hung up
     */
    // public void hangupUpdateUi() {
    // Log.i(TAG, "Hangup call " + mCallID);
    //
    // if(mCallElementList != null)
    // mCallElementList.removeCall(this);
    //
    // if(mHome != null)
    // mHome.onUnselectedCallAction();
    // }

    public static class SipCallBuilder {

        private String bCallID = "";
        private String bAccountID = "";
        private ArrayList<CallContact> bContacts = new ArrayList<CallContact>();

        private int bCallType = state.CALL_TYPE_UNDETERMINED;
        private int bCallState = state.CALL_STATE_NONE;
        private int bMediaState = state.MEDIA_STATE_NONE;



        public SipCallBuilder setCallType(int bCallType) {
            this.bCallType = bCallType;
            return this;
        }

        public SipCallBuilder setMediaState(int state) {
            this.bMediaState = state;
            return this;
        }

        public SipCallBuilder setCallState(int state) {
            this.bCallState = state;
            return this;
        }
        
        private static final String TAG = SipCallBuilder.class.getSimpleName();
        
        public SipCallBuilder startCallCreation(String id) {
            bCallID = id;
            bContacts = new ArrayList<CallContact>();
            bCallType = SipCall.state.CALL_TYPE_INCOMING;
            return this;
        }

        public SipCallBuilder startCallCreation() {
            Random random = new Random();
            bCallID = Integer.toString(random.nextInt());
            bContacts = new ArrayList<CallContact>();
            return this;
        }

        public SipCallBuilder setAccountID(String h) {
            Log.i(TAG, "setAccountID" + h);
            bAccountID = h;
            return this;
        }

        public SipCallBuilder addContact(CallContact c) {
            bContacts.add(c);
            return this;
        }

        public SipCall build() throws Exception {
            if (bCallID.contentEquals("") || bAccountID.contentEquals("") || bContacts.size() == 0) {
                throw new Exception("SipCallBuilder's parameters missing");
            }
            return new SipCall(bCallID, bAccountID, bCallType, bCallState, bMediaState, bContacts);
        }

        public static SipCallBuilder getInstance() {
            return new SipCallBuilder();
        }


    }

    /**
     * Perform hangup action and send request to the service
     */
    public boolean notifyServiceHangup(ISipService service) {
        try {
            if ((getCallStateInt() == state.CALL_STATE_NONE) || (getCallStateInt() == state.CALL_STATE_CURRENT)
                    || (getCallStateInt() == state.CALL_STATE_HOLD)) {
                service.hangUp(mCallID);
                return true;

            } else if (getCallStateInt() == state.CALL_STATE_RINGING) {
                if (getCallType() == state.CALL_TYPE_INCOMING) {
                    service.refuse(mCallID);
                    return true;
                } else if (getCallType() == state.CALL_TYPE_OUTGOING) {
                    service.hangUp(mCallID);
                    return true;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return false;
    }

    public boolean notifyServiceRefuse(ISipService service) {
        try {
            if (getCallStateInt() == state.CALL_STATE_RINGING) {
                service.refuse(mCallID);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return false;
    }

    public boolean notifyServiceHold(ISipService service) {
        try {
            if (getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.hold(mCallID);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return false;
    }

    public boolean notifyServiceUnhold(ISipService service) {
        try {
            if (getCallStateInt() == state.CALL_STATE_HOLD) {
                service.unhold(mCallID);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return false;
    }

    public void addToConference() {
        Log.i(TAG, "Add call to conference");
    }

    public void sendTextMessage() {
        Log.i(TAG, "Send text message");
    }

    // public void printCallInfo() {
    // Log.i(TAG, "CallInfo: CallID: " + mCallID);
    // Log.i(TAG, "          AccountID: " + mAccountID);
    // Log.i(TAG, "          Display Name: " + mDisplayName);
    // Log.i(TAG, "          Phone: " + mPhone);
    // Log.i(TAG, "          Email: " + mEmail);
    // Log.i(TAG, "          Contact: " + mRemoteContact);
    // }

    /**
     * Compare sip calls based on call ID
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof SipCall && ((SipCall) c).mCallID == mCallID) {
            return true;
        }
        return false;

    }

    public void notifyServiceTransfer(ISipService service, String to) {
        try {
            if (getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.transfer(mCallID, to);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void notifyServiceRecord(ISipService service) {
        try {
            if (getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.setRecordPath(Environment.getExternalStorageDirectory().getAbsolutePath());
                Log.w(TAG, "Recording path" + service.getRecordPath());
                service.setRecordingCall(mCallID);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    // public void notifyServiceSendMsg(ISipService service, String msg) {
    // try {
    // if (getCallStateInt() == state.CALL_STATE_CURRENT) {
    // service.sendTextMessage(mCallID, msg, mDisplayName);
    // }
    // } catch (RemoteException e) {
    // Log.e(TAG, "Cannot call service method", e);
    // }
    //
    // }
}
