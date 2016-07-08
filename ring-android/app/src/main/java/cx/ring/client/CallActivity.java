/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 */

package cx.ring.client;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.fragments.CallFragment;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.SipUri;
import cx.ring.model.account.Account;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.utils.CallProximityManager;

import static cx.ring.service.LocalService.Callbacks;

public class CallActivity extends AppCompatActivity implements Callbacks, CallFragment.ConversationCallbacks, CallProximityManager.ProximityDirector {
    static final String TAG = CallActivity.class.getSimpleName();

    public static final String ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call";

    private boolean init = false;
    private View mMainView;

    private LocalService service;

    private CallFragment mCurrentCallFragment;
    private Conference mDisplayedConference;
    private String mSavedConferenceId = null;

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;
    private CallProximityManager mProximityManager = null;

    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private boolean dimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            mSavedConferenceId = savedInstanceState.getString("conference", null);
        Log.d(TAG, "CallActivity onCreate " + mSavedConferenceId);

        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        Window w = getWindow();
        w.setFlags(flags, flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.activity_call_layout);

        mMainView = findViewById(R.id.main_call_layout);
        mMainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dimmed = !dimmed;
                if (dimmed) {
                    hideSystemUI();
                } else {
                    showSystemUI();
                }
            }
        });

        Intent intent = new Intent(this, LocalService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged " + newConfig.screenWidthDp);

        currentOrientation = newConfig.orientation;

        // Checks the orientation of the screen
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            dimmed = true;
            hideSystemUI();
        } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            dimmed = false;
            showSystemUI();
        }

        // Reload a new view
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mCurrentCallFragment = new CallFragment();
        fragmentTransaction.replace(R.id.main_call_layout, mCurrentCallFragment).commit();

        super.onConfigurationChanged(newConfig);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        if (mMainView != null) {
            mMainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        if (mMainView != null) {
            mMainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
            hideSystemUI();
        else
            showSystemUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDisplayedConference != null)
            outState.putString("conference", mDisplayedConference.getId());
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
    protected void onDestroy() {
        Log.d(TAG, "CallActivity onDestroy");
        unbindService(mConnection);
        if (mProximityManager != null) {
            mProximityManager.stopTracking();
            mProximityManager.release(0);
        }

        super.onDestroy();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder) binder).getService();

            if (!init) {
                mProximityManager = new CallProximityManager(CallActivity.this, CallActivity.this);
                mProximityManager.startTracking();

                if (mSavedConferenceId != null) {
                    mDisplayedConference = service.getConference(mSavedConferenceId);
                } else {
                    checkExternalCall();
                }

                if (mDisplayedConference == null || mDisplayedConference.getParticipants().isEmpty()) {
                    Log.e(TAG, "Conference displayed is null or empty");
                    CallActivity.this.finish();
                    return;
                }

                Log.i(TAG, "CallActivity onCreate in:" + mDisplayedConference.isIncoming() +
                        " out:" + mDisplayedConference.isOnGoing());
                CallContact contact = mDisplayedConference.getParticipants().get(0).getContact();
                if (null != contact) {
                    Log.i(TAG, "CallActivity onCreate contact:" + contact.getDisplayName());
                }

                init = true;
            }

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            mCurrentCallFragment = new CallFragment();
            fragmentTransaction.add(R.id.main_call_layout, mCurrentCallFragment).commit();
            hideSystemUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private Pair<Account, SipUri> guess(SipUri number, String account_id) {
        Account a = service.getAccount(account_id);
        Conversation conv = service.findConversationByNumber(number);

        // Guess account from number
        if (a == null && number != null)
            a = service.guessAccount(number);

        // Guess number from account/call history
        if (a != null && (number == null || number.isEmpty()))
            number = new SipUri(conv.getLastNumberUsed(a.getAccountID()));

        // If no account found, use first active
        if (a == null)
            a = service.getAccounts().get(0);

        // If no number found, use first from contact
        if (number == null || number.isEmpty())
            number = conv.contact.getPhones().get(0).getNumber();

        return new Pair<>(a, number);
    }

    private boolean checkExternalCall() {
        Log.d(TAG, "intent " + getIntent().toString());

        if (getIntent() == null) {
            terminateCall();
            return false;
        }

        Uri u = getIntent().getData();
        if (u == null) {
            terminateCall();
            return false;
        }

        Log.d(TAG, "uri " + u.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_CALL.equals(action) || ACTION_CALL.equals(action)) {
            SipUri number = new SipUri(u.getSchemeSpecificPart());
            Log.d(TAG, "number " + number);

            boolean hasVideo = getIntent().getBooleanExtra("video", false);
            Pair<Account, SipUri> g = guess(number, getIntent().getStringExtra("account"));

            SipCall call = new SipCall(null, g.first.getAccountID(), g.second, SipCall.Direction.OUTGOING);
            call.muteVideo(!hasVideo);

            mDisplayedConference = service.placeCall(call);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String conf_id = u.getLastPathSegment();
            Log.d(TAG, "conf " + conf_id);
            mDisplayedConference = service.getConference(conf_id);
        }

        return false;
    }

    @Override
    public IDRingService getRemoteService() {
        return service.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return service;
    }

    @Override
    public Conference getDisplayedConference() {
        return mDisplayedConference;
    }

    @Override
    public void updateDisplayedConference(Conference c) {
        mDisplayedConference = c;
    }

    @Override
    public void terminateCall() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        finish();
    }

    @Override
    public void startTimer() {
        mHandler.postDelayed(mUpdateTimeTask, 0);
    }

    @Override
    public boolean shouldActivateProximity() {
        return true;
    }

    @Override
    public void onProximityTrackingChanged(boolean acquired) {
    }
}
