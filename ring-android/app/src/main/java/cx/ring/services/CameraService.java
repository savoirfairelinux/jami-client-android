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
import java.util.List;
import java.util.Map;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.utils.Log;
import io.reactivex.Completable;

abstract public class CameraService {
    private static final String TAG = CameraService.class.getSimpleName();

    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    private final Map<String, DeviceParams> mNativeParams = new HashMap<>();

    static class VideoDevices {
        final List<String> cameras = new ArrayList<>();
        String currentId;
        int currentIndex;
        String cameraFront;

        public String switchInput(boolean setDefaultCamera) {
            if(setDefaultCamera && !cameras.isEmpty()) {
                currentId = cameras.get(0);
            }
            else if (!cameras.isEmpty()) {
                currentIndex = (currentIndex + 1) % cameras.size();
                currentId = cameras.get(currentIndex);
            } else {
                currentId = null;
            }
            return currentId;
        }
    }

    protected VideoDevices devices = null;
    private VideoParams previewParams = null;

    public String switchInput(boolean setDefaultCamera) {
        if (devices == null)
            return null;
        return devices.switchInput(setDefaultCamera);
    }

    public VideoParams getParams(String camId) {
        if (camId != null) {
            return mParams.get(camId);
        } else if (previewParams != null) {
            return previewParams;
        } else if (devices != null && !devices.cameras.isEmpty()) {
            devices.currentId = devices.cameras.get(0);
            return mParams.get(devices.currentId);
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

        CameraService.VideoParams params = mParams.get(camId);
        if (params == null) {
            params = new CameraService.VideoParams(camId, format, deviceParams.size.x, deviceParams.size.y, rate);
            mParams.put(camId, params);
        } else {
            params.id = camId;
            params.format = format;
            params.width = deviceParams.size.x;
            params.height = deviceParams.size.y;
            params.rate = rate;
        }
        if (deviceParams.infos != null) {
            params.rotation = getCameraDisplayRotation(deviceParams, rotation);
        }
        Ringservice.setDeviceOrientation(camId, params.rotation);
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
        return mNativeParams.size() == 1 || (devices != null && devices.currentId != null && devices.currentId.equals(devices.cameraFront));
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
            if (codec == null)
                return MediaFormat.MIMETYPE_VIDEO_AVC;
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
            map.set("size", size.x + "x" + size.y);
            map.set("rate", Long.toString(rate));
            return map;
        }
    }

    abstract Completable init();

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
