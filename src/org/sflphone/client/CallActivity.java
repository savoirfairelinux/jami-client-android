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

package org.sflphone.client;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.R;
import org.sflphone.fragments.CallFragment;
import org.sflphone.fragments.IMFragment;
import org.sflphone.interfaces.CallInterface;
import org.sflphone.model.Account;
import org.sflphone.model.CallContact;
import org.sflphone.model.Conference;
import org.sflphone.model.SipCall;
import org.sflphone.model.SipMessage;
import org.sflphone.receivers.CallReceiver;
import org.sflphone.service.CallManagerCallBack;
import org.sflphone.service.ISipService;
import org.sflphone.service.SipService;
import org.sflphone.utils.CallProximityManager;
import org.sflphone.utils.CallProximityManager.ProximityDirector;
import org.sflphone.views.CallPaneLayout;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class CallActivity extends Activity implements CallInterface, IMFragment.Callbacks, CallFragment.Callbacks, ProximityDirector {
    static final String TAG = "CallActivity";
    private ISipService service;

    CallReceiver receiver;

    CallPaneLayout slidingPaneLayout;

    IMFragment mIMFragment;
    CallFragment mCurrentCallFragment;
    // private boolean fragIsChanging;

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;

    private CallProximityManager proximityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_layout);

        receiver = new CallReceiver(this);

        proximityManager = new CallProximityManager(this, this);

        slidingPaneLayout = (CallPaneLayout) findViewById(R.id.slidingpanelayout);

        slidingPaneLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {

            @Override
            public void onPanelSlide(View view, float offSet) {
            }

            @Override
            public void onPanelOpened(View view) {

            }

            @Override
            public void onPanelClosed(View view) {
                mCurrentCallFragment.getBubbleView().restartDrawing();
            }
        });

        proximityManager.startTracking();
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
        intentFilter.addAction(CallManagerCallBack.CONF_CREATED);
        intentFilter.addAction(CallManagerCallBack.CONF_REMOVED);
        intentFilter.addAction(CallManagerCallBack.CONF_CHANGED);
        intentFilter.addAction(CallManagerCallBack.RECORD_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);

        super.onResume();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentCallFragment != null)
                mCurrentCallFragment.updateTime();
            // mCallsFragment.update();

            mHandler.postAtTime(this, SystemClock.uptimeMillis() + 1000);
        }
    };

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyUp(keyCode, event);
        }
        mCurrentCallFragment.onKeyUp(keyCode, event);
        return true;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        unbindService(mConnection);
        proximityManager.stopTracking();
        proximityManager.release(0);
        super.onDestroy();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);

            mCurrentCallFragment = new CallFragment();
            mIMFragment = new IMFragment();

            Uri u = getIntent().getData();
            if (u != null) {
                CallContact c = CallContact.ContactBuilder.buildUnknownContact(u.getSchemeSpecificPart());
                try {
                    service.destroyNotification();

                    String accountID = (String) service.getAccountList().get(1); // We use the first account to place outgoing calls
                    Account acc = new Account(accountID, (HashMap<String, String>) service.getAccountDetails(accountID));

                    SipCall call = SipCall.SipCallBuilder.getInstance().startCallCreation().setContact(c).setAccount(acc)
                            .setCallType(SipCall.state.CALL_TYPE_OUTGOING).build();
                    Conference tmp = new Conference("-1");
                    tmp.getParticipants().add(call);
                    Bundle b = new Bundle();
                    b.putParcelable("conference", tmp);
                    mCurrentCallFragment.setArguments(b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (getIntent().getBooleanExtra("resuming", false)) {

                    Bundle b = new Bundle();
                    Conference resumed = getIntent().getParcelableExtra("conference");
                    b.putParcelable("conference", getIntent().getParcelableExtra("conference"));
                    mCurrentCallFragment.setArguments(b);

                    Bundle IMBundle = new Bundle();
                    IMBundle.putParcelableArrayList("messages", resumed.getMessages());
                    mIMFragment.setArguments(IMBundle);

                } else {
                    mCurrentCallFragment.setArguments(getIntent().getExtras());
                    
                    Bundle IMBundle = new Bundle();
                    IMBundle.putParcelableArrayList("messages", new ArrayList<SipMessage>());
                    mIMFragment.setArguments(IMBundle);
                }

            }

            slidingPaneLayout.setCurFragment(mCurrentCallFragment);
            getIntent().getExtras();
            // mCallsFragment.update();
            getFragmentManager().beginTransaction().replace(R.id.ongoingcall_pane, mCurrentCallFragment).commit();
            getFragmentManager().beginTransaction().replace(R.id.message_list_frame, mIMFragment).commit();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void incomingCall(Intent call) {
        Bundle b = new Bundle();
        Conference tmp = new Conference("-1");
        tmp.getParticipants().add((SipCall) call.getParcelableExtra("newcall"));
        b.putParcelable("conference", tmp);
        mCurrentCallFragment = new CallFragment();
        mCurrentCallFragment.setArguments(b);
        getFragmentManager().beginTransaction().replace(R.id.ongoingcall_pane, mCurrentCallFragment).commit();
        slidingPaneLayout.setCurFragment(mCurrentCallFragment);

    }

    @Override
    public void callStateChanged(Intent callState) {

        Bundle b = callState.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
        processCallStateChangedSignal(b.getString("CallID"), b.getString("State"));

    }

    @SuppressWarnings("unchecked")
    // No proper solution with HashMap runtime cast
    public void processCallStateChangedSignal(String callID, String newState) {

        if (mCurrentCallFragment != null) {
            mCurrentCallFragment.changeCallState(callID, newState);
        }

        proximityManager.updateProximitySensorMode();

        try {
            HashMap<String, SipCall> callMap = (HashMap<String, SipCall>) service.getCallList();
            HashMap<String, Conference> confMap = (HashMap<String, Conference>) service.getConferenceList();

        } catch (RemoteException e) {

            Log.e(TAG, e.toString());
        }

        Log.w(TAG, "processCallStateChangedSignal " + newState);

    }

    @Override
    public void incomingText(Intent in) {
        Bundle b = in.getBundleExtra("com.savoirfairelinux.sflphone.service.newtext");

        if (mIMFragment != null) {
            SipMessage msg = new SipMessage(true, b.getString("Msg"));
            mIMFragment.putMessage(msg);
        }

    }

    @Override
    public ISipService getService() {
        return service;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent launchHome = new Intent(this, SFLPhoneHomeActivity.class);
        launchHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchHome.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launchHome);
    }

    @Override
    public void confCreated(Intent intent) {
        // mCallsFragment.update();

    }

    @Override
    public void confRemoved(Intent intent) {
        // mCallsFragment.update();
    }

    @Override
    public void confChanged(Intent intent) {
        // mCallsFragment.update();
    }

    @Override
    public void recordingChanged(Intent intent) {
        // mCallsFragment.update();
    }

    @Override
    public void terminateCall() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mCurrentCallFragment.getBubbleView().stopThread();
        // getFragmentManager().beginTransaction().remove(mCurrentCallFragment).commit();
        // mCurrentCallFragment = null;
        finish();

    }

    @Override
    public boolean sendIM(SipMessage msg) {

        try {
            service.sendTextMessage(mCurrentCallFragment.getConference().getId(), msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void startTimer() {
        mHandler.postDelayed(mUpdateTimeTask, 0);
    }

    public void onCallSuspended() {
        try {
            if (mCurrentCallFragment.getConference().hasMultipleParticipants()) {
                service.holdConference(mCurrentCallFragment.getConference().getId());
            } else {
                service.hold(mCurrentCallFragment.getConference().getParticipants().get(0).getCallId());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void slideChatScreen() {

        if (slidingPaneLayout.isOpen()) {
            slidingPaneLayout.closePane();
        } else {
            mCurrentCallFragment.getBubbleView().stopThread();
            slidingPaneLayout.openPane();
        }
    }

    @Override
    public boolean shouldActivateProximity() {
        return true;
    }

    @Override
    public void onProximityTrackingChanged(boolean acquired) {
        // TODO Stub de la méthode généré automatiquement

    }
}
