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

import android.content.Context;
import android.media.AudioManager;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.utils.Compatibility;
import cx.ring.utils.Log;
import cx.ring.utils.Ringer;
import cx.ring.utils.BluetoothWrapper;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();

    @Inject
    ExecutorService mExecutor;

    @Inject
    Context mContext;

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
                    System.loadLibrary("ring");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Could not load Ring library", e);
                    return false;
                }
            }
        });

        try {
            result.get();
            Log.i(TAG, "Ring library has been successfully loaded");
        } catch (Exception e) {
            Log.e(TAG, "Could not load Ring library", e);
        }

        if (mBluetoothWrapper == null) {
            mBluetoothWrapper = new BluetoothWrapper(mContext);
            mBluetoothWrapper.register();
        }
    }

    @Override
    public File provideFilesDir() {
        return mContext.getFilesDir();
    }

    @Override
    public String provideDefaultVCardName() {
        return mContext.getString(R.string.unknown);
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
        abandonAudioFocus();
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
    }

    @Override
    public void obtainAudioFocus(boolean requestSpeakerOn) {
        mAudioManager.requestAudioFocus(this, Compatibility.getInCallStream(false), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
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
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @Override
    public void toogleSpeakerphone() {
        mAudioManager.setSpeakerphoneOn(!mAudioManager.isSpeakerphoneOn());
    }
}
