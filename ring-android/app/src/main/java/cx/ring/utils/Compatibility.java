/*
 *  Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;

import java.lang.reflect.Field;

@SuppressWarnings("deprecation")
public final class Compatibility {

    private Compatibility() {
    }

    public static int getApiLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    public static boolean isCompatible(int apiLevel) {
        return android.os.Build.VERSION.SDK_INT >= apiLevel;
    }

    /**
     * Get the stream id for in call track. Can differ on some devices. Current device for which it's different :
     * 
     * @return
     */
    public static int getInCallStream(boolean requestBluetooth) {
        /* Archos 5IT */
        if (android.os.Build.BRAND.equalsIgnoreCase("archos") && android.os.Build.DEVICE.equalsIgnoreCase("g7a")) {
            // Since archos has no voice call capabilities, voice call stream is
            // not implemented
            // So we have to choose the good stream tag, which is by default
            // falled back to music
            return AudioManager.STREAM_MUSIC;
        }
        if (requestBluetooth) {
            return 6; /* STREAM_BLUETOOTH_SCO -- Thx @Stefan for the contrib */
        }

        return AudioManager.STREAM_VOICE_CALL;
    }

    public static boolean isTabletScreen(Context ctxt) {
        boolean isTablet = false;
        Configuration cfg = ctxt.getResources().getConfiguration();
        int screenLayoutVal = 0;
        try {
            Field f = Configuration.class.getDeclaredField("screenLayout");
            screenLayoutVal = (Integer) f.get(cfg);
        } catch (Exception e) {
            return false;
        }
        int screenLayout = (screenLayoutVal & 0xF);
        // 0xF = SCREENLAYOUT_SIZE_MASK but avoid 1.5 incompat doing that
        if (screenLayout == 0x3 || screenLayout == 0x4) {
            // 0x3 = SCREENLAYOUT_SIZE_LARGE but avoid 1.5 incompat doing that
            // 0x4 = SCREENLAYOUT_SIZE_XLARGE but avoid 1.5 incompat doing that
            isTablet = true;
        }

        return isTablet;
    }

}