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
import java.util.ArrayList;

import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.client.CallActivity;

public class SipCall
{
    final static String TAG = "SipCall";
    public static CallElementList mCallElementList = null;
    public CallInfo mCallInfo;

    public static int CALL_STATE_INVALID = 0;      // The call is not existent in SFLphone service
    public static int CALL_STATE_NULL = 1;         // Before any action performed
    public static int CALL_STATE_CALLING = 2;      // After INVITE is sent
    public static int CALL_STATE_INCOMING = 3;     // After INVITE is received
    public static int CALL_STATE_EARLY = 4;        // After response with To tag
    public static int CALL_STATE_CONNECTING = 5;   // After 2xx is sent/received
    public static int CALL_STATE_CONFIRMED = 6;    // After ACK is sent/received
    public static int CALL_STATE_DISCONNECTED = 7; // Session is terminated

    public static int MEDIA_STATE_NONE = 0;        // No media currently
    public static int MEDIA_STATE_ACTIVE = 1;      // Media is active
    public static int MEDIA_STATE_LOCAL_HOLD = 2;  // Media is put on hold bu user
    public static int MEDIA_STATE_REMOTE_HOLD = 3; // Media is put on hold by peer
    public static int MEDIA_STATE_ERROR = 5;       // Media is in error state

    public static class CallInfo implements Parcelable
    {
        public String mCallID = "";
        public String mDisplayName = "";
        public String mPhone = "";
        public String mEmail = "";
        public String mRemoteContact = "";
        public int mCallState = CALL_STATE_NULL;
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
            mDisplayName = list.get(1);
            mPhone = list.get(2);
            mEmail = list.get(3);
            mRemoteContact = list.get(4);

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

    public void placeCall()
    {
        if(mCallElementList != null)
            mCallElementList.addCall(this); 
        // mManager.callmanagerJNI.placeCall("IP2IP", "CALL1234", "192.168.40.35");
    }

    public void answer()
    {

    }

    /**
     * Perform hangup action without sending request to the service
     */
    public void hangup() {
        Log.i(TAG, "Hangup call " + mCallInfo.mCallID);

        if(mCallElementList != null)
            mCallElementList.removeCall(this);
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

    public void addToConference()
    {
        Log.i(TAG, "Add call to conference");
    }

    public void sendTextMessage()
    {
        Log.i(TAG, "Send text message");
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
