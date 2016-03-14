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

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import cx.ring.utils.bluetooth.BluetoothWrapper;

public class MediaManager implements OnAudioFocusChangeListener, BluetoothWrapper.BluetoothChangeListener {

    private static final String TAG = MediaManager.class.getSimpleName();
    private final Context context;
    private final SettingsContentObserver settingsContentObserver;
    public final AudioManager audioManager;
    private final Ringer ringer;
    //Bluetooth related
    private BluetoothWrapper bluetoothWrapper;

    public MediaManager(Context c) {
        context = c;
        settingsContentObserver = new SettingsContentObserver(c, new Handler());
        audioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        //audioManager.registerMediaButtonEventReceiver();
        
        ringer = new Ringer(c);
    }

    public void startService() {
        if(bluetoothWrapper == null) {
            bluetoothWrapper = BluetoothWrapper.getInstance(context);
            bluetoothWrapper.setBluetoothChangeListener(this);
            bluetoothWrapper.register();
        }
        context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, settingsContentObserver);
    }

    public void stopService() {
        Log.i(TAG, "Remove media manager....");
        context.getContentResolver().unregisterContentObserver(settingsContentObserver);
        if(bluetoothWrapper != null) {
            bluetoothWrapper.unregister();
            bluetoothWrapper.setBluetoothChangeListener(null);
            bluetoothWrapper = null;
        }
    }

    public void obtainAudioFocus(boolean requestSpeakerOn) {
        audioManager.requestAudioFocus(this, Compatibility.getInCallStream(false), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if(bluetoothWrapper != null && bluetoothWrapper.canBluetooth()) {
            Log.d(TAG, "Try to enable bluetooth");
            bluetoothWrapper.setBluetoothOn(true);
        } else if (!audioManager.isWiredHeadsetOn()){
            audioManager.setSpeakerphoneOn(requestSpeakerOn);
        }
    }

    @Override
    public void onAudioFocusChange(int arg0) {
        Log.i(TAG, "onAudioFocusChange " + arg0);
    }

    public void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
        }
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void routeToSpeaker() {
        audioManager.setSpeakerphoneOn(true);
    }

    public void routeToInternalSpeaker() {
        audioManager.setSpeakerphoneOn(false);
    }
    
    /**5
     * Start ringing announce for a given contact.
     * It will also focus audio for us.
     * @param remoteContact the contact to ring for. May resolve the contact ringtone if any.
     */
    synchronized public void startRing(String remoteContact) {
        ringer.ring();
    }
    
    /**
     * Stop all ringing. <br/>
     * Warning, this will not unfocus audio.
     */
    synchronized public void stopRing() {
        ringer.stopRing();
    }

    @Override
    public void onBluetoothStateChanged(int status) {
        //setSoftwareVolume();
        //broadcastMediaChanged();
    }

}
