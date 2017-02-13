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
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.daemon.StringMap;
import cx.ring.utils.Log;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    Context mContext;

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
    public File provideFilesDir() {
        return mContext.getFilesDir();
    }

    @Override
    public String provideDefaultVCardName() {
        return mContext.getString(R.string.unknown);
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
    public Map<String, StringMap> retrieveAvailablePreviewSettings() {
        RingApplication application = (RingApplication) mContext.getApplicationContext();
        Map<String, StringMap> camSettings = new HashMap<>();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            if (application.mVideoManagerCallback.getNativeParams(i) != null) {
                camSettings.put(Integer.toString(i), application.mVideoManagerCallback.getNativeParams(i).toMap(mContext.getResources().getConfiguration().orientation));
            }
        }
        return camSettings;
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
