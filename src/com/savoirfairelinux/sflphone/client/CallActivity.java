/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.CallActionsDFragment;
import com.savoirfairelinux.sflphone.fragments.CallFragment;
import com.savoirfairelinux.sflphone.fragments.CallListFragment;
import com.savoirfairelinux.sflphone.interfaces.CallInterface;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.model.SipCall.state;
import com.savoirfairelinux.sflphone.receivers.CallReceiver;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.views.CallPaneLayout;

public class CallActivity extends Activity implements CallInterface, CallFragment.Callbacks, CallListFragment.Callbacks {
    static final String TAG = "CallActivity";
    private ISipService service;

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    CallReceiver receiver;

    CallPaneLayout slidingPaneLayout;

    CallListFragment mCallsFragment;
    CallFragment mCurrentCallFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_layout);

        receiver = new CallReceiver(this);

        mCallsFragment = new CallListFragment();

        getFragmentManager().beginTransaction().replace(R.id.calllist_pane, mCallsFragment).commit();

        slidingPaneLayout = (CallPaneLayout) findViewById(R.id.slidingpanelayout);
        // slidingPaneLayout.
        // slidingPaneLayout.requestDisallowInterceptTouchEvent(disallowIntercept)
        // Toast.makeText(this, getIntent().getData().toString(), Toast.LENGTH_LONG).show();
        slidingPaneLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {

            @Override
            public void onPanelSlide(View view, float offSet) {
            }

            @Override
            public void onPanelOpened(View view) {

                switch (view.getId()) {
                case R.id.calllist_pane:
                    // getFragmentManager().findFragmentById(R.id.calllist_pane).setHasOptionsMenu(true);
                    // getFragmentManager().findFragmentById(R.id.ongoingcall_pane).setHasOptionsMenu(false);
                    break;
                default:
                    break;
                }
            }

            @Override
            public void onPanelClosed(View view) {

                switch (view.getId()) {
                case R.id.ongoingcall_pane:
                    Log.i(TAG, "PANEL CLOSED DRAWING SHOULD RESTART");
                    mCurrentCallFragment.getBubbleView().restartDrawing();
                    break;
                default:
                    break;
                }
            }
        });

        Intent intent = new Intent(this, SipService.class);
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

    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(receiver);

        try {
            service.createNotification();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        // Log.i(TAG, "Destroying Call Activity for call " + mCall.getCallId());
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        try {
            service.destroyNotification();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        unbindService(mConnection);

        super.onDestroy();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
            Log.i(TAG, "Placing call");
            mCurrentCallFragment = new CallFragment();
            Uri u = getIntent().getData();
            if (u != null) {
                CallContact c = CallContact.ContactBuilder.buildUnknownContact(u.getSchemeSpecificPart());
                try {
                    service.destroyNotification();
                    SipCall call = SipCall.SipCallBuilder.getInstance().startCallCreation().addContact(c)
                            .setAccountID(service.getAccountList().get(1).toString()).setCallType(SipCall.state.CALL_TYPE_OUTGOING).build();
                    Bundle b = new Bundle();
                    b.putParcelable("CallInfo", call);
                    mCurrentCallFragment.setArguments(b);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                mCurrentCallFragment.setArguments(getIntent().getExtras());
            }

            slidingPaneLayout.setCurFragment(mCurrentCallFragment);
            getIntent().getExtras();
            mCallsFragment.update();
            getFragmentManager().beginTransaction().replace(R.id.ongoingcall_pane, mCurrentCallFragment).commit();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void incomingCall(Intent call) {
        Toast.makeText(this, "New Call incoming", Toast.LENGTH_LONG).show();

        mCallsFragment.update();

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
        // CallFragment fr = mCurrentCallFragment;

        mCallsFragment.update();

        mCurrentCallFragment.changeCallState(callID, newState);

        HashMap<String, SipCall> map;
        try {
            map = (HashMap<String, SipCall>) service.getCallList();
            if (map.size() == 0) {
                finish();
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        Log.w(TAG, "processCallStateChangedSignal " + newState);

    }

    @Override
    public void incomingText(Intent msg) {
        Bundle b = msg.getBundleExtra("com.savoirfairelinux.sflphone.service.newtext");

        Toast.makeText(this, b.getString("From") + " : " + b.getString("Msg"), Toast.LENGTH_LONG).show();

    }

    @Override
    public ISipService getService() {
        return service;
    }

    @Override
    public void onCallSelected(SipCall call) {

        mCurrentCallFragment.getBubbleView().restartDrawing();
        mCurrentCallFragment = new CallFragment();
        Bundle b = new Bundle();
        b.putParcelable("CallInfo", call);
        mCurrentCallFragment.setArguments(b);
        getFragmentManager().beginTransaction().replace(R.id.ongoingcall_pane, mCurrentCallFragment).commit();

        onCallResumed(call);
        slidingPaneLayout.setCurFragment(mCurrentCallFragment);
        slidingPaneLayout.closePane();

    }

    @Override
    public void callContact(SipCall call) {
        try {
            service.placeCall(call);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    @Override
    public void onCallAccepted(SipCall call) {
        int callState = call.getCallStateInt();
        if (callState != state.CALL_STATE_RINGING && callState != state.CALL_STATE_NONE) {
            return;
        }

        try {
            service.accept(call.getCallId());
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    @Override
    public void onCallRejected(SipCall call) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_RINGING) {
                service.refuse(call.getCallId());
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    @Override
    public void onCallEnded(SipCall call) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_NONE || call.getCallStateInt() == state.CALL_STATE_CURRENT
                    || call.getCallStateInt() == state.CALL_STATE_HOLD) {
                service.hangUp(call.getCallId());
                return;

            } else if (call.getCallStateInt() == state.CALL_STATE_RINGING) {
                if (call.getCallType() == state.CALL_TYPE_INCOMING) {
                    service.refuse(call.getCallId());
                    return;
                } else if (call.getCallType() == state.CALL_TYPE_OUTGOING) {
                    service.hangUp(call.getCallId());
                    return;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    @Override
    public void onCallSuspended(SipCall call) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.hold(call.getCallId());
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    @Override
    public void onCallResumed(SipCall call) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_HOLD) {
                service.unhold(call.getCallId());
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    @Override
    public void onCalltransfered(SipCall call, String to) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.transfer(call.getCallId(), to);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    @Override
    public void onRecordCall(SipCall call) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.setRecordPath(Environment.getExternalStorageDirectory().getAbsolutePath());
                Log.w(TAG, "Recording path" + service.getRecordPath());
                service.setRecordingCall(call.getCallId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent launchHome = new Intent(this, SFLPhoneHomeActivity.class);
        launchHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launchHome);
    }

    @Override
    public void onSendMessage(SipCall call, String msg) {
        try {
            if (call.getCallStateInt() == state.CALL_STATE_CURRENT) {
                service.sendTextMessage(call.getCallId(), msg, "Me");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

    }

}
