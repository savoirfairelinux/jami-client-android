/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.text.format.DateUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import javax.inject.Inject;

import cx.ring.application.JamiApplication;
import dagger.hilt.android.AndroidEntryPoint;

import net.jami.services.PreferencesService;

@AndroidEntryPoint
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Inject
    PreferencesService mPreferencesService;

    public BootReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_REBOOT.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
        {
            try {
                //((JamiApplication) context.getApplicationContext()).getInjectionComponent().inject(this);
                if (mPreferencesService.getSettings().isAllowOnStartup()) {
                    try {
                        ContextCompat.startForegroundService(context, new Intent(SyncService.ACTION_START)
                                .setClass(context, SyncService.class)
                                .putExtra(SyncService.EXTRA_TIMEOUT, 5 * DateUtils.SECOND_IN_MILLIS));
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Error starting service", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't start on boot", e);
            }
        }
    }
}
