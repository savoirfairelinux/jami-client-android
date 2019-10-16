/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.application.JamiApplication;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringVect;
import cx.ring.service.OpenSlParams;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.NetworkUtils;
import cx.ring.utils.StringUtils;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();
    private static final String[] PROFILE_PROJECTION = new String[]{ContactsContract.Profile._ID,
            ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
            ContactsContract.Profile.PHOTO_ID};
    @Inject
    protected Context mContext;
    @Inject
    @Named("DaemonExecutor")
    ScheduledExecutorService mExecutor;
    private long mDaemonThreadId = -1;

    @Override
    public void loadNativeLibrary() {
        mExecutor.submit(() -> {
            try {
                mDaemonThreadId = Thread.currentThread().getId();
                System.loadLibrary("ring");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Could not load Ring library", e);
                return false;
            }
        });
    }


    @Override
    public File provideFilesDir() {
        return mContext.getFilesDir();
    }

    @Override
    public File getFilePath(String filename) {
        return AndroidFileUtils.getFilePath(mContext, filename);
    }

    @Override
    public File getConversationPath(String conversationId, String name) {
        return AndroidFileUtils.getConversationPath(mContext, conversationId, name);
    }

    @Override
    public File getTemporaryPath(String conversationId, String name) {
        return AndroidFileUtils.getTempPath(mContext, conversationId, name);
    }

    @Override
    public File getCacheDir() {
        return mContext.getCacheDir();
    }

    @Override
    public String getPushToken() {
        return JamiApplication.getInstance().getPushToken();
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
    public boolean hasWriteExternalStoragePermission() {
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

}
