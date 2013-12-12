package org.sflphone.utils;

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
