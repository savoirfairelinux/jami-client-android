/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

package cx.ring.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class SystemServiceUtils {

    private static final String TAG = SystemServiceUtils.class.getSimpleName();

    /**
     * Some device don't implement a "Wake on notification" feature,
     * this method is a hack to wake them even in this case
     */
    public static void wakeUpDevice(Context context) {
        if (context == null) {
            Log.e(TAG, "wakeUpDevice: invalid context");
            return;
        }

        if (deviceWillWake()) {
            Log.d(TAG, "wakeUpDevice: this hack is not required for this device");
            return;
        }

        Log.d(TAG, "wakeUpDevice: waking up device");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                            PowerManager.SCREEN_DIM_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            wakeLock.acquire(5000);
        }
    }

    private static boolean deviceWillWake() {
        return !Build.MANUFACTURER.equals("samsung");
    }
}
