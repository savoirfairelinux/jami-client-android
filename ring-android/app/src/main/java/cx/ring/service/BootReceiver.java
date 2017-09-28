/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.services.PreferencesService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Inject
    PreferencesService mPreferencesService;

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            ((RingApplication) context.getApplicationContext()).getRingInjectionComponent().inject(this);
            boolean isAllowRingOnStartup = mPreferencesService.loadSettings().isAllowRingOnStartup();

            if (isAllowRingOnStartup) {
                Log.w(TAG, "Starting Ring on boot");
                Intent serviceIntent = new Intent(context, DRingService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
