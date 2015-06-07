/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.utils;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;


/**
 * Ringer manager for the Phone app.
 */
public class Ringer {
    private static final String THIS_FILE = "Ringer";
   
    private static final int VIBRATE_LENGTH = 1000; // ms
    private static final int PAUSE_LENGTH = 1000; // ms

    // Uri for the ringtone.
    Uri customRingtoneUri;

    Vibrator vibrator;
    VibratorThread vibratorThread;
    Context context;

    public Ringer(Context aContext) {
        context = aContext;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Starts the ringtone and/or vibrator. 
     * 
     */
    public void ring(String remoteContact, String defaultRingtone) {
        Log.d(THIS_FILE, "==> ring() called...");

        synchronized (this) {

            AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            
            //Save ringtone at the begining in case we raise vol
//            ringtone = getRingtone(remoteContact, defaultRingtone);
            
            //No ring no vibrate
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d(THIS_FILE, "skipping ring and vibrate because profile is Silent");
                return;
            }
            
            // Vibrate
            int vibrateSetting = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
            Log.d(THIS_FILE, "v=" + vibrateSetting + " rm=" + ringerMode);
            if (vibratorThread == null &&
                    (vibrateSetting == AudioManager.VIBRATE_SETTING_ON || 
                            ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
                vibratorThread = new VibratorThread();
                Log.d(THIS_FILE, "Starting vibrator...");
                vibratorThread.start();
            }

            // Vibrate only
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
                    audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0 ) {
                Log.d(THIS_FILE, "skipping ring because profile is Vibrate OR because volume is zero");
                return;
            }

        }
    }

    /**
     * @return true if we're playing a ringtone and/or vibrating
     *     to indicate that there's an incoming call.
     *     ("Ringing" here is used in the general sense.  If you literally
     *     need to know if we're playing a ringtone or vibrating, use
     *     isRingtonePlaying() or isVibrating() instead.)
     */
    public boolean isRinging() {
        return (vibratorThread != null);
    }
    
    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    public void stopRing() {
        synchronized (this) {
            Log.d(THIS_FILE, "==> stopRing() called...");

            stopVibrator();
        }
    }
    
        
    private void stopVibrator() {

        if (vibratorThread != null) {
            vibratorThread.interrupt();
            try {
                vibratorThread.join(250); // Should be plenty long (typ.)
            } catch (InterruptedException e) {
            } // Best efforts (typ.)
            vibratorThread = null;
        }
    }

    public void updateRingerMode() {

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        synchronized (this) {
            int ringerMode = audioManager.getRingerMode();
            // Silent : stop everything
            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                stopRing();
                return;
            }

            // Vibrate
            int vibrateSetting = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
            // If not already started restart it
            if (vibratorThread == null && (vibrateSetting == AudioManager.VIBRATE_SETTING_ON || ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
                vibratorThread = new VibratorThread();
                vibratorThread.start();
            }

            // Vibrate only
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                return;
            }
            
        }
    }

    private class VibratorThread extends Thread {
        public void run() {
            try {
                while (true) {
                    vibrator.vibrate(VIBRATE_LENGTH);
                    Thread.sleep(VIBRATE_LENGTH + PAUSE_LENGTH);
                }
            } catch (InterruptedException ex) {
                Log.d(THIS_FILE, "Vibrator thread interrupt");
            } finally {
                vibrator.cancel();
            }
            Log.d(THIS_FILE, "Vibrator thread exiting");
        }
    }

}
