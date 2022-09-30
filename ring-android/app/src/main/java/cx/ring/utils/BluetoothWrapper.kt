/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.utils

import android.media.AudioManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothClass
import android.content.IntentFilter
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.Context
import android.util.Log
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.jvm.Synchronized

class BluetoothWrapper(private val mContext: Context, private val btChangesListener: BluetoothChangeListener) {
    private val audioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter: BluetoothAdapter? = try {
        BluetoothAdapter.getDefaultAdapter()
    } catch (e: RuntimeException) {
        Log.w(TAG, "Cant get default bluetooth adapter ", e)
        null
    }
    private var isBluetoothConnecting = false
    private var isBluetoothConnected = false
    private var headsetAdapter: BluetoothHeadset? = null
    private var connectedDevices: List<BluetoothDevice>? = null
    private var targetBt = false

    private val mHeadsetProfileReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            try {
                if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == action) {
                    val status = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                    Log.d(TAG, "BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED $status")
                }
                if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED == action) {
                    val status = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                    Log.d(TAG, "BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED $status")
                        synchronized(this) {
                            if (headsetAdapter != null && status == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                                val devices = headsetAdapter!!.connectedDevices
                                connectedDevices = devices
                            } else if (status == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                                connectedDevices = null
                            }
                        }

                } else if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT == action) {
                    val cmdType = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1)
                    val cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                    Log.d(TAG, "BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT $cmdType $cmd")
                }
            } catch (e: SecurityException) {
                Log.d(TAG, "Error receiving Bluetooth state", e)
            }
        }
    }
    private val mediaStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED == action) {
                val status = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
                Log.d(TAG, "BT SCO state changed : $status target is $targetBt")
                if (status == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d(TAG, "BT SCO state changed : CONNECTED")
                    audioManager.isBluetoothScoOn = true
                    isBluetoothConnecting = false
                    isBluetoothConnected = true
                    btChangesListener.onBluetoothStateChanged(BluetoothHeadset.STATE_AUDIO_CONNECTED)
                } else if (status == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "BT SCO state changed : DISCONNECTED")
                    val wasConnected = isBluetoothConnected
                    audioManager.isBluetoothScoOn = false
                    isBluetoothConnecting = false
                    isBluetoothConnected = false
                    if (wasConnected) {
                        btChangesListener.onBluetoothStateChanged(BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                    }
                } else {
                    Log.d(TAG, "BT SCO state changed : $status")
                }
            }
        }
    }
    val isBTHeadsetConnected: Boolean
        get() = try {
            bluetoothAdapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
        } catch (e: SecurityException) {
            true
        }

    fun canBluetooth(): Boolean {
        // Detect if any bluetooth a device is available for call
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled || !audioManager.isBluetoothScoAvailableOffCall) {
                return false
            }
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't get bluetooth status " + e.message)
        }
        return false
    }

    fun setBluetoothOn(on: Boolean) {
        targetBt = on
        if (on && isBluetoothConnecting || on && isBluetoothConnected) {
            return
        }
        /*Log.d(TAG, "setBluetoothOn: $on");
        Log.i(TAG, "mAudioManager.isBluetoothA2dpOn():" + audioManager.isBluetoothA2dpOn);
        Log.i(TAG, "mAudioManager.isBluetoothscoOn():" + audioManager.isBluetoothScoOn);*/
        try {
            if (on) {
                isBluetoothConnecting = true
                audioManager.startBluetoothSco()
            } else {
                audioManager.stopBluetoothSco()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error switching bluetooth " + e.message)
        }
    }

    private fun registerUpdates() {
        Log.d(TAG, "registerScoUpdate: Register BT receivers")
        mContext.registerReceiver(mediaStateReceiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
        mContext.registerReceiver(mHeadsetProfileReceiver, IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))
        mContext.registerReceiver(mHeadsetProfileReceiver, IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED))
        bluetoothAdapter?.getProfileProxy(mContext, object : ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                Log.d(TAG, "onServiceConnected $profile $proxy")
                if (profile == BluetoothProfile.HEADSET) {
                    headsetAdapter = proxy as BluetoothHeadset
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.d(TAG, "onServiceDisconnected $profile")
                if (profile == BluetoothProfile.HEADSET) {
                    headsetAdapter = null
                }
            }
        }, BluetoothProfile.HEADSET)
    }

    @Synchronized
    fun unregister() {
        try {
            Log.d(TAG, "unregister: Unregister BT media receiver")
            mContext.unregisterReceiver(mediaStateReceiver)
            mContext.unregisterReceiver(mHeadsetProfileReceiver)
            headsetAdapter?.let { adapter ->
                bluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HEADSET, adapter)
                headsetAdapter = null
            }
            connectedDevices = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister media state receiver", e)
        }
    }

    // Use first bounded device with audio connected
    // Fallback on first bounded device
    @get:Synchronized
    val deviceName: String?
        get() {
            try {
                connectedDevices?.takeIf { it.isNotEmpty() }?.let { devices ->
                    // Use first bounded device with audio connected
                    for (device in devices) {
                        if (device.bondState == BluetoothDevice.BOND_BONDED && headsetAdapter!!.isAudioConnected(device)) {
                            return device.name
                        }
                    }
                    // Fallback on first bounded device
                    for (device in devices) {
                        if (device.bondState == BluetoothDevice.BOND_BONDED) {
                            return device.name
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to get bluetooth device name", e)
            }
            return null
        }

    interface BluetoothChangeListener {
        fun onBluetoothStateChanged(status: Int)
    }

    init {
        registerUpdates()
    }

    companion object {
        private val TAG = BluetoothWrapper::class.simpleName!!
        private const val DBG = false
    }
}