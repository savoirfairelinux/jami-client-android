/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import cx.ring.application.JamiApplication;
import cx.ring.application.JamiApplicationFirebase;

public class JamiFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            // Even if wakeLock is deprecated, without this part, some devices are blocking
            // during the call negotiation. So, re-add this code to avoid to block here.
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push");
            wl.setReferenceCounted(false);
            wl.acquire(10 * 1000);
        } catch (Exception e) {
            Log.w("JamiFirebaseMessaging", "Can't acquire wake lock", e);
        }

        JamiApplicationFirebase app = (JamiApplicationFirebase)JamiApplication.getInstance();
        if (app != null)
            app.onMessageReceived(remoteMessage);
    }

    @Override
    public void onNewToken(@NonNull String refreshedToken) {
        JamiApplicationFirebase app = (JamiApplicationFirebase)JamiApplication.getInstance();
        if (app != null)
            app.setPushToken(refreshedToken);
    }
}
