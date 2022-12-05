/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    fun isTablet(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.isTablet)
    }

     /*   fun getScheduler(activity: Activity): Boolean {
        return context.resources.getBoolean(R.bool.isTablet)
    }*/

    val uiScheduler: Scheduler = Schedulers.from {
        //conversationFragment.requireActivity().runOnUiThread(it)
        val looper = Looper.getMainLooper()
        if (Thread.currentThread().equals(looper.thread)) {
            it.run()
        } else {
            Handler(looper).post(it)
        }
    }



}