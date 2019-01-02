/*
 *  Copyright (C) 2018-2019 Savoir-faire Linux Inc.
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
package cx.ring.services;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.HashMap;
import java.util.Map;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.utils.Log;

abstract public class CameraService {
    private static final String TAG = CameraService.class.getName();

    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    final Map<String, DeviceParams> mNativeParams = new HashMap<>();

    String cameraFront = null;
    String cameraBack = null;
    String cameraExternal = null;
    String currentCamera = null;
    private VideoParams previewParams = null;

    public String switchInput() {
        String camId;
        if (cameraExternal != null && currentCamera.equals(cameraBack)) {
            camId = cameraExternal;
        } else if (currentCamera.equals(cameraFront)) {
            camId = cameraBack;
        } else {
            camId = cameraFront;
        }
        currentCamera = camId;
        return camId;
    }

    public String getCurrentCamera() {
        return currentCamera;
    }

    public VideoParams getParams(String camId) {
        VideoParams videoParams;
        if (camId != null) {
            videoParams = mParams.get(camId);
        } else if (previewParams != null) {
            videoParams = previewParams;
        } else if (mParams.size() == 2) {
            currentCamera = cameraFront;
            videoParams = mParams.get(cameraFront);
        } else {
            currentCamera = cameraBack;
            videoParams = mParams.get(cameraBack);
        }
        return videoParams;
    }

    public void setPreviewParams(VideoParams params) {
        previewParams = params;
    }

    public void setParameters(String camId, int format, int width, int height, int rate, int rotation) {
        DeviceParams deviceParams = mNativeParams.get(camId);
        CameraService.VideoParams newParams = new CameraService.VideoParams(camId, format, deviceParams.size.x, deviceParams.size.y, rate);
        newParams.rotWidth = width;
        newParams.rotHeight = height;
        if (deviceParams.infos != null) {
            if (deviceParams.infos.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                newParams.rotation = (deviceParams.infos.orientation + rotationToDegrees(rotation) + 360) % 360;
            } else {
                newParams.rotation = (deviceParams.infos.orientation - rotationToDegrees(rotation) + 360) % 360;
            }
        }
        mParams.put(camId, newParams);
    }

    public void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates, Point minVideoSize) {
        Log.d(TAG, "getCameraInfo: " + camId + " min. size: " + minVideoSize);
        DeviceParams p = new CameraService.DeviceParams();
        p.size = new Point(0, 0);
        p.infos = new Camera.CameraInfo();

        rates.clear();

        fillCameraInfo(p, camId, formats, sizes, rates, minVideoSize);
        sizes.add(p.size.x);
        sizes.add(p.size.y);
        sizes.add(p.size.y);
        sizes.add(p.size.x);

        mNativeParams.put(camId, p);
    }

    public DeviceParams getNativeParams(String camId) {
        return mNativeParams.get(camId);
    }

    public boolean isPreviewFromFrontCamera() {
        return mNativeParams.size() == 1 || currentCamera.equals(cameraFront);
    }

    public Map<String, StringMap> getPreviewSettings(int orientation) {
        Map<String, StringMap> camSettings = new HashMap<>();
        for (String id : getCameraIds()) {
            CameraService.DeviceParams params = getNativeParams(id);
            if (params != null) {
                camSettings.put(id, params.toMap(orientation));
                Log.w(TAG, "setPreviewSettings camera:" + id);
            }
        }
        return camSettings;
    }


    public boolean hasCamera() {
        return getCameraCount() > 0;
    }


    public static class VideoParams {
        public String id;
        public int format;
        // size as captured by Android
        public int width;
        public int height;
        //size, rotated, as seen by the daemon
        public int rotWidth;
        public int rotHeight;
        public int rate;
        public int rotation;

        public VideoParams(String id, int format, int width, int height, int rate) {
            this.id = id;
            this.format = format;
            this.width = width;
            this.height = height;
            this.rate = rate;
        }
    }

    static class DeviceParams {
        Point size;
        long rate;
        Camera.CameraInfo infos;

        StringMap toMap(int orientation) {
            StringMap map = new StringMap();
            boolean rotated = (size.x > size.y) == (orientation == Configuration.ORIENTATION_PORTRAIT);
            map.set("size", Integer.toString(rotated ? size.y : size.x) + "x" + Integer.toString(rotated ? size.x : size.y));
            map.set("rate", Long.toString(rate));
            return map;
        }
    }

    abstract void init();

    interface CameraListener {
        void onOpened();
        void onError();
    }

    abstract void closeCamera();

    abstract boolean isOpen();

    abstract String[] getCameraIds();
    abstract int getCameraCount();

    abstract void fillCameraInfo(DeviceParams p, String camId, IntVect formats, UintVect sizes, UintVect rates, Point minVideoSize);

    static private int rotationToDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }
}
