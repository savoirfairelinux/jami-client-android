/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.call;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cx.ring.R;
import cx.ring.application.JamiApplication;

import net.jami.call.CallView;

import cx.ring.utils.ConversationPath;
import dagger.hilt.android.AndroidEntryPoint;

import net.jami.services.NotificationService;
import net.jami.utils.Log;

@AndroidEntryPoint
public class TVCallActivity extends FragmentActivity {

    static final String TAG = TVCallActivity.class.getSimpleName();
    private static final String CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG";

    private TVCallFragment callFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.tv_activity_call);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        JamiApplication.getInstance().startDaemon();

        ConversationPath path = ConversationPath.fromIntent(intent);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (path != null) {
            Log.d(TAG, "onCreate: outgoing call " + path);
            callFragment = TVCallFragment.newInstance(intent.getAction(),
                    path.getAccountId(),
                    path.getConversationId(),
                    intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER, path.getConversationId()),
                    false);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        } else {
            Log.d(TAG, "onCreate: incoming call");

            String confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID);
            Log.d(TAG, "onCreate: conf " + confId);

            callFragment = TVCallFragment.newInstance(Intent.ACTION_VIEW, confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit();
        }
    }

    @Override
    public void onUserLeaveHint() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CALL_FRAGMENT_TAG);
        if (fragment instanceof CallView) {
            CallView callFragment = (CallView) fragment;
            callFragment.onUserLeave();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        callFragment.onKeyDown();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        callFragment.onKeyDown();
        return super.onTouchEvent(event);
    }
}