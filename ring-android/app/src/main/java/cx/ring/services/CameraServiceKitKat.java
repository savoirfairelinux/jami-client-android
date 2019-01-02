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
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.UintVect;
import cx.ring.utils.Log;

class CameraServiceKitKat extends CameraService {
    private final ScheduledExecutorService mVideoExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "VideoThread"));
    private static final String TAG = CameraServiceKitKat.class.getName();

    private Camera previewCamera;

    @Override
    void init() {
        mNativeParams.clear();
        int numberCameras = Camera.getNumberOfCameras();
        if (numberCameras > 0) {
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberCameras; i++) {
                RingserviceJNI.addVideoDevice(Integer.toString(i));
                Camera.getCameraInfo(i, camInfo);
                if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraFront = Integer.toString(i);
                } else {
                    cameraBack = Integer.toString(i);
                }
            }
            currentCamera = cameraFront;
            RingserviceJNI.setDefaultDevice(cameraFront);
        } else {
            Log.w(TAG, "initVideo: No camera available");
            currentCamera = null;
            cameraFront = null;
        }
    }

    @Override
    public void openCamera(Context c, VideoParams videoParams, SurfaceHolder surface, CameraListener listener) {
        final Camera preview;
        try {
            if (previewCamera != null) {
                ((Camera) previewCamera).release();
                previewCamera = null;
            }
            int id = Integer.parseInt(videoParams.id);
            preview = Camera.open(id);
            setCameraDisplayOrientation(c, id, preview);
        } catch (Exception e) {
            Log.e(TAG, "Camera.open: " + e.getMessage());
            return;
        }

        try {
            preview.setPreviewDisplay(surface);
        } catch (IOException e) {
            Log.e(TAG, "setPreviewDisplay: " + e.getMessage());
            return;
        }

        Camera.Parameters parameters = preview.getParameters();
        parameters.setPreviewFormat(videoParams.format);
        parameters.setPreviewSize(videoParams.width, videoParams.height);
        parameters.setRotation(0);

        for (int[] fps : parameters.getSupportedPreviewFpsRange()) {
            if (videoParams.rate >= fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] &&
                    videoParams.rate <= fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                parameters.setPreviewFpsRange(fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            }
        }

        try {
            preview.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while settings preview parameters", e);
        }

        final int videoWidth = videoParams.width;
        final int heigth = videoParams.height;
        final int rotation = videoParams.rotation;

        preview.setPreviewCallbackWithBuffer((data, camera) -> {
            mVideoExecutor.execute(() -> {
                long frame = RingserviceJNI.obtainFrame(data.length);
                if (frame != 0) {
                    RingserviceJNI.setVideoFrame(data, data.length, frame, videoWidth, heigth, rotation);
                }
                RingserviceJNI.releaseFrame(frame);
            });

            preview.addCallbackBuffer(data);
        });

        // enqueue first buffer
        int bufferSize = parameters.getPreviewSize().width * parameters.getPreviewSize().height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        preview.addCallbackBuffer(new byte[bufferSize]);

        preview.setErrorCallback((error, cam) -> {
            Log.w(TAG, "Camera onError " + error);
            if (previewCamera == cam)
                previewCamera = null;
            if (preview == cam)
                listener.onError();
        });
        try {
            preview.startPreview();
        } catch (RuntimeException e) {
            Log.e(TAG, "startPreview: " + e.getMessage());
            return;
        }
        previewCamera = preview;
        listener.onOpened();
    }

    @Override
    public boolean isOpen() {
        return previewCamera != null;
    }

    @Override
    void fillCameraInfo(DeviceParams p, String camId, IntVect formats, UintVect sizes, UintVect rates, Point minVideoSize) {
        int id = Integer.valueOf(camId);
        if (id < 0 || id >= Camera.getNumberOfCameras()) {
            return;
        }
        Camera cam;
        try {
            cam = Camera.open(id);
        } catch (Exception e) {
            Log.e(TAG, "An error occurred getting camera info", e);
            return;
        }
        Camera.Parameters param = cam.getParameters();
        cam.release();
        for (int fmt : param.getSupportedPreviewFormats()) {
            formats.add(fmt);
        }

        /* {@link Camera.Parameters#getSupportedPreviewSizes} :
         * "This method will always return a list with at least one element."
         * Attempt to find the size with width closest (but above) MIN_WIDTH. */
        for (Camera.Size s : param.getSupportedPreviewSizes()) {
            if (s.width < s.height) {
                continue;
            }
            if (p.size.x < minVideoSize.x ? s.width > p.size.x : (s.width >= minVideoSize.x && s.width < p.size.x)) {
                p.size.x = s.width;
                p.size.y = s.height;
            }
        }

        for (int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) / 2;
            rates.add(rate);
        }
        p.rate = rates.get(0);
        Camera.getCameraInfo(id, p.infos);
    }

    @Override
    public void closeCamera() {
        final Camera preview = previewCamera;
        previewCamera = null;
        try {
            preview.setPreviewCallback(null);
            preview.setErrorCallback(null);
            preview.stopPreview();
            preview.release();
        } catch (Exception e) {
            Log.e(TAG, "stopCapture error" + e);
        }
    }

    private void setCameraDisplayOrientation(Context c, int camId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(camId, info);
        WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + rotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - rotation + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        Log.w(TAG, "setCameraDisplayOrientation " + Integer.toString(rotation) + " " + Integer.toString(result));
    }

    @Override
    String[] getCameraIds() {
        int numberCameras = Camera.getNumberOfCameras();
        String[] ids = new String[numberCameras];
        for (int i = 0; i < numberCameras; i++)
            ids[i] = Integer.toString(i);
        return ids;
    }

    @Override
    public int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

}
