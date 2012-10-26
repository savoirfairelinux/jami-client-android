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
package com.savoirfairelinux.sflphone.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;

import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.client.CallActivity;
import com.savoirfairelinux.sflphone.client.CallElementList.CallElementView;

public class SipCall
{
    final static String TAG = "SipCall";
    public CallInfo mCallInfo;
    // Update UI on actions (answer, hangup)
    static private CallElementList mCallElementList = null;
    static private SFLPhoneHome mHome = null;
    private View mRowView = null;

    public static final int CALL_STATE_NONE = 0;
    public static final int CALL_STATE_INCOMING = 1;
    public static final int CALL_STATE_RINGING = 2;
    public static final int CALL_STATE_CURRENT = 3;
    public static final int CALL_STATE_HUNGUP = 4;
    public static final int CALL_STATE_BUSY = 5;
    public static final int CALL_STATE_FAILURE = 6;
    public static final int CALL_STATE_HOLD = 7;
    public static final int CALL_STATE_UNHOLD = 8;

    public static final int MEDIA_STATE_NONE = 0;        // No media currently
    public static final int MEDIA_STATE_ACTIVE = 1;      // Media is active
    public static final int MEDIA_STATE_LOCAL_HOLD = 2;  // Media is put on hold bu user
    public static final int MEDIA_STATE_REMOTE_HOLD = 3; // Media is put on hold by peer
    public static final int MEDIA_STATE_ERROR = 5;       // Media is in error state

    public static class CallInfo implements Parcelable
    {
        public String mCallID = "";
        public String mAccountID = "";
        public String mDisplayName = "";
        public String mPhone = "";
        public String mEmail = "";
        public String mRemoteContact = "";
        public int mCallState = CALL_STATE_NONE;
        public int mMediaState = MEDIA_STATE_NONE;

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
            list.add(mDisplayName);
            list.add(mPhone);
            list.add(mEmail);
            list.add(mRemoteContact);

            out.writeStringList(list);
            out.writeInt(mCallState);
            out.writeInt(mMediaState);
        }

        public static final Parcelable.Creator<CallInfo> CREATOR
            = new Parcelable.Creator<CallInfo>() {
            public CallInfo createFromParcel(Parcel in) {
                return new CallInfo(in);
            }

            public CallInfo[] newArray(int size) {
                return new CallInfo[size];
            }
        };

        public CallInfo() {}

        private CallInfo(Parcel in) {
            ArrayList<String> list = in.createStringArrayList();

            // Don't mess with this order!!!
            mCallID = list.get(0);
            mAccountID = list.get(1);
            mDisplayName = list.get(2);
            mPhone = list.get(3);
            mEmail = list.get(4);
            mRemoteContact = list.get(5);

            mCallState = in.readInt();
            mMediaState = in.readInt();
        }
    }

    public SipCall()
    {
        mCallInfo = new CallInfo(); 
    }

    public SipCall(CallInfo info)
    {
        mCallInfo = info; 
    }

    public static void setCallElementList(CallElementList list)
    {
        mCallElementList = list;
    }

    public static void setSFLPhoneHomeContext(SFLPhoneHome home)
    {
        mHome = home;
    }

    public void setAssociatedRowView(View view)
    {
        mRowView = view;
    }

    public void setCallID(String callID) {
        mCallInfo.mCallID = callID;
    }

    public String getCallId() {
        return mCallInfo.mCallID;
    }

    public void setAccountID(String accountID) {
        mCallInfo.mAccountID = accountID;
    }

    public String getAccountID() {
        return mCallInfo.mAccountID;
    }

    public void setDisplayName(String displayName) {
        mCallInfo.mDisplayName = displayName;
    }

    public String getDisplayName() {
        return mCallInfo.mDisplayName;
    }

    public void setPhone(String phone) {
        mCallInfo.mPhone = phone;
    }

    public String getPhone() {
        return mCallInfo.mPhone;
    }

    public void setEmail(String email) {
        mCallInfo.mEmail = email;
    }

    public String getEmail() {
        return mCallInfo.mEmail;
    }

    public void setRemoteContact(String remoteContact) {
        mCallInfo.mRemoteContact = remoteContact;
    }

    public String getRemoteContact() {
        return mCallInfo.mRemoteContact;
    }

    public void setCallState(int callState) {
        mCallInfo.mCallState = callState;
       
        if(mRowView == null)
            return;

        String state;

        switch(mCallInfo.mCallState) {
            case CALL_STATE_INCOMING:
                state = "INCOMING";
                break;
            case CALL_STATE_RINGING:
                state = "RINGING";
                break;
            case CALL_STATE_CURRENT:
                state = "CURRENT";
                break;
            case CALL_STATE_HUNGUP:
                state = "HUNGUP";
                break;
            case CALL_STATE_BUSY:
                state = "BUSY";
                break;
            case CALL_STATE_FAILURE:
                state = "FAILURE";
                break;
            case CALL_STATE_HOLD:
                state = "HOLD";
                break;
            case CALL_STATE_UNHOLD:
                state = "UNHOLD";
                break;
            default:
                state = "NULL";
        }

        CallElementView entryView = (CallElementView) mRowView.getTag();
        final String CURRENT_STATE_LABEL = "    CURRENT STATE: ";
        entryView.state.setText(CURRENT_STATE_LABEL + getCallStateString());
    }

    public int getCallStateInt() {
        return mCallInfo.mCallState;
    }

    public String getCallStateString() {
        String state;

        switch(mCallInfo.mCallState) {
            case CALL_STATE_INCOMING:
                state = "INCOMING";
                break;
            case CALL_STATE_RINGING:
                state = "RINGING";
                break;
            case CALL_STATE_CURRENT:
                state = "CURRENT";
                break;
            case CALL_STATE_HUNGUP:
                state = "HUNGUP";
                break;
            case CALL_STATE_BUSY:
                state = "BUSY";
                break;
            case CALL_STATE_FAILURE:
                state = "FAILURE";
                break;
            case CALL_STATE_HOLD:
                state = "HOLD";
                break;
            case CALL_STATE_UNHOLD:
                state = "UNHOLD";
                break;
            default:
                state = "NULL";
        }

        return state;
    }


    public void setMediaState(int mediaState) {
        mCallInfo.mMediaState = mediaState;
    }

    public int getMediaState() {
        return mCallInfo.mMediaState;
    }

    public void placeCallUpdateUi()
    {
        if(mCallElementList != null)
            mCallElementList.addCall(this);

        if(mHome != null)
            mHome.onSelectedCallAction(this);
    }

    public void notifyServicePlaceCall(ISipService service)
    {
        try {
            service.placeCall(mCallInfo.mAccountID, mCallInfo.mCallID, mCallInfo.mPhone);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void receiveCallUpdateUi()
    {
        if(mCallElementList != null)
            mCallElementList.addCall(this); 

        if(mHome != null)
            mHome.onSelectedCallAction(this);
    }

    public void answerUpdateUi()
    {
        if(mHome != null)
            mHome.onSelectedCallAction(this);
        
    }

    public void notifyServiceAnswer(ISipService service)
    {
        int callState = getCallStateInt();
        if((callState != CALL_STATE_RINGING) &&
           (callState != CALL_STATE_NONE)) {
            return;
        }

        try {
            service.accept(mCallInfo.mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    /**
     * Perform hangup action without sending request to the service
     * Used when SipService haved been notified that this call hung up
     */
    public void hangupUpdateUi() {
        Log.i(TAG, "Hangup call " + mCallInfo.mCallID);

        if(mCallElementList != null)
            mCallElementList.removeCall(this);

        if(mHome != null)
            mHome.onUnselectedCallAction();
    }

    /**
     * Perform hangup action and send request to the service
     */
    public void notifyServiceHangup(ISipService service)
    {
        try {
            service.hangUp(mCallInfo.mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void notifyServiceRefuse(ISipService service)
    {
        try {
            service.refuse(mCallInfo.mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void notifyServiceHold(ISipService service)
    {
        try {
            service.hold(mCallInfo.mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void notifyServiceUnhold(ISipService service)
    {
        try {
            service.unhold(mCallInfo.mCallID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public void addToConference()
    {
        Log.i(TAG, "Add call to conference");
    }

    public void sendTextMessage()
    {
        Log.i(TAG, "Send text message");
    }

    public void printCallInfo()
    {
        Log.i(TAG, "CallInfo: CallID: " + mCallInfo.mCallID);
        Log.i(TAG, "          AccountID: " + mCallInfo.mAccountID);
        Log.i(TAG, "          Display Name: " + mCallInfo.mDisplayName);
        Log.i(TAG, "          Phone: " + mCallInfo.mPhone);
        Log.i(TAG, "          Email: " + mCallInfo.mEmail);
        Log.i(TAG, "          Contact: " + mCallInfo.mRemoteContact);
    }

    public void launchCallActivity(Context context)
    {
        Log.i(TAG, "Launch Call Activity");
        Bundle bundle = new Bundle();
        bundle.putParcelable("CallInfo", mCallInfo);
        Intent intent = new Intent().setClass(context, CallActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
}
