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

import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaFormat;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.utils.Log;

abstract public class CameraService {
    private static final String TAG = CameraService.class.getName();

    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    final Map<String, DeviceParams> mNativeParams = new HashMap<>();

    String cameraFront = null;
    final ArrayList<String> cameras = new ArrayList<>();

    int currentCameraIndex = 0;
    String currentCamera = null;
    private VideoParams previewParams = null;

    public String switchInput(boolean setDefaultCamera) {
        if(setDefaultCamera && cameraFront != null) {
            currentCamera= cameraFront;
        }
        else if (!cameras.isEmpty()) {
            currentCameraIndex = (currentCameraIndex + 1) % cameras.size();
            currentCamera = cameras.get(currentCameraIndex);
        } else {
            currentCamera = null;
        }
        return currentCamera;
    }

    public String getCurrentCamera() {
        return currentCamera;
    }

    public VideoParams getParams(String camId) {
        if (camId != null) {
            return mParams.get(camId);
        } else if (previewParams != null) {
            return previewParams;
        } else if (cameraFront != null) {
            currentCamera = cameraFront;
            return mParams.get(cameraFront);
        } else if (!cameras.isEmpty()) {
            currentCamera = cameras.get(0);
            return mParams.get(currentCamera);
        }
        return null;
    }

    public void setPreviewParams(VideoParams params) {
        previewParams = params;
    }

    public void setParameters(String camId, int format, int width, int height, int rate, int rotation) {
        DeviceParams deviceParams = mNativeParams.get(camId);
        if (deviceParams == null)
            return;
        CameraService.VideoParams newParams = new CameraService.VideoParams(camId, format, deviceParams.size.x, deviceParams.size.y, rate);
        if (deviceParams.infos != null) {
            newParams.rotation = getCameraDisplayRotation(deviceParams, rotation);
        }
        Ringservice.setDeviceOrientation(camId, newParams.rotation);
        mParams.put(camId, newParams);
    }

    public void setOrientation(int rotation) {
        for (String id : getCameraIds())
            setDeviceOrientation(id, rotation);
    }

    private void setDeviceOrientation(String camId, int screenRotation) {
        DeviceParams deviceParams = mNativeParams.get(camId);
        int rotation = 0;
        if (deviceParams != null && deviceParams.infos != null) {
            rotation = getCameraDisplayRotation(deviceParams, screenRotation);
        }
        CameraService.VideoParams params = mParams.get(camId);
        if (params != null) {
            params.rotation = rotation;
        }
        Ringservice.setDeviceOrientation(camId, rotation);
    }

    private static int getCameraDisplayRotation(DeviceParams device, int screenRotation) {
        return getCameraDisplayRotation(device.infos.orientation, rotationToDegrees(screenRotation), device.infos.facing);
    }

    private static int getCameraDisplayRotation(int sensorOrientation, int screenOrientation, int cameraFacing) {
        int rotation = 0;
        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (sensorOrientation + screenOrientation + 360) % 360;
        } else {
            rotation = (sensorOrientation - screenOrientation + 360) % 360;
        }
        rotation = ((180 - rotation) + 180) % 360;
        return rotation;
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

    public Map<String, StringMap> getPreviewSettings() {
        Map<String, StringMap> camSettings = new HashMap<>();
        for (String id : getCameraIds()) {
            CameraService.DeviceParams params = getNativeParams(id);
            if (params != null) {
                camSettings.put(id, params.toMap());
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
        public int rate;
        public int rotation;
        public String codec;

        public VideoParams(String id, int format, int width, int height, int rate) {
            this.id = id;
            this.format = format;
            this.width = width;
            this.height = height;
            this.rate = rate;
        }

        public String getCodec() {
            switch (codec) {
                case "H264": return MediaFormat.MIMETYPE_VIDEO_AVC;
                case "H265": return MediaFormat.MIMETYPE_VIDEO_HEVC;
                case "VP8": return MediaFormat.MIMETYPE_VIDEO_VP8;
                case "VP9": return MediaFormat.MIMETYPE_VIDEO_VP9;
                case "MP4V-ES": return MediaFormat.MIMETYPE_VIDEO_MPEG4;
            }
            return codec;
        }
    }

    static class DeviceParams {
        Point size;
        long rate;
        Camera.CameraInfo infos;

        StringMap toMap() {
            StringMap map = new StringMap();
            map.set("size", Integer.toString(size.x) + "x" + Integer.toString(size.y));
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
