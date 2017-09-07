/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringVect;
import cx.ring.service.OpenSlParams;
import cx.ring.utils.BluetoothWrapper;
import cx.ring.utils.Log;
import cx.ring.utils.NetworkUtils;
import cx.ring.utils.Ringer;
import cx.ring.utils.StringUtils;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService implements AudioManager.OnAudioFocusChangeListener, BluetoothWrapper.BluetoothChangeListener {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();
    private static final String[] PROFILE_PROJECTION = new String[]{ContactsContract.Profile._ID,
            ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
            ContactsContract.Profile.PHOTO_ID};

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    protected Context mContext;

    private long mDaemonThreadId = -1;

    private Ringer mRinger;
    private AudioManager mAudioManager;
    private BluetoothWrapper mBluetoothWrapper;

    @Override
    public void loadNativeLibrary() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRinger = new Ringer(mContext);

        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    mDaemonThreadId = Thread.currentThread().getId();
                    System.loadLibrary("ring");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Could not load Ring library", e);
                    return false;
                }
            }
        });

        try {
            boolean loaded = result.get();
            Log.i(TAG, "Ring library has been successfully loaded");
        } catch (Exception e) {
            Log.e(TAG, "Could not load Ring library", e);
        }
    }

    @Override
    public void updateAudioState(final boolean isRinging) {
        Handler mainHandler = new Handler(mContext.getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothWrapper == null) {
                    mBluetoothWrapper = new BluetoothWrapper(mContext);
                    mBluetoothWrapper.registerScoUpdate();
                    mBluetoothWrapper.registerBtConnection();
                    mBluetoothWrapper.setBluetoothChangeListener(DeviceRuntimeServiceImpl.this);
                }

                obtainAudioFocus(isRinging);
                if (isRinging) {
                    mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                    startRinging();
                } else {
                    stopRinging();
                    mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
            }
        });
    }

    @Override
    public void closeAudioState() {
        stopRinging();
        abandonAudioFocus();
    }

    @Override
    public File provideFilesDir() {
        return mContext.getFilesDir();
    }

    private boolean isNetworkConnectedForType(int connectivityManagerType) {
        NetworkInfo info = NetworkUtils.getNetworkInfo(mContext);
        return (info != null && info.isConnected() && info.getType() == connectivityManagerType);
    }

    @Override
    public boolean isConnectedBluetooth() {
        return isNetworkConnectedForType(ConnectivityManager.TYPE_BLUETOOTH);
    }

    @Override
    public boolean isConnectedWifi() {
        return isNetworkConnectedForType(ConnectivityManager.TYPE_WIFI);
    }

    @Override
    public boolean isConnectedMobile() {
        return isNetworkConnectedForType(ConnectivityManager.TYPE_MOBILE);
    }

    @Override
    public boolean isConnectedEthernet() {
        return isNetworkConnectedForType(ConnectivityManager.TYPE_ETHERNET);
    }

    @Override
    public long provideDaemonThreadId() {
        return mDaemonThreadId;
    }

    @Override
    public boolean hasVideoPermission() {
        return checkPermission(Manifest.permission.CAMERA);
    }

    @Override
    public boolean hasAudioPermission() {
        return checkPermission(Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public boolean hasContactPermission() {
        return checkPermission(Manifest.permission.READ_CONTACTS);
    }

    @Override
    public boolean hasCallLogPermission() {
        return checkPermission(Manifest.permission.WRITE_CALL_LOG);
    }

    @Override
    public boolean hasPhotoPermission() {
        return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public boolean hasGalleryPermission() {
        return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    public String getProfileName() {
        Cursor mProfileCursor = mContext.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, PROFILE_PROJECTION, null, null, null);
        if (mProfileCursor != null) {
            if (mProfileCursor.moveToFirst()) {
                String profileName = mProfileCursor.getString(mProfileCursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME_PRIMARY));
                mProfileCursor.close();
                return profileName;
            }
            mProfileCursor.close();
        }
        return null;
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void getHardwareAudioFormat(IntVect ret) {
        OpenSlParams audioParams = OpenSlParams.createInstance(mContext);
        ret.add(audioParams.getSampleRate());
        ret.add(audioParams.getBufferSize());
        Log.d(TAG, "getHardwareAudioFormat: " + audioParams.getSampleRate() + " " + audioParams.getBufferSize());
    }

    @Override
    public void getAppDataPath(String name, StringVect ret) {
        if (name == null || ret == null) {
            return;
        }

        switch (name) {
            case "files":
                ret.add(mContext.getFilesDir().getAbsolutePath());
                break;
            case "cache":
                ret.add(mContext.getCacheDir().getAbsolutePath());
                break;
            default:
                ret.add(mContext.getDir(name, Context.MODE_PRIVATE).getAbsolutePath());
                break;
        }
    }

    @Override
    public void getDeviceName(StringVect ret) {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            ret.add(StringUtils.capitalize(model));
        } else {
            ret.add(StringUtils.capitalize(manufacturer) + " " + model);
        }
    }

    @Override
    public void startRinging() {
        mRinger.ring();
    }

    @Override
    public boolean isSpeakerOn() {
        return mAudioManager.isSpeakerphoneOn();
    }

    @Override
    public void stopRinging() {
        mRinger.stopRing();
    }

    @Override
    public void onAudioFocusChange(int arg0) {
        Log.i(TAG, "onAudioFocusChange " + arg0);
    }

    @Override
    public void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(this);
        if (mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(false);
        }
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        if (mBluetoothWrapper != null) {
            mBluetoothWrapper.unregister();
            mBluetoothWrapper.setBluetoothOn(false);
            mBluetoothWrapper = null;
        }
    }

    @Override
    public void obtainAudioFocus(boolean requestSpeakerOn) {

        mAudioManager.requestAudioFocus(this, getInCallStream(mAudioManager.isBluetoothA2dpOn()), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
            Log.d(TAG, "Try to enable bluetooth");
            mBluetoothWrapper.setBluetoothOn(true);
        } else if (!mAudioManager.isWiredHeadsetOn()) {
            mAudioManager.setSpeakerphoneOn(requestSpeakerOn);
        }
    }

    @Override
    public void switchAudioToCurrentMode() {
        mRinger.stopRing();
        if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
            routeToBTHeadset();
        } else {
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }

    private void routeToBTHeadset() {
        Log.d(TAG, "Try to enable bluetooth");
        mAudioManager.setSpeakerphoneOn(false);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mBluetoothWrapper.setBluetoothOn(true);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @Override
    public void toggleSpeakerphone() {
        if (mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(!mAudioManager.isSpeakerphoneOn());
            if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
                routeToBTHeadset();
            }
        } else {
            mAudioManager.setSpeakerphoneOn(true);
        }
    }

    @Override
    public void onBluetoothStateChanged(int status) {
        Log.d(TAG, "bluetoothStateChanged to: " + status);
        if (mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            routeToBTHeadset();
        }
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
}
