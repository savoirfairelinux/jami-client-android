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

package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.client.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;

public class CallActivity extends Activity implements OnClickListener 
{
    static final String TAG = "CallActivity";
    private ISipService service;
    private SipCall mCall;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String signalName = intent.getStringExtra(CallManagerCallBack.SIGNAL_NAME);
            Log.d(TAG, "Signal received: " + signalName);

            if(signalName.equals(CallManagerCallBack.NEW_CALL_CREATED)) {
            } else if(signalName.equals(CallManagerCallBack.CALL_STATE_CHANGED)) {
                processCallStateChangedSignal(intent);
            } else if(signalName.equals(CallManagerCallBack.INCOMING_CALL)) {
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_activity_layout);

        Bundle b = getIntent().getExtras();
        // Parcelable value = b.getParcelable("CallInfo");
        SipCall.CallInfo info = b.getParcelable("CallInfo"); 
        Log.i(TAG, "Starting activity for call " + info.mCallID);
        mCall = new SipCall(info); 

        Intent intent = new Intent(this, SipService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.buttonanswer).setOnClickListener(this);
        findViewById(R.id.buttonhangup).setOnClickListener(this);

        setCallStateDisplay(mCall.getCallStateString());
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("new-call-created"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("call-state-changed"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("incoming-call"));
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying Call Activity for call " + mCall.getCallId());
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        unbindService(mConnection);
        super.onDestroy();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void onClick(View view)
    {
        Log.i(TAG, "On click action");
        switch(view.getId()) {
            case R.id.buttonanswer:
                mCall.notifyServiceAnswer(service);
                break;
            case R.id.buttonhangup:
                if((mCall.getCallStateInt() == SipCall.CALL_STATE_NONE) ||
                   (mCall.getCallStateInt() == SipCall.CALL_STATE_CURRENT)) {

                    mCall.notifyServiceHangup(service);
                    finish();
                }
                else if(mCall.getCallStateInt() == SipCall.CALL_STATE_RINGING) {
                    mCall.notifyServiceRefuse(service);
                    finish();
                }
                break;
            default:
                Log.e(TAG, "Invalid button clicked");
        }
    }

    private void processCallStateChangedSignal(Intent intent) {
        Bundle bundle = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
        String callID = bundle.getString("CallID");
        String newState = bundle.getString("State");

        if(newState.equals("INCOMING")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("RINGING")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("CURRENT")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("HUNGUP")) {
            setCallStateDisplay(newState);
            finish();
        } else if(newState.equals("BUSY")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("FAILURE")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("HOLD")) {
            setCallStateDisplay(newState);
        } else if(newState.equals("UNHOLD")) {
            setCallStateDisplay(newState);
        }
    }

    private void setCallStateDisplay(String newState) {
        TextView textView = (TextView)findViewById(R.id.callstate);
        textView.setText("Call State: " + newState);
    }
}
