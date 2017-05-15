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

package cx.ring.client;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import javax.inject.Inject;

import cx.ring.Call.CallFragment;
import cx.ring.Call.CallPresenter;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Uri;

public class CallActivity extends Activity {

    static final String TAG = CallActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        android.net.Uri u = getIntent().getData();

        boolean hasVideo = true;
        String accountId = getIntent().getStringExtra("account");
//        Uri number = new Uri(u.getSchemeSpecificPart());
        Uri number = new Uri("ring:62fb9f4ff586168e6c137ebf64bfa2296654e17f");

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CallFragment callFragment = CallFragment.newInstance(CallFragment.ACTION_PLACE_CALL,
                accountId,
                number,
                hasVideo);
        fragmentTransaction.replace(R.id.main_call_layout, callFragment).commit();
    }
}