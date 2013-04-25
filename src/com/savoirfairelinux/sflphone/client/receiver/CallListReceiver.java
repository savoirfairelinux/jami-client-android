/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

package com.savoirfairelinux.sflphone.client.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.client.SFLPhoneHomeActivity;

public class CallListReceiver extends BroadcastReceiver
{
    static final String TAG = "CallList";
    static ArrayList<SipCall> mList = new ArrayList<SipCall>();
    // Requires a reference to the main context when sending call activity
    static private SFLPhoneHomeActivity mHome = null;

    private enum Signals {
        NEW_CALL_CREATED,
        INCOMING_CALL,
        INCOMING_MESSAGE,
        CALL_STATE_CHANGED
    }

    /**
     * Factory method to create/retreive call instance
     */
    public static SipCall getCallInstance(SipCall.CallInfo info)
    {
        if(mList.isEmpty()) {
            SipCall call = new SipCall(info);
            mList.add(call);
            return call;
        }

        for(SipCall sipcall : mList) {
            if(sipcall.mCallInfo.mCallID.equals(info.mCallID)) {
                return sipcall;
            }
        }

        SipCall call = new SipCall(info);
        mList.add(call);

        return call;
    }

    public CallListReceiver(SFLPhoneHomeActivity home) {
        mHome = home;
    }

    public CallListReceiver() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String signalName = intent.getStringExtra(CallManagerCallBack.SIGNAL_NAME);
        Log.d(TAG, "Signal received: " + signalName);

        if(signalName.equals(CallManagerCallBack.NEW_CALL_CREATED)) {
            
        } else if(signalName.equals(CallManagerCallBack.CALL_STATE_CHANGED)) {
            processCallStateChangedSignal(intent);
        } else if(signalName.equals(CallManagerCallBack.INCOMING_CALL)) {
            processIncomingCallSignal(intent);
        }
    }

    private void processCallStateChangedSignal(Intent intent) {
        Bundle bundle = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");

        SipCall.CallInfo info = new SipCall.CallInfo();
        info.mCallID = bundle.getString("CallID");

        String newState = bundle.getString("State");

        SipCall call = getCallInstance(info);
        if(newState.equals("INCOMING")) {
            call.setCallState(SipCall.CALL_STATE_INCOMING);
        } else if(newState.equals("RINGING")) {
            call.setCallState(SipCall.CALL_STATE_RINGING);
        } else if(newState.equals("CURRENT")) {
            call.setCallState(SipCall.CALL_STATE_CURRENT);
        } else if(newState.equals("HUNGUP")) {
            call.setCallState(SipCall.CALL_STATE_HUNGUP);
//            call.hangupUpdateUi();
            mList.remove(call);
        } else if(newState.equals("BUSY")) {
            call.setCallState(SipCall.CALL_STATE_BUSY);
        } else if(newState.equals("FAILURE")) {
            call.setCallState(SipCall.CALL_STATE_FAILURE);
        } else if(newState.equals("HOLD")) {
            call.setCallState(SipCall.CALL_STATE_HOLD);
        } else if(newState.equals("UNHOLD")) {
            call.setCallState(SipCall.CALL_STATE_CURRENT);
        } else {
            call.setCallState(SipCall.CALL_STATE_NONE);
        }
    }

    private void processIncomingCallSignal(Intent intent) {
        Bundle bundle = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newcall");
        String accountID = bundle.getString("AccountID");
        String callID = bundle.getString("CallID");
        String from = bundle.getString("From");

        SipCall.CallInfo info = new SipCall.CallInfo();
        info.mCallID = callID;
        info.mAccountID = accountID;
        info.mDisplayName = "Cool Guy!";
        info.mPhone = from;
        info.mEmail = "coolGuy@coolGuy.com";
        info.mCallType = SipCall.CALL_TYPE_INCOMING;
                
        SipCall call = getCallInstance(info);
//        call.receiveCallUpdateUi();
//        call.launchCallActivity(mHome); 
    }
}
