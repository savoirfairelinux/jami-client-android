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
package cx.ring.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import cx.ring.R
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

object DeviceUtils {
    fun isTv(context: Context): Boolean =
        context.getSystemService(UiModeManager::class.java)
            .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources
            .getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun isTablet(context: Context): Boolean =
        context.resources.getBoolean(R.bool.isTablet)

    private val uiThread = Looper.getMainLooper().thread
    val uiHandler = Handler(Looper.getMainLooper())
    val uiScheduler: Scheduler = Schedulers.from {
        if (Thread.currentThread() === uiThread) {
            it.run()
        } else {
            uiHandler.post(it)
        }
    }

}
