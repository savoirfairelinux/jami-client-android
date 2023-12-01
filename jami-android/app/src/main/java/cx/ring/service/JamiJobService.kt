/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Handler
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.ContextCompat
import cx.ring.application.JamiApplication

class JamiJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        if (params.jobId != JOB_ID) return false
        Log.w(TAG, "onStartJob() $params")
        try {
            try {
                ContextCompat.startForegroundService(this, Intent(SyncService.ACTION_START)
                    .putExtra(SyncService.EXTRA_TIMEOUT, JOB_DURATION)
                    .setClass(this, SyncService::class.java))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error starting service", e)
            }
            JamiApplication.instance?.startDaemon(this)
            Handler().postDelayed({
                Log.w(TAG, "jobFinished() $params")
                jobFinished(params, false)
            }, JOB_DURATION + 500)
        } catch (e: Exception) {
            Log.e(TAG, "onStartJob failed", e)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.w(TAG, "onStopJob() $params")
        try {
            startService(Intent(SyncService.ACTION_STOP)
                .setClass(this, SyncService::class.java))
        } catch (ignored: IllegalStateException) {
        } catch (e: Exception) {
            Log.e(TAG, "onStopJob failed", e)
        }
        return false
    }

    companion object {
        private val TAG = JamiJobService::class.java.name
        const val JOB_INTERVAL = 12 * DateUtils.HOUR_IN_MILLIS
        const val JOB_FLEX = 60 * DateUtils.MINUTE_IN_MILLIS
        const val JOB_DURATION = 7 * DateUtils.SECOND_IN_MILLIS
        const val JOB_ID = 3905
    }
}