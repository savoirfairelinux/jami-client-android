/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.os.Build;

import androidx.annotation.RequiresApi;
import cx.ring.application.RingApplication;
import cx.ring.utils.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RingJobService extends JobService
{
    private static final String TAG = RingJobService.class.getName();

    private static final int SECOND = 1000;
    private static final int MINUTE = SECOND * 60;

    public static final int JOB_INTERVAL = 20 * MINUTE;
    public static final int JOB_FLEX = 10 * MINUTE;
    public static final int JOB_DURATION = 30 * SECOND;
    public static final int JOB_ID = 3905;

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (params.getJobId() != JOB_ID)
            return false;
        Log.w(TAG, "onStartJob() " + params);
        try {
            RingApplication.getInstance().startDaemon();
            new Thread(() -> {
                synchronized (this) {
                    try {
                        wait(JOB_DURATION);
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
            synchronized (this) {
                notify();
            }
        } catch (Exception e) {
            Log.e(TAG, "onStopJob failed", e);
        }
        return false;
    }
}
