/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.fragments.CallFragment;
import net.jami.services.NotificationService;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.utils.MediaButtonsHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CallActivity extends AppCompatActivity {
    public static final String ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call";
    public static final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";

    private static final String CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG";

    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;
    private View mMainView;
    private Handler handler;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private boolean dimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();

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

        handler = new Handler(Looper.getMainLooper());

        mMainView = findViewById(R.id.main_call_layout);
        mMainView.setOnClickListener(v -> {
            dimmed = !dimmed;
            if (dimmed) {
                hideSystemUI();
            } else {
                showSystemUI();
            }
        });

        Intent intent = getIntent();
        if (intent != null)
            handleNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restartNoInteractionTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (handler != null) {
            handler.removeCallbacks(onNoInteraction);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNewIntent(intent);
    }

    private void handleNewIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_CALL.equals(action) || ACTION_CALL.equals(action)) {
            boolean audioOnly = intent.getBooleanExtra(CallFragment.KEY_AUDIO_ONLY, true);
            String contactId = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_PLACE_CALL,
                    ConversationPath.fromIntent(intent),
                    contactId,
                    audioOnly);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        } else if (Intent.ACTION_VIEW.equals(action) || ACTION_CALL_ACCEPT.equals(action)) {
            String confId = intent.getStringExtra(NotificationService.KEY_CALL_ID);
            CallFragment callFragment = CallFragment.newInstance(Intent.ACTION_VIEW.equals(action) ? CallFragment.ACTION_GET_CALL : ACTION_CALL_ACCEPT, confId);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        }
    }

    private final Runnable onNoInteraction = () -> {
        if (!dimmed) {
            dimmed = true;
            hideSystemUI();
        }
    };

    public void restartNoInteractionTimer() {
        if (handler != null) {
            handler.removeCallbacks(onNoInteraction);
            handler.postDelayed(onNoInteraction, 4 * 1000);
        }
    }

    @Override
    public void onUserLeaveHint() {
        CallFragment callFragment = getCallFragment();
        if (callFragment != null) {
            callFragment.onUserLeave();
        }
    }

    @Override
    public void onUserInteraction() {
        restartNoInteractionTimer();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (currentOrientation != newConfig.orientation) {
            currentOrientation = newConfig.orientation;
            if (dimmed)
                hideSystemUI();
            else
                showSystemUI();
        } else {
            restartNoInteractionTimer();
        }
        super.onConfigurationChanged(newConfig);
    }

    private void hideSystemUI() {
        KeyboardVisibilityManager.hideKeyboard(this);
        if (mMainView != null) {
            mMainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);

            CallFragment callFragment = getCallFragment();
            if(callFragment != null && !callFragment.isChoosePluginMode()) {
                callFragment.toggleVideoPluginsCarousel(false);
            }
            if (handler != null)
                handler.removeCallbacks(onNoInteraction);
        }
    }

    public void showSystemUI() {
        if (mMainView != null) {
            mMainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);

            CallFragment callFragment = getCallFragment();
            if (callFragment != null) {
                callFragment.toggleVideoPluginsCarousel(true);
            }
            restartNoInteractionTimer();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CallFragment callFragment = getCallFragment();
        if (callFragment != null) {
            return MediaButtonsHelper.handleMediaKeyCode(keyCode, callFragment)
                    || super.onKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    private CallFragment getCallFragment() {
        CallFragment callFragment = null;
        // Get the call Fragment
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CALL_FRAGMENT_TAG);
        if (fragment instanceof CallFragment) {
            callFragment = (CallFragment) fragment;
        }
        return callFragment;
    }
}
