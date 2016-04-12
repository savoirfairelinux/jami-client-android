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
import android.hardware.Camera;
import android.util.Log;
import android.util.LongSparseArray;
import android.graphics.Point;

import java.util.HashMap;


public class VideoManagerCallback extends VideoCallback
{
    private static final String TAG = VideoManagerCallback.class.getSimpleName();

    private final DRingService mService;
    private final LongSparseArray<DeviceParams> native_params = new LongSparseArray<>();
    private final HashMap<String, DRingService.VideoParams> params = new HashMap<>();

    class DeviceParams {
        Point size;
        long rate;
        Camera.CameraInfo infos;

        public StringMap toMap(int orientation) {
            StringMap p = new StringMap();
            boolean rotated = (size.x >  size.y) == (orientation == Configuration.ORIENTATION_PORTRAIT);
            p.set("size", Integer.toString(rotated ? size.y : size.x) + "x" + Integer.toString(rotated ? size.x : size.y));
            p.set("rate", Long.toString(rate));
            return p;
        }
    }

    public int cameraFront = 0;
    public int cameraBack = 0;

    public VideoManagerCallback(DRingService s) {
        mService = s;
    }

    public void init() {
        int number_cameras = getNumberOfCameras();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for(int i = 0; i < number_cameras; i++) {
            RingserviceJNI.addVideoDevice(Integer.toString(i));
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                cameraFront = i;
            else
                cameraBack = i;
            Log.d(TAG, "Camera number " + i);
        }
        RingserviceJNI.setDefaultDevice(Integer.toString(cameraFront));
    }

    DeviceParams getNativeParams(int i) {
        return native_params.get(i);
    }

    @Override
    public void decodingStarted(String id, String shm_path, int w, int h, boolean is_mixer) {
        mService.decodingStarted(id, shm_path, w, h, is_mixer);
    }

    @Override
    public void decodingStopped(String id, String shm_path, boolean is_mixer) {
        mService.decodingStopped(id);
    }

    public void setParameters(String camid, int format, int width, int height, int rate) {
        int id = Integer.valueOf(camid);
        DeviceParams p = native_params.get(id);
        DRingService.VideoParams new_params = new DRingService.VideoParams(id, format, p.size.x, p.size.y, rate);
        new_params.rot_width = width;
        new_params.rot_height = height;
        mService.setVideoRotation(new_params, p.infos);
        params.put(camid, new_params);
    }

    public void startCapture(String camid) {
        DRingService.VideoParams p = params.get(camid);
        if (p == null)
            return;

        mService.startCapture(p);
    }

    public void stopCapture() {
        mService.stopCapture();
    }

    @Override
    public void getCameraInfo(String camid, IntVect formats, UintVect sizes, UintVect rates) {
        int id = Integer.valueOf(camid);

        if (id < 0 || id >= getNumberOfCameras())
            return;

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

        native_params.put(id, p);
    }

    private int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    private void getFormats(Camera.Parameters param, IntVect formats_) {
        for(int fmt : param.getSupportedPreviewFormats()) {
            formats_.add(fmt);
        }
    }

    private Point getSizeToUse(Camera.Parameters param) {
        int sw = 1280, sh = 720;
        for(Camera.Size s : param.getSupportedPreviewSizes()) {
            if (s.width < sw) {
                sw = s.width;
                sh = s.height;
            }
        }
        Log.d(TAG, "Supported size: " + sw + " x " + sh);
        return new Point(sw, sh);
    }

    private void getRates(Camera.Parameters param, UintVect rates_) {
        for(int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])/2;
            rates_.add(rate);
        }
    }
}
