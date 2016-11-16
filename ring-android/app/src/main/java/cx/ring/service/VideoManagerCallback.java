/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Damien Riegel <damien.riegel@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.service;

import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.HashMap;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.VideoCallback;
import cx.ring.daemon.UintVect;
import cx.ring.daemon.IntVect;
import cx.ring.services.DaemonService;

public class VideoManagerCallback extends VideoCallback {
    private static final String TAG = VideoManagerCallback.class.getSimpleName();

    @Inject
    DaemonService mDaemonService;

    private final DRingService mService;
    private final LongSparseArray<DeviceParams> mNativeParams = new LongSparseArray<>();
    private final HashMap<String, DRingService.VideoParams> mParams = new HashMap<>();

    class DeviceParams {
        Point size;
        long rate;
        Camera.CameraInfo infos;

        public StringMap toMap(int orientation) {
            StringMap map = new StringMap();
            boolean rotated = (size.x > size.y) == (orientation == Configuration.ORIENTATION_PORTRAIT);
            map.set("size", Integer.toString(rotated ? size.y : size.x) + "x" + Integer.toString(rotated ? size.x : size.y));
            map.set("rate", Long.toString(rate));
            return map;
        }
    }

    public int cameraFront = 0;
    public int cameraBack = 0;

    public VideoManagerCallback(DRingService s) {
        mService = s;
        ((RingApplication) mService.getApplication()).getRingInjectionComponent().inject(this);
    }

    public void init() {
        int number_cameras = getNumberOfCameras();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for (int i = 0; i < number_cameras; i++) {
            mDaemonService.addVideoDevice(Integer.toString(i));
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i;
            } else {
                cameraBack = i;
            }
            Log.d(TAG, "Camera number " + i);
        }
        mDaemonService.setDefaultVideoDevice(Integer.toString(cameraFront));
    }

    DeviceParams getNativeParams(int i) {
        return mNativeParams.get(i);
    }

    @Override
    public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
        mService.decodingStarted(id, shmPath, width, height, isMixer);
    }

    @Override
    public void decodingStopped(String id, String shmPath, boolean isMixer) {
        mService.decodingStopped(id);
    }

    public void setParameters(String camid, int format, int width, int height, int rate) {
        int id = Integer.valueOf(camid);
        DeviceParams p = mNativeParams.get(id);
        DRingService.VideoParams new_params = new DRingService.VideoParams(id, format, p.size.x, p.size.y, rate);
        new_params.rotWidth = width;
        new_params.rotHeight = height;
        mService.setVideoRotation(new_params, p.infos);
        mParams.put(camid, new_params);
    }

    public void startCapture(String camId) {
        DRingService.VideoParams params = mParams.get(camId);
        if (params == null) {
            return;
        }

        mService.startCapture(params);
    }

    public void stopCapture() {
        mService.stopCapture();
    }

    @Override
    public void getCameraInfo(String camid, IntVect formats, UintVect sizes, UintVect rates) {
        int id = Integer.valueOf(camid);

        if (id < 0 || id >= getNumberOfCameras()) {
            return;
        }

        Camera cam;
        try {
            cam = Camera.open(id);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return;
        }

        Camera.Parameters param = cam.getParameters();
        cam.release();

        getFormats(param, formats);

        DeviceParams p = new DeviceParams();
        p.size = getSizeToUse(param);
        sizes.add(p.size.x);
        sizes.add(p.size.y);
        sizes.add(p.size.y);
        sizes.add(p.size.x);

        getRates(param, rates);
        p.rate = rates.get(0);

        p.infos = new Camera.CameraInfo();
        Camera.getCameraInfo(id, p.infos);

        mNativeParams.put(id, p);
    }

    private int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    private void getFormats(Camera.Parameters param, IntVect formats) {
        for (int fmt : param.getSupportedPreviewFormats()) {
            formats.add(fmt);
        }
    }

    private Point getSizeToUse(Camera.Parameters param) {
        int sw = 1280, sh = 720;
        for (Camera.Size s : param.getSupportedPreviewSizes()) {
            if (s.width < sw) {
                sw = s.width;
                sh = s.height;
            }
        }
        Log.d(TAG, "Supported size: " + sw + " x " + sh);
        return new Point(sw, sh);
    }

    private void getRates(Camera.Parameters param, UintVect rates) {
        for (int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) / 2;
            rates.add(rate);
        }
    }
}
