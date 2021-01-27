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
package cx.ring.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import cx.ring.application.JamiApplication;
import cx.ring.services.SyncService;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JamiJobService extends JobService
{
    private static final String TAG = JamiJobService.class.getName();

    public static final long JOB_INTERVAL = 6 * DateUtils.HOUR_IN_MILLIS;
    public static final long JOB_FLEX = 30 * DateUtils.MINUTE_IN_MILLIS;
    public static final long JOB_DURATION = 10 * DateUtils.SECOND_IN_MILLIS;
    public static final int JOB_ID = 3905;

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (params.getJobId() != JOB_ID)
            return false;
        Log.w(TAG, "onStartJob() " + params);
        try {
            try {
                ContextCompat.startForegroundService(this, new Intent(SyncService.ACTION_START)
                        .setClass(this, SyncService.class));
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error starting service", e);
            }
            new Handler().postDelayed(() -> {
                Log.w(TAG, "jobFinished() " + params);
                try {
                    startService(new Intent(SyncService.ACTION_STOP).setClass(this, SyncService.class));
                } catch (IllegalStateException ignored) {
                }
                jobFinished(params, false);
            }, JOB_DURATION);
            JamiApplication.getInstance().startDaemon();
        } catch (Exception e) {
            Log.e(TAG, "onStartJob failed", e);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.w(TAG, "onStopJob() " + params);
        try {
            synchronized (this) {
                notify();
            }
            try {
                startService(new Intent(SyncService.ACTION_STOP).setClass(this, SyncService.class));
            } catch (IllegalStateException ignored) {
            }
        } catch (Exception e) {
            Log.e(TAG, "onStopJob failed", e);
        }
        return false;
    }
}
