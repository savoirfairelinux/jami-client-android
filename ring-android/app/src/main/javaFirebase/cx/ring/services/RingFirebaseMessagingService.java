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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;

public class RingFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = RingFirebaseMessagingService.class.getSimpleName();

    @Inject
    protected AccountService mAccountService;

    @Override
    public void onCreate() {
        super.onCreate();
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
        Map<String, String> data = remoteMessage.getData();

        if (BuildConfig.DEBUG) {
            for (Map.Entry<String, String> e : data.entrySet()) {
                Log.d(TAG, "entry: " + e.getKey() + " -> " + e.getValue());
            }
        }

        mAccountService.pushNotificationReceived(remoteMessage.getFrom(), data);
    }

    @Override
    public void onMessageSent(String s) {
        Log.d(TAG, "onMessageSent: " + s);
    }
}
