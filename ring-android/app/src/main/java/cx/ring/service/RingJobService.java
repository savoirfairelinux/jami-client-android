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
package cx.ring.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

import javax.inject.Inject;

import androidx.annotation.RequiresApi;
import cx.ring.application.RingApplication;
import cx.ring.services.HardwareService;
import cx.ring.utils.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RingJobService extends JobService
{
    private static final String TAG = RingJobService.class.getName();
    public static final int JOB_ID = 3905;
    public static final int JOB_INTERVAL_MILLIS = 1000 * 60 * 20;
    public static final int JOB_FLEX_MILLIS = 1000 * 60 * 10;
    public static final int JOB_DURATION_MILLIS = 1000 * 20;

    @Inject
    protected HardwareService mHardwareService;

    private final Object syncObject = new Object();

    @Override
    public void onCreate() {
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.w(TAG, "onStartJob() " + params);
        try {
            startService(new Intent(this, DRingService.class));
            mHardwareService.connectivityChanged();
            new Thread(() -> {
                synchronized (syncObject) {
                    try {
                        syncObject.wait(JOB_DURATION_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.w(TAG, "jobFinished() " + params);
                jobFinished(params, false);
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "onStartJob failed", e);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.w(TAG, "onStopJob() " + params);
        try {
            synchronized (syncObject) {
                syncObject.notify();
            }
        } catch (Exception e) {
            Log.e(TAG, "onStopJob failed", e);
        }
        return false;
    }
}
