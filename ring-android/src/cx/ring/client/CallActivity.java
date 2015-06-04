/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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

package cx.ring.client;

import java.util.*;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import cx.ring.R;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.IMFragment;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.model.SipMessage;
import cx.ring.service.ISipService;
import cx.ring.service.SipService;
import cx.ring.utils.CallProximityManager;
import cx.ring.views.CallPaneLayout;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class CallActivity extends FragmentActivity implements IMFragment.Callbacks, CallFragment.Callbacks, CallProximityManager.ProximityDirector {

    @SuppressWarnings("unused")
    static final String TAG = "CallActivity";
    private ISipService mService;
    CallPaneLayout mSlidingPaneLayout;

    IMFragment mIMFragment;
    CallFragment mCurrentCallFragment;
    private Conference mDisplayedConference;

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;
    private CallProximityManager mProximityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_layout);

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setUpSlidingPanel();

        mProximityManager = new CallProximityManager(this, this);
        mProximityManager.startTracking();

        mCurrentCallFragment = new CallFragment();
        mIMFragment = new IMFragment();

        if(!checkExternalCall()) {
            mDisplayedConference = getIntent().getParcelableExtra("conference");
            Bundle IMBundle = new Bundle();
            if (getIntent().getBooleanExtra("resuming", false)) {
                IMBundle.putParcelableArrayList("messages", mDisplayedConference.getMessages());
                mIMFragment.setArguments(IMBundle);
            } else {
                IMBundle.putParcelableArrayList("messages", new ArrayList<SipMessage>());
                mIMFragment.setArguments(IMBundle);
            }
        }

        mSlidingPaneLayout.setCurFragment(mCurrentCallFragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.ongoingcall_pane, mCurrentCallFragment)
                .replace(R.id.message_list_frame, mIMFragment).commit();
    }

    private void setUpSlidingPanel() {
        mSlidingPaneLayout = (CallPaneLayout) findViewById(R.id.slidingpanelayout);
        mSlidingPaneLayout.setParallaxDistance(500);
        mSlidingPaneLayout.setSliderFadeColor(Color.TRANSPARENT);

        mSlidingPaneLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {

            @Override
            public void onPanelSlide(View view, float offSet) {
            }

            @Override
            public void onPanelOpened(View view) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            }

            @Override
            public void onPanelClosed(View view) {
                mCurrentCallFragment.getBubbleView().restartDrawing();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }
        });
    }

    @Override
    public void onFragmentCreated() {
        Intent intent = new Intent(this, SipService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

        unbindService(mConnection);

        mProximityManager.stopTracking();
        mProximityManager.release(0);

        super.onDestroy();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = ISipService.Stub.asInterface(binder);

            if (mDisplayedConference.getState().contentEquals("NONE")) {
                try {
                    mService.placeCall(mDisplayedConference.getParticipants().get(0));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private boolean checkExternalCall() {
        Uri u = getIntent().getData();
        if (u != null) {
            CallContact c = CallContact.ContactBuilder.buildUnknownContact(u.getSchemeSpecificPart());
            try {
                String accountID = (String) mService.getAccountList().get(1); // We use the first account to place outgoing calls
                Map<String, String> details = (Map<String, String>) mService.getAccountDetails(accountID);
                ArrayList<Map<String, String>> credentials = (ArrayList<Map<String, String>>) mService.getCredentials(accountID);
                Account acc = new Account(accountID, details, credentials);

                Bundle args = new Bundle();
                args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
                args.putParcelable(SipCall.ACCOUNT, acc);
                args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_NONE);
                args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_OUTGOING);
                args.putParcelable(SipCall.CONTACT, c);

                mDisplayedConference = new Conference(Conference.DEFAULT_ID);
                mDisplayedConference.getParticipants().add(new SipCall(args));
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public ISipService getService() {
        return mService;
    }

    @Override
    public Conference getDisplayedConference() {
        return mDisplayedConference;
    }

    @Override
    public void updateDisplayedConference(Conference c) {
        if(mDisplayedConference.equals(c)){
            mDisplayedConference = c;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent launchHome = new Intent(this, HomeActivity.class);
        launchHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchHome.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launchHome);
    }

    @Override
    public void terminateCall() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mCurrentCallFragment.getBubbleView().stopThread();
        TimerTask quit = new TimerTask() {

            @Override
            public void run() {
                finish();
            }
        };

        new Timer().schedule(quit, 1000);
    }

    @Override
    public boolean sendIM(SipMessage msg) {

        try {
            Log.i(TAG, "Sending:"+msg.comment+"to"+mDisplayedConference.getId());
            mService.sendTextMessage(mDisplayedConference.getId(), msg);
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

    @Override
    public void slideChatScreen() {

        if (mSlidingPaneLayout.isOpen()) {
            mSlidingPaneLayout.closePane();
        } else {
            mCurrentCallFragment.getBubbleView().stopThread();
            mSlidingPaneLayout.openPane();
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
