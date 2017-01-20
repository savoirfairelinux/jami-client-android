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
import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.model.DaemonEvent;
import cx.ring.services.HardwareService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;


public class VideoManagerCallback implements Observer<DaemonEvent> {
    private static final String TAG = VideoManagerCallback.class.getSimpleName();

    @Inject
    HardwareService mHardwareService;

    private final RingApplication mRingApplication;
    private final LongSparseArray<DeviceParams> mNativeParams = new LongSparseArray<>();
    private final HashMap<String, RingApplication.VideoParams> mParams = new HashMap<>();

    @Override
    public void update(Observable o, DaemonEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case DECODING_STARTED:
                decodingStarted(
                        event.getEventInput(DaemonEvent.EventInput.ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.PATHS, String.class),
                        event.getEventInput(DaemonEvent.EventInput.WIDTH, Integer.class),
                        event.getEventInput(DaemonEvent.EventInput.HEIGHT, Integer.class),
                        event.getEventInput(DaemonEvent.EventInput.IS_MIXER, Boolean.class)
                );
                break;
            case DECODING_STOPPED:
                decodingStopped(
                        event.getEventInput(DaemonEvent.EventInput.ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.PATHS, String.class),
                        event.getEventInput(DaemonEvent.EventInput.IS_MIXER, Boolean.class)
                );
                break;
            case GET_CAMERA_INFO:
                getCameraInfo(
                        event.getEventInput(DaemonEvent.EventInput.CAMERA_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.FORMATS, IntVect.class),
                        event.getEventInput(DaemonEvent.EventInput.SIZES, UintVect.class),
                        event.getEventInput(DaemonEvent.EventInput.RATES, UintVect.class)
                );
                break;
            case SET_PARAMETERS:
                setParameters(
                        event.getEventInput(DaemonEvent.EventInput.CAMERA_ID, String.class),
                        event.getEventInput(DaemonEvent.EventInput.FORMATS, Integer.class),
                        event.getEventInput(DaemonEvent.EventInput.WIDTH, Integer.class),
                        event.getEventInput(DaemonEvent.EventInput.HEIGHT, Integer.class),
                        event.getEventInput(DaemonEvent.EventInput.RATES, Integer.class)
                );
                break;
            case START_CAPTURE:
                startCapture(
                        event.getEventInput(DaemonEvent.EventInput.CAMERA_ID, String.class)
                );
                break;
            case STOP_CAPTURE:
                stopCapture();
                break;
            default:
                Log.i(TAG, "Unknown daemon event");
                break;
        }
    }

    static public class DeviceParams {
        Camera.Size size;
        long rate;
        Camera.CameraInfo infos;

        public StringMap toMap(int orientation) {
            StringMap map = new StringMap();
            boolean rotated = (size.width > size.height) == (orientation == Configuration.ORIENTATION_PORTRAIT);
            map.set("size", Integer.toString(rotated ? size.height : size.width) + "x" + Integer.toString(rotated ? size.width : size.height));
            map.set("rate", Long.toString(rate));
            return map;
        }
    }

    public int cameraFront = 0;
    public int cameraBack = 0;

    public VideoManagerCallback(RingApplication app) {
        mRingApplication = app;
        mRingApplication.getRingInjectionComponent().inject(this);
    }

    public void init() {
        mNativeParams.clear();
        int number_cameras = getNumberOfCameras();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for (int i = 0; i < number_cameras; i++) {
            mHardwareService.addVideoDevice(Integer.toString(i));
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i;
            } else {
                cameraBack = i;
            }
            Log.d(TAG, "Camera number " + i);
        }
        mHardwareService.setDefaultVideoDevice(Integer.toString(cameraFront));
    }

    DeviceParams getNativeParams(int i) {
        return mNativeParams.get(i);
    }

    private void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
        mRingApplication.decodingStarted(id, shmPath, width, height, isMixer);
    }

    private void decodingStopped(String id, String shmPath, boolean isMixer) {
        mRingApplication.decodingStopped(id);
    }

    private void setParameters(String camId, int format, int width, int height, int rate) {
        int id = Integer.valueOf(camId);
        DeviceParams p = mNativeParams.get(id);
        RingApplication.VideoParams newParams = new RingApplication.VideoParams(id, format, p.size.width, p.size.height, rate);
        newParams.rotWidth = width;
        newParams.rotHeight = height;
        mRingApplication.setVideoRotation(newParams, p.infos);
        mParams.put(camId, newParams);
    }

    private void startCapture(String camId) {
        RingApplication.VideoParams params = mParams.get(camId);
        if (params == null) {
            return;
        }

        mRingApplication.startCapture(params);
    }

    private void stopCapture() {
        mRingApplication.stopCapture();
    }

    private void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates) {

        int id = Integer.valueOf(camId);

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
        sizes.add(p.size.width);
        sizes.add(p.size.height);
        sizes.add(p.size.height);
        sizes.add(p.size.width);

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

    private Camera.Size getSizeToUse(Camera.Parameters param) {
        final int MIN_W = 320;
        Camera.Size sz = null;
        /** {@link Camera.Parameters#getSupportedPreviewSizes} :
         * "This method will always return a list with at least one element." */
        for (Camera.Size s : param.getSupportedPreviewSizes()) {
            if (sz == null || (s.width < sz.width && s.width >= MIN_W)) {
                sz = s;
            }
        }
        Log.d(TAG, "Supported size: " + sz.width + " x " + sz.height);
        return sz;
    }

    private void getRates(Camera.Parameters param, UintVect rates) {
        for (int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) / 2;
            rates.add(rate);
        }
    }
}
