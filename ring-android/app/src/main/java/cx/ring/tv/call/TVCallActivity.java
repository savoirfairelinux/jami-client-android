/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;

public class TVCallActivity extends Activity {

    static final String TAG = TVCallActivity.class.getSimpleName();
    private TVCallFragment callFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.tv_activity_call);

        // dependency injection
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        RingApplication.getInstance().startDaemon();

        android.net.Uri u = getIntent().getData();

        boolean audioOnly = false;
        String accountId = getIntent().getStringExtra("account");
        String ringId = getIntent().getStringExtra("ringId");

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (!TextUtils.isEmpty(ringId)) {
            Log.d(TAG, "onCreate: outgoing call");
            callFragment = TVCallFragment.newInstance(TVCallFragment.ACTION_PLACE_CALL,
                    accountId,
                    ringId,
                    audioOnly);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
        } else {
            Log.d(TAG, "onCreate: incoming call");

            String confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID);
            Log.d(TAG, "onCreate: conf " + confId);

            callFragment = TVCallFragment.newInstance(TVCallFragment.ACTION_GET_CALL,
                    confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
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