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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.fragments.CallFragment;
import cx.ring.model.Uri;

public class CallActivity extends AppCompatActivity {
    static final String TAG = CallActivity.class.getSimpleName();

    public static final String ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call";

    private View mMainView;


    /* result code sent in case of call failure */
    public static int RESULT_FAILURE = -10;

    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private boolean dimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        android.net.Uri u = getIntent().getData();

        String action = getIntent().getAction();
        if (Intent.ACTION_CALL.equals(action) || ACTION_CALL.equals(action)) {
            Uri number = new Uri(u.getSchemeSpecificPart());
            Log.d(TAG, "number " + number);

            boolean hasVideo = getIntent().getBooleanExtra("video", false);
            String accountId = getIntent().getStringExtra("account");


            // Reload a new view
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_PLACE_CALL,
                    accountId,
                    number,
                    hasVideo);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();

        } else if (Intent.ACTION_VIEW.equals(action)) {
            String confId = u.getLastPathSegment();
            Log.d(TAG, "conf " + confId);
            // Reload a new view
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_GET_CALL,
                    confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
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
}
