/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.application;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class JamiApplicationFirebase extends JamiApplication {
    static private final String TAG = JamiApplicationFirebase.class.getSimpleName();
    private String pushToken = null;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp.initializeApp(this);
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                Log.w(TAG, "Found push token");
                try {
                    setPushToken(token);
                } catch (Exception e) {
                    Log.e(TAG, "Can't set push token", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Can't start service", e);
        }
    }

    @Override
    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String token) {
        // Log.d(TAG, "setPushToken: " + token);
        pushToken = token;
        if (mAccountService != null && mPreferencesService != null) {
            if (mPreferencesService.getSettings().isAllowPushNotifications()) {
                mAccountService.setPushNotificationToken(token);
            }
        }
    }

    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
        if (mAccountService != null)
            mAccountService.pushNotificationReceived(remoteMessage.getFrom(), remoteMessage.getData());
    }
}
