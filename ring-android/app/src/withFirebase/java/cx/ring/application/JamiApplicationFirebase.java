/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Intent;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;

import cx.ring.service.DRingService;

public class JamiApplicationFirebase extends JamiApplication {
    static private String TAG = JamiApplicationFirebase.class.getSimpleName();

    static private String pushToken = "";

    @Override
    public void onCreate() {
        try {
            FirebaseApp.initializeApp(this);
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(c -> {
                Log.w(TAG, "Found push token");
                try {
                    pushToken = c.getResult().getToken();
                    startService(new Intent(DRingService.ACTION_PUSH_TOKEN_CHANGED)
                            .setClass(this, DRingService.class)
                            .putExtra(DRingService.PUSH_TOKEN_FIELD_TOKEN, pushToken));
                } catch (Exception e) {
                    Log.e(TAG, "Can't start service", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Can't start service", e);
        }
        super.onCreate();
    }

    @Override
    public String getPushToken() {
        return pushToken;
    }

    public static void setPushToken(String token) {
        pushToken = token;
    }

}
