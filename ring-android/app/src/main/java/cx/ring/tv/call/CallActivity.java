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
import android.os.Bundle;
import android.text.TextUtils;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Uri;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;

public class CallActivity extends Activity {

    static final String TAG = CallActivity.class.getSimpleName();
    public static final String SHARED_ELEMENT_NAME = "shared_element";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_call);

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        android.net.Uri u = getIntent().getData();

        Log.d(TAG, "u >> " + u);
        boolean hasVideo = true;
        String accountId = getIntent().getStringExtra("account");
        String ringId = getIntent().getStringExtra("ringId");

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        android.util.Log.d(TAG, "IN CALL ACTIVITY !");
        if (!TextUtils.isEmpty(ringId)) {
            Log.d(TAG, " outgoing call");
            Uri number = new Uri(ringId);
            CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_PLACE_CALL,
                    accountId,
                    number,
                    hasVideo);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
        } else {
            Log.d(TAG, "incoming call");

            String confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID);
            Log.d(TAG, "conf " + confId);

            CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_GET_CALL,
                    confId);
            fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
        }
    }
}
