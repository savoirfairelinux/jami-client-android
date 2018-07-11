/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import cx.ring.application.RingApplicationFirebase;
import cx.ring.service.DRingService;

public class RingFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = RingFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
            Map<String, String> data = remoteMessage.getData();
            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
            Intent serviceIntent = new Intent(DRingService.ACTION_PUSH_RECEIVED)
                    .setClass(this, DRingService.class)
                    .putExtra(DRingService.PUSH_RECEIVED_FIELD_FROM, remoteMessage.getFrom())
                    .putExtra(DRingService.PUSH_RECEIVED_FIELD_DATA, bundle);
            WakefulBroadcastReceiver.startWakefulService(this, serviceIntent);
        } catch (Exception e) {
            Log.w(TAG, "Error handling push notification", e);
        }
    }

    @Override
    public void onNewToken(String refreshedToken) {
        try {
            Log.d(TAG, "onTokenRefresh: refreshed token: " + refreshedToken);
            RingApplicationFirebase.setPushToken(refreshedToken);
            startService(new Intent(DRingService.ACTION_PUSH_TOKEN_CHANGED)
                    .setClass(this, DRingService.class)
                    .putExtra(DRingService.PUSH_TOKEN_FIELD_TOKEN, refreshedToken));
        } catch (Exception e) {
            Log.w(TAG, "Error handling token refresh", e);
        }
    }
}
