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

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import cx.ring.R;
import cx.ring.fragments.CallFragment;
import cx.ring.model.Conversation;
import cx.ring.model.SipUri;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.model.account.AccountDetailBasic;
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
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import static cx.ring.service.LocalService.*;

public class CallActivity extends AppCompatActivity implements Callbacks, CallFragment.Callbacks, CallProximityManager.ProximityDirector {
    @SuppressWarnings("unused")
    static final String TAG = "CallActivity";
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
    public void onFragmentCreated() {
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

                if(!checkExternalCall()) {
                    mDisplayedConference = getIntent().getParcelableExtra("conference");
                    if (!mDisplayedConference.hasMultipleParticipants()) {
                        Conversation conv = service.startConversation(mDisplayedConference.getParticipants().get(0).getContact());
                        mDisplayedConference.getParticipants().get(0).setContact(conv.getContact());
                    }
                }
                Log.i(TAG, "CallActivity onCreate in:" + mDisplayedConference.isIncoming() + " out:" + mDisplayedConference.isOnGoing() + " contact" + mDisplayedConference.getParticipants().get(0).getContact().getDisplayName());
                init = true;
            }

            if (mDisplayedConference.getState().contentEquals("NONE")) {
                SipCall call = mDisplayedConference.getParticipants().get(0);
                try {
                    String callId = service.getRemoteService().placeCall(call);
                    if (callId == null || callId.isEmpty()) {
                        CallActivity.this.terminateCall();
                    }
                    mDisplayedConference = service.getRemoteService().getConference(callId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            setContentView(R.layout.activity_call_layout);
            mCurrentCallFragment = (CallFragment) getFragmentManager().findFragmentById(R.id.ongoingcall_pane);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private boolean checkExternalCall() {
        Uri u = getIntent().getData();
        if (u != null) {
            String number = u.getSchemeSpecificPart();
            Log.w(TAG, "number " + number);
            SipUri uri = new SipUri(number);
            number = uri.getRawUriString();
            Log.w(TAG, "canonicalNumber " + number);
            CallContact c = service.findContactByNumber(number);
            Conversation conv = service.getByContact(c);
            if (conv == null)
                conv = new Conversation(c);
            Account acc = service.getAccounts().get(0);
            String id = conv.getLastAccountUsed();
            if (id != null && !id.isEmpty()) {
                Account alt_acc = service.getAccount(id);
                Log.w(TAG, "Found suitable account for calling " + u.getSchemeSpecificPart() + " " + id + " " + alt_acc.getBasicDetails().getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_TYPE));
                if (alt_acc.isEnabled())
                    acc = alt_acc;
            } else {
                acc = service.guessAccount(c, number);
            }
            try {
                SipCall call = new SipCall(null, acc.getAccountID(), number, SipCall.Direction.OUTGOING);
                call.setCallState(SipCall.State.NONE);
                call.setContact(c);
                mDisplayedConference = new Conference(Conference.DEFAULT_ID);
                mDisplayedConference.getParticipants().add(call);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
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
