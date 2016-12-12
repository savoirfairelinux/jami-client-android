/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 * <p>
 * CSipSimple is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * If you own a pjsip commercial license you can also redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as an android library.
 * <p>
 * CSipSimple is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import java.util.Set;


public class BluetoothWrapper {

    private static String TAG = BluetoothWrapper.class.getSimpleName();

    protected Context mContext;
    private AudioManager audioManager;
    private boolean isBluetoothConnected = false;
    private BluetoothAdapter bluetoothAdapter;
    private boolean targetBt = false;

    interface BluetoothChangeListener {
        void onBluetoothStateChanged(int status);
    }

    private BluetoothChangeListener btChangesListener;

    public BluetoothWrapper(Context context) {
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (bluetoothAdapter == null) {
            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (RuntimeException e) {
                Log.w(TAG, "Cant get default bluetooth adapter ", e);
            }
        }
    }

    public void setBluetoothChangeListener(BluetoothChangeListener l) {
        btChangesListener = l;
    }

    public boolean isBTHeadsetConnected() {
        return bluetoothAdapter != null && (bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED);
    }

    private BroadcastReceiver mediaStateReceiver = new BroadcastReceiver() {

        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, ">>> BT SCO state changed !!! ");
            if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
                int status = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
                Log.d(TAG, "BT SCO state changed : " + status + " target is " + targetBt);
                audioManager.setBluetoothScoOn(targetBt);

                if (status == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d(TAG, "BT SCO state changed : CONNECTED");
                    isBluetoothConnected = true;
                } else if (status == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "BT SCO state changed : DISCONNECTED");
                    isBluetoothConnected = false;
                } else {
                    Log.d(TAG, "BT SCO state changed : " + status);
                }

                if (btChangesListener != null) {
                    btChangesListener.onBluetoothStateChanged(status);
                }
            }
        }
    };

    public boolean canBluetooth() {
        // Detect if any bluetooth a device is available for call
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            return false;
        }
        boolean hasConnectedDevice = false;
        //If bluetooth is on
        if (bluetoothAdapter.isEnabled()) {

            //We get all bounded bluetooth devices
            // bounded is not enough, should search for connected devices....
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                BluetoothClass bluetoothClass = device.getBluetoothClass();
                if (bluetoothClass != null) {
                    int deviceClass = bluetoothClass.getDeviceClass();
                    if (bluetoothClass.hasService(BluetoothClass.Service.RENDER) ||
                            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                        //And if any can be used as a audio handset
                        hasConnectedDevice = true;
                        break;
                    }
                }
            }
        }
        boolean retVal = hasConnectedDevice && audioManager.isBluetoothScoAvailableOffCall();
        Log.d(TAG, "Can I do BT ? " + retVal);
        return retVal;
    }

    public void setBluetoothOn(boolean on) {
        Log.d(TAG, "Try to turn BT " + on);
        cx.ring.utils.Log.i(TAG, "mAudioManager.isBluetoothA2dpOn():" + audioManager.isBluetoothA2dpOn());
        cx.ring.utils.Log.i(TAG, "mAudioManager.isBluetoothscoOn():" + audioManager.isBluetoothScoOn());

        targetBt = on;
        if (on != isBluetoothConnected) {
            // BT SCO connection state is different from required activation
            if (on) {
                // First we try to connect
                Log.d(TAG, "BT SCO on >>>");
                audioManager.startBluetoothSco();
            } else {
                Log.d(TAG, "BT SCO off >>>");
                // We stop to use BT SCO
                audioManager.setBluetoothScoOn(false);
                // And we stop BT SCO connection
                audioManager.stopBluetoothSco();
            }
        }
        else if (on != audioManager.isBluetoothScoOn()) {
            // BT SCO is already in desired connection state
            // we only have to use it
            audioManager.setBluetoothScoOn(on);
        }
    }

    public void register() {
        Log.d(TAG, "Register BT media receiver");
        mContext.registerReceiver(mediaStateReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
    }

    public void unregister() {
        try {
            Log.d(TAG, "Unregister BT media receiver");
            mContext.unregisterReceiver(mediaStateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister media state receiver", e);
        }
    }
}
