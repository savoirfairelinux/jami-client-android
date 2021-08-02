/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.utils

import android.content.Context
import android.os.Vibrator
import android.media.AudioManager
import android.media.AudioAttributes
import android.util.Log

/**
 * Ringer manager
 */
class Ringer(private val context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    /**
     * Starts the ringtone and/or vibrator.
     */
    fun ring() {
        Log.d(TAG, "ring: called...")
        vibrator.cancel()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            //No ring no vibrate
            Log.d(TAG, "ring: skipping ring and vibrate because profile is Silent")
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            // Vibrate
            vibrator.vibrate(VIBRATE_PATTERN, 0, VIBRATE_ATTRIBUTES)
        }
    }

    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    fun stopRing() {
        Log.d(TAG, "stopRing: called...")
        vibrator.cancel()
    }

    companion object {
        private val TAG = Ringer::class.simpleName!!
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)
        private val VIBRATE_ATTRIBUTES = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
    }

}