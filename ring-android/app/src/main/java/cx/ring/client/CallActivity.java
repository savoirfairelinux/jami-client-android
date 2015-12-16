/*
 *  Copyright (C) 2004-2015 Savoir-faire Linux Inc.
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

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.fragments.CallFragment;
import cx.ring.model.Conversation;
import cx.ring.model.SipUri;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.utils.CallProximityManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import static cx.ring.service.LocalService.*;

public class CallActivity extends AppCompatActivity implements Callbacks, CallFragment.Callbacks, CallProximityManager.ProximityDirector {
    static final String TAG = CallActivity.class.getSimpleName();

    public static final String ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call";

    private boolean init = false;
    private LocalService service;

    private CallFragment mCurrentCallFragment;
    private Conference mDisplayedConference;

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;
    private CallProximityManager mProximityManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "CallActivity onCreate");
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        Intent intent = new Intent(this, LocalService.class);
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
        Log.i(TAG, "CallActivity onDestroy");
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
            service = ((LocalService.LocalBinder)binder).getService();

            if (!init) {
                mProximityManager = new CallProximityManager(CallActivity.this, CallActivity.this);
                mProximityManager.startTracking();

                checkExternalCall();

                if (mDisplayedConference == null || mDisplayedConference.getParticipants().isEmpty()) {
                    CallActivity.this.finish();
                    return;
                }

                Log.i(TAG, "CallActivity onCreate in:" + mDisplayedConference.isIncoming() + " out:" + mDisplayedConference.isOnGoing() + " contact" + mDisplayedConference.getParticipants().get(0).getContact().getDisplayName());
                init = true;
            }

            setContentView(R.layout.activity_call_layout);
            mCurrentCallFragment = (CallFragment) getFragmentManager().findFragmentById(R.id.ongoingcall_pane);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private Pair<Account, String> guess(String number, String account_id) {
        Account a = service.getAccount(account_id);
        Conversation conv = service.findConversationByNumber(number);

        // Guess account from number
        if (a == null && number != null)
            a = service.guessAccount(conv.getContact(), number);

        // Guess number from account/call history
        if (a != null && (number == null/* || number.isEmpty()*/))
            number = CallContact.canonicalNumber(conv.getLastNumberUsed(a.getAccountID()));

        // If no account found, use first active
        if (a == null)
            a = service.getAccounts().get(0);

        // If no number found, use first from contact
        if (number == null || number.isEmpty())
            number = CallContact.canonicalNumber(conv.contact.getPhones().get(0).getNumber());

        return new Pair<>(a, number);
    }

    private boolean checkExternalCall() {
        Log.w(TAG, "intent " + getIntent().toString());

        Uri u = getIntent().getData();
        if (u == null) {
            terminateCall();
            return false;
        }

        Log.w(TAG, "uri " + u.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_CALL.equals(action) || ACTION_CALL.equals(action)) {
            String number = u.getSchemeSpecificPart();
            Log.w(TAG, "number " + number);
            SipUri uri = new SipUri(number);
            number = uri.getRawUriString();
            Log.w(TAG, "canonicalNumber " + number);

            Pair<Account, String> g = guess(number, getIntent().getStringExtra("account"));

            mDisplayedConference = service.placeCall(new SipCall(null, g.first.getAccountID(), g.second, SipCall.Direction.OUTGOING));
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String conf_id = u.getLastPathSegment();
            Log.w(TAG, "conf " + conf_id);
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
        if(mDisplayedConference.equals(c)){
            mDisplayedConference = c;
        }
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
