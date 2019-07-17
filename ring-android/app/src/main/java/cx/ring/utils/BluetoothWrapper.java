/*
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import java.util.List;
import java.util.Set;

public class BluetoothWrapper {

    private static String TAG = BluetoothWrapper.class.getSimpleName();
    private static final boolean DBG = false;

    protected Context mContext;
    private AudioManager audioManager;
    private boolean isBluetoothConnecting = false;
    private boolean isBluetoothConnected = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHeadset headsetAdapter = null;

    private boolean targetBt = false;
    private BluetoothChangeListener btChangesListener;

    private final BroadcastReceiver mHeadsetProfileReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int status = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                Log.d(TAG, "BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED " + status);
            }
            if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                int status = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                Log.d(TAG, "BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED " + status);
                if (DBG && headsetAdapter != null && status == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    List<BluetoothDevice> devices = headsetAdapter.getConnectedDevices();
                    Log.d(TAG, "Bluetooth Headset profile: " + devices.size() + " devices.");
                    for (BluetoothDevice device : devices) {
                        Log.d(TAG, "BluetoothDevice " + device.getName() + " " + device + " " + device.getBondState() + " connected: " + headsetAdapter.isAudioConnected(device));
                    }
                }
            } else if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT.equals(action)) {
                int cmdType = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1);
                String cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
                Log.d(TAG, "BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT " + cmdType + " " + cmd);
            }
        }
    };

    private BroadcastReceiver mediaStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
                int status = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
                Log.d(TAG, "BT SCO state changed : " + status + " target is " + targetBt);

                if (status == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d(TAG, "BT SCO state changed : CONNECTED");
                    audioManager.setBluetoothScoOn(true);
                    isBluetoothConnecting = false;
                    isBluetoothConnected = true;
                    if (btChangesListener != null) {
                        btChangesListener.onBluetoothStateChanged(BluetoothHeadset.STATE_AUDIO_CONNECTED);
                    }
                } else if (status == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "BT SCO state changed : DISCONNECTED");
                    boolean wasConnected = isBluetoothConnected;
                    audioManager.setBluetoothScoOn(false);
                    isBluetoothConnecting = false;
                    isBluetoothConnected = false;
                    if (btChangesListener != null && wasConnected) {
                        btChangesListener.onBluetoothStateChanged(BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                    }
                } else {
                    Log.d(TAG, "BT SCO state changed : " + status);
                }
            }
        }
    };

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

    public void setBluetoothChangeListener(BluetoothChangeListener listener) {
        btChangesListener = listener;
    }

    public boolean isBTHeadsetConnected() {
        return bluetoothAdapter != null && (bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED);
    }

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
                    if (bluetoothClass.hasService(BluetoothClass.Service.TELEPHONY) ||
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
        Log.d(TAG, "canBluetooth: Can I do BT ? " + retVal);
        return retVal;
    }

    public void setBluetoothOn(boolean on) {
        targetBt = on;
        if (on && isBluetoothConnecting || on && isBluetoothConnected) {
            return;
        }
        Log.d(TAG, "setBluetoothOn: " + on);
        Log.i(TAG, "mAudioManager.isBluetoothA2dpOn():" + audioManager.isBluetoothA2dpOn());
        Log.i(TAG, "mAudioManager.isBluetoothscoOn():" + audioManager.isBluetoothScoOn());

            if (on) {
                isBluetoothConnecting = true;
                audioManager.startBluetoothSco();
            } else {
                audioManager.stopBluetoothSco();
            }
    }

    public void registerScoUpdate() {
        Log.d(TAG, "registerScoUpdate: Register BT media receiver");
        mContext.registerReceiver(mediaStateReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
    }

    public void registerBtConnection() {
        Log.d(TAG, "registerBtConnection: Register BT connection");
        mContext.registerReceiver(mHeadsetProfileReceiver, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
        mContext.registerReceiver(mHeadsetProfileReceiver, new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED));

        if (bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(mContext, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    Log.d(TAG, "onServiceConnected " + profile + " " + proxy);
                    if (profile == BluetoothProfile.HEADSET) {
                        headsetAdapter = (BluetoothHeadset) proxy;
                        if (DBG) {
                            List<BluetoothDevice> devices = headsetAdapter.getConnectedDevices();
                            Log.d(TAG, "Bluetooth Headset profile connected: " + devices.size() + " devices.");
                            for (BluetoothDevice device : devices) {
                                Log.d(TAG, "BluetoothDevice " + device.getName() + " " + device + " " + device.getBondState() + " connected: " + headsetAdapter.isAudioConnected(device));
                            }
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    Log.d(TAG, "onServiceDisconnected " + profile);
                    if (profile == BluetoothProfile.HEADSET) {
                        headsetAdapter = null;
                    }
                }
            }, BluetoothProfile.HEADSET);
        }
    }

    public void unregister() {
        try {
            Log.d(TAG, "unregister: Unregister BT media receiver");
            mContext.unregisterReceiver(mediaStateReceiver);
            mContext.unregisterReceiver(mHeadsetProfileReceiver);
            if (headsetAdapter != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetAdapter);
                headsetAdapter = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister media state receiver", e);
        }
    }

    public interface BluetoothChangeListener {
        void onBluetoothStateChanged(int status);
    }
}
