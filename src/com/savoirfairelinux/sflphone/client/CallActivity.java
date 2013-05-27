/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.CallPagerAdapter;
import com.savoirfairelinux.sflphone.client.receiver.CallReceiver;
import com.savoirfairelinux.sflphone.fragments.CallFragment;
import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;

public class CallActivity extends Activity implements CallInterface, CallFragment.Callbacks {
    static final String TAG = "CallActivity";
    private ISipService service;

    private String pendingAction = null;

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    CallReceiver receiver;

    ViewPager vp;
    private CallPagerAdapter mCallPagerAdapter;
    private ViewPager mViewPager;

    /*
     * private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
     * 
     * @Override public void onReceive(Context context, Intent intent) { String signalName = intent.getStringExtra(CallManagerCallBack.SIGNAL_NAME);
     * Log.d(TAG, "Signal received: " + signalName);
     * 
     * if (signalName.equals(CallManagerCallBack.NEW_CALL_CREATED)) { } else if (signalName.equals(CallManagerCallBack.CALL_STATE_CHANGED)) {
     * processCallStateChangedSignal(intent); } else if (signalName.equals(CallManagerCallBack.INCOMING_CALL)) { } } };
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_layout);

        receiver = new CallReceiver(this);

        if (mCallPagerAdapter == null) {
            mCallPagerAdapter = new CallPagerAdapter(this, getFragmentManager());
        }

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mViewPager.setAdapter(mCallPagerAdapter);

        Bundle b = getIntent().getExtras();

        Intent intent = new Intent(this, SipService.class);

        // setCallStateDisplay(mCall.getCallStateString());


        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        super.onResume();
    }

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        // Log.i(TAG, "Destroying Call Activity for call " + mCall.getCallId());
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        unbindService(mConnection);

        super.onDestroy();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
                Log.i(TAG, "Placing call");
                CallFragment newCall = new CallFragment();
                newCall.setArguments(getIntent().getExtras());
                getIntent().getExtras();
                SipCall.CallInfo info = getIntent().getExtras().getParcelable("CallInfo");
                mCallPagerAdapter.addCall(info.mCallID, newCall);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void incomingCall(Intent call) {
        Toast.makeText(this, "New Call incoming", Toast.LENGTH_LONG).show();

        // TODO Handle multicall here

    }

    @Override
    public void callStateChanged(Intent callState) {

        Bundle b = callState.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
        processCallStateChangedSignal(b.getString("CallID"), b.getString("State"));

    }

    public void processCallStateChangedSignal(String callID, String newState) {
        /*
         * Bundle bundle = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate"); String callID = bundle.getString("CallID"); String
         * newState = bundle.getString("State");
         */
        CallFragment fr = (CallFragment) mCallPagerAdapter.getCall(callID);

        if (newState.equals("INCOMING")) {
            fr.changeCallState(SipCall.CALL_STATE_INCOMING);

        } else if (newState.equals("RINGING")) {
            fr.changeCallState(SipCall.CALL_STATE_RINGING);

        } else if (newState.equals("CURRENT")) {
            fr.changeCallState(SipCall.CALL_STATE_CURRENT);

        } else if (newState.equals("HUNGUP")) {
//            mCallPagerAdapter.remove(callID);

        } else if (newState.equals("BUSY")) {
//            mCallPagerAdapter.remove(callID);

        } else if (newState.equals("FAILURE")) {
//            mCallPagerAdapter.remove(callID);

        } else if (newState.equals("HOLD")) {
            fr.changeCallState(SipCall.CALL_STATE_HOLD);

        } else if (newState.equals("UNHOLD")) {
            fr.changeCallState(SipCall.CALL_STATE_CURRENT);

        } else {
            fr.changeCallState(SipCall.CALL_STATE_NONE);

        }

        Log.w(TAG, "processCallStateChangedSignal " + newState);

    }

    @Override
    public void incomingText(Intent msg) {
        Toast.makeText(this, "New Call incoming", Toast.LENGTH_LONG).show();

        // TODO link text message to associate call and display it at the right place

    }

    @Override
    public ISipService getService() {
        return service;
    }

}
