/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.PreferencesService
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var mPreferencesService: PreferencesService

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_REBOOT == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                if (mPreferencesService.settings.runOnStartup) {
                    try {
                        ContextCompat.startForegroundService(context, Intent(SyncService.ACTION_START)
                                .setClass(context, SyncService::class.java)
                                .putExtra(SyncService.EXTRA_TIMEOUT, 7 * DateUtils.SECOND_IN_MILLIS))
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error starting service", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can't start on boot", e)
            }
        }
    }

    companion object {
        private val TAG = BootReceiver::class.simpleName!!
    }
}