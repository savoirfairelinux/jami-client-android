/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

import net.jami.utils.Log;

import static android.content.Context.UI_MODE_SERVICE;

public class DeviceUtils {

    private static final String TAG = DeviceUtils.class.getSimpleName();

    private static final int MIN_SIZE_TABLET = 720;

    private DeviceUtils() {
    }

    public static boolean isTv(Context context) {
        if (context == null) {
            net.jami.utils.Log.e(TAG, "null context");
            return false;
        }
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        return (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
    }

    public static boolean isTablet(Context context) {
        if (context == null) {
            Log.e(TAG, "null context");
            return false;
        }
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && context.getResources().getConfiguration().screenWidthDp >= MIN_SIZE_TABLET;
    }
}
