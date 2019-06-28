/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.call.CallView;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.services.NotificationService;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.MediaButtonsHelper;

public class CallActivity extends AppCompatActivity {
    public static final String ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call";
    public static final String ACTION_CALL_RECEIVE = BuildConfig.APPLICATION_ID + ".action.CALL_RECEIVE";

    private static final String CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG";

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;
    private View mMainView;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private boolean dimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RingApplication.getInstance().startDaemon();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_call_layout);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mMainView = findViewById(R.id.main_call_layout);
        mMainView.setOnClickListener(v -> {
            dimmed = !dimmed;
            if (dimmed) {
                hideSystemUI();
            } else {
                showSystemUI();
            }
        });

        String action = getIntent().getAction();
        CallFragment callFragment;
        if (Intent.ACTION_CALL.equals(action) || ACTION_CALL.equals(action)) {

            boolean audioOnly = getIntent().getBooleanExtra(CallFragment.KEY_AUDIO_ONLY, true);
            String accountId = getIntent().getStringExtra(ConversationFragment.KEY_ACCOUNT_ID);
            String contactRingId = getIntent().getStringExtra(ConversationFragment.KEY_CONTACT_RING_ID);

            // Reload a new view
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            callFragment = CallFragment.newInstance(CallFragment.ACTION_PLACE_CALL,
                    accountId,
                    contactRingId,
                    audioOnly);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();

        } else if (Intent.ACTION_VIEW.equals(action)) {
            String confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID);
            // Reload a new view
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            callFragment = CallFragment.newInstance(CallFragment.ACTION_GET_CALL, confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        }
        else if (ACTION_CALL_RECEIVE.equals(action)) {
            String confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            callFragment = CallFragment.newInstance(ACTION_CALL_RECEIVE, confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        }
    }

    @Override
    public void onUserLeaveHint () {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CALL_FRAGMENT_TAG);
        if (fragment instanceof CallView) {
            CallView callFragment = (CallView) fragment;
            callFragment.onUserLeave();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        currentOrientation = newConfig.orientation;

        // Checks the orientation of the screen
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            dimmed = true;
            hideSystemUI();
        } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            dimmed = false;
            showSystemUI();
        }

        super.onConfigurationChanged(newConfig);
    }

    private void hideSystemUI() {
        KeyboardVisibilityManager.hideKeyboard(this, 0);
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
        if (hasFocus && currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CALL_FRAGMENT_TAG);
        if (fragment instanceof CallFragment) {
            CallFragment callFragment = (CallFragment) fragment;

            return MediaButtonsHelper.handleMediaKeyCode(keyCode, callFragment)
                    || super.onKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }
}
