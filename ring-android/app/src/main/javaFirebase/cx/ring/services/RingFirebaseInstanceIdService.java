/*
 *  Copyright (C) 2018 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import dagger.Lazy;

public class RingFirebaseInstanceIdService extends FirebaseInstanceIdService {
    private static final String TAG = RingFirebaseInstanceIdService.class.getSimpleName();

    public RingFirebaseInstanceIdService() {
        onTokenRefresh();
    }

    @Inject
    protected AccountService mAccountService;

    @Override
    public void onCreate() {
        super.onCreate();
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
    }

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.w(TAG, "Refreshed token: " + refreshedToken);
        if (mAccountService != null)
            mAccountService.setPushNotificationToken(FirebaseInstanceId.getInstance().getToken());
    }

}
