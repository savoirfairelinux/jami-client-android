/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

/**
 * Ringer manager
 */
public class Ringer {
    private static final String TAG = Ringer.class.getSimpleName();
    private static final long[] VIBRATE_PATTERN = {0, 1000, 1000};

    @SuppressLint("NewApi")
    private static final AudioAttributes VIBRATE_ATTRIBUTES = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ?
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build() : null;

    private final Context context;
    private final Vibrator vibrator;

    public Ringer(Context aContext) {
        context = aContext;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Starts the ringtone and/or vibrator.
     */
    public void ring() {
        Log.d(TAG, "==> ring() called...");

        AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        vibrator.cancel();

        int ringerMode = audioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            //No ring no vibrate
            Log.d(TAG, "skipping ring and vibrate because profile is Silent");
        }
        else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            // Vibrate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                vibrator.vibrate(VIBRATE_PATTERN, 0, VIBRATE_ATTRIBUTES);
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
            audioManager.setMode(AudioManager.MODE_RINGTONE);
        }
    }

    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    public void stopRing() {
        Log.d(TAG, "==> stopRing() called...");
        vibrator.cancel();
    }

}
