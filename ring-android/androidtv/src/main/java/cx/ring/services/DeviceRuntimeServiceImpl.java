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
import cx.ring.utils.Log;
import cx.ring.utils.MediaManager;
import cx.ring.utils.NetworkUtils;
import cx.ring.utils.StringUtils;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();
    private static final String[] PROFILE_PROJECTION = new String[]{ContactsContract.Profile._ID,
            ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
            ContactsContract.Profile.PHOTO_ID};

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    protected Context mContext;

    @Inject
    protected MediaManager mediaManager;

    private long mDaemonThreadId = -1;

    @Override
    public void loadNativeLibrary() {
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
                mediaManager.obtainAudioFocus(isRinging);
                if (isRinging) {
                    mediaManager.audioManager.setMode(AudioManager.MODE_RINGTONE);
                    mediaManager.startRing();
                } else {
                    mediaManager.stopRing();
                    mediaManager.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
            }
        });
    }

    @Override
    public void closeAudioState() {
        mediaManager.stopRing();
        mediaManager.abandonAudioFocus();
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

    @Override
    public void startRinging() {

    }

    @Override
    public boolean isSpeakerOn() {
        return false;
    }

    @Override
    public void stopRinging() {

    }

    @Override
    public void abandonAudioFocus() {

    }

    @Override
    public void obtainAudioFocus(boolean requesSpeakerOn) {

    }

    @Override
    public void switchAudioToCurrentMode() {

    }

    @Override
    public void toggleSpeakerphone() {

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
}
