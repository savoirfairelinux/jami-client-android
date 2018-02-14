/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.LongSparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.Log;

public class HardwareServiceImpl extends HardwareService {

    public static final int VIDEO_WIDTH = 640;
    public static final int MIN_VIDEO_WIDTH = 320;
    public static final int VIDEO_HEIGHT = 480;
    public static final int MIN_VIDEO_HEIGHT = 240;

    public static final String TAG = HardwareServiceImpl.class.getName();
    private static WeakReference<SurfaceHolder> mCameraPreviewSurface = new WeakReference<>(null);
    private static Map<String, WeakReference<SurfaceHolder>> videoSurfaces = Collections.synchronizedMap(new HashMap<String, WeakReference<SurfaceHolder>>());
    private final Map<String, Shm> videoInputs = new HashMap<>();
    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    private final LongSparseArray<DeviceParams> mNativeParams = new LongSparseArray<>();
    private Context mContext;
    private int cameraFront = 0;
    private int cameraBack = 0;
    private int currentCamera = -1;
    private VideoParams previewParams = null;
    private Camera previewCamera = null;

    public HardwareServiceImpl(Context mContext) {
        this.mContext = mContext;
    }

    public void initVideo() {
        Log.i(TAG, "initVideo()");
        mNativeParams.clear();
        int numberCameras = Camera.getNumberOfCameras();
        if (numberCameras > 0) {
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberCameras; i++) {
                addVideoDevice(Integer.toString(i));
                Camera.getCameraInfo(i, camInfo);
                if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraFront = i;
                } else {
                    cameraBack = i;
                }
            }
            currentCamera = cameraFront;
            setDefaultVideoDevice(Integer.toString(cameraFront));
        } else {
            Log.w(TAG, "initVideo: No camera available");
            currentCamera = -1;
            cameraFront = -1;
        }
    }

    public boolean isVideoAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) || Camera.getNumberOfCameras() > 0;
    }

    public boolean hasMicrophone() {
        PackageManager pm = mContext.getPackageManager();
        boolean hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);

        if (!hasMicrophone) {
            MediaRecorder recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(new File(mContext.getCacheDir(), "MediaUtil#micAvailTestFile").getAbsolutePath());
            try {
                recorder.prepare();
                recorder.start();
                hasMicrophone = true;
            } catch (IllegalStateException e) {
                // Microphone is already in use
                hasMicrophone = true;
            } catch (Exception exception) {
                hasMicrophone = false;
            } finally {
                recorder.release();
            }
        }

        return hasMicrophone;
    }

    @Override
    public boolean isSpeakerPhoneOn() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager != null && audioManager.isSpeakerphoneOn();
    }

    @Override
    public void switchSpeakerPhone() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
        }
    }

    @Override
    public void setSpeakerPhone(boolean activateSpeaker) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(activateSpeaker);
        }
    }

    public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
        Log.i(TAG, "decodingStarted() " + id + " " + width + "x" + height);
        Shm shm = new Shm();
        shm.id = id;
        shm.w = width;
        shm.h = height;
        videoInputs.put(id, shm);
        WeakReference<SurfaceHolder> weakSurfaceHolder = videoSurfaces.get(id);
        if (weakSurfaceHolder != null) {
            SurfaceHolder holder = weakSurfaceHolder.get();
            if (holder != null) {
                shm.window = startVideo(id, holder.getSurface(), width, height);

                if (shm.window == 0) {
                    Log.i(TAG, "DRingService.decodingStarted() no window !");

                    ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
                    event.addEventInput(ServiceEvent.EventInput.VIDEO_START, true);
                    setChanged();
                    notifyObservers(event);
                    return;
                }

                ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
                event.addEventInput(ServiceEvent.EventInput.VIDEO_CALL, shm.id);
                event.addEventInput(ServiceEvent.EventInput.VIDEO_STARTED, true);
                event.addEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, shm.w);
                event.addEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, shm.h);
                setChanged();
                notifyObservers(event);
            }
        }
    }

    @Override
    public void decodingStopped(String id, String shmPath, boolean isMixer) {
        Log.i(TAG, "decodingStopped() " + id);
        Shm shm = videoInputs.remove(id);
        if (shm == null) {
            return;
        }
        if (shm.window != 0) {
            try {
                stopVideo(shm.id, shm.window);
            } catch (Exception e) {
                Log.e(TAG, "decodingStopped error" + e);
            }
            shm.window = 0;
        }
    }

    @Override
    public void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates) {
        Log.d(TAG, "getCameraInfo: " + camId);
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

        DeviceParams p = new DeviceParams();

        // Use a larger resolution for Android 6.0+, 64 bits devices
        final boolean useLargerSize = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.SUPPORTED_64_BIT_ABIS.length > 0;
        int MIN_WIDTH = useLargerSize ? VIDEO_WIDTH : MIN_VIDEO_WIDTH;
        Point size = new Point(0, 0);
        /* {@link Camera.Parameters#getSupportedPreviewSizes} :
         * "This method will always return a list with at least one element."
         * Attempt to find the size with width closest (but above) MIN_WIDTH. */
        for (Camera.Size s : param.getSupportedPreviewSizes()) {
            if (s.width < s.height) {
                continue;
            }
            if (size.x < MIN_WIDTH ? s.width > size.x : (s.width >= MIN_WIDTH && s.width < size.x)) {
                size.x = s.width;
                size.y = s.height;
            }
        }

        p.size = size;

        sizes.add(p.size.x);
        sizes.add(p.size.y);
        sizes.add(p.size.y);
        sizes.add(p.size.x);

        for (int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) / 2;
            rates.add(rate);
        }
        p.rate = rates.get(0);
        Log.d(TAG, "getCameraInfo: using resolution " + p.size.x + "x" + p.size.y + " " + p.rate / 1000 + " FPS");

        p.infos = new Camera.CameraInfo();
        Camera.getCameraInfo(id, p.infos);

        mNativeParams.put(id, p);
    }

    @Override
    public void setParameters(String camId, int format, int width, int height, int rate) {
        Log.d(TAG, "setParameters: " + camId + ", " + format + ", " + width + ", " + height + ", " + rate);
        int id = Integer.valueOf(camId);
        DeviceParams deviceParams = mNativeParams.get(id);
        VideoParams newParams = new VideoParams(id, format, deviceParams.size.x, deviceParams.size.y, rate);
        newParams.rotWidth = width;
        newParams.rotHeight = height;
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
        if (deviceParams.infos.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            newParams.rotation = (deviceParams.infos.orientation + rotation + 360) % 360;
        } else {
            newParams.rotation = (deviceParams.infos.orientation - rotation + 360) % 360;
        }
        mParams.put(camId, newParams);
    }

    @Override
    public void startCapture(@Nullable String camId) {
        VideoParams videoParams;

        if (camId == null && previewParams != null) {
            videoParams = previewParams;
        } else if (camId != null) {
            videoParams = mParams.get(camId);
        } else if (mParams.size() == 2) {
            currentCamera = cameraFront;
            videoParams = mParams.get(cameraFront);
        } else {
            currentCamera = cameraBack;
            videoParams = mParams.get(cameraBack);
        }

        SurfaceHolder surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            previewParams = videoParams;

            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
            event.addEventInput(ServiceEvent.EventInput.VIDEO_START, true);
            setChanged();
            notifyObservers(event);
            return;
        }

        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        Log.d(TAG, "startCapture: startCapture " + videoParams.id + " " + videoParams.width + "x" + videoParams.height + " rot" + videoParams.rotation);

        final Camera preview;
        try {
            if (previewCamera != null) {
                previewCamera.release();
                previewCamera = null;
            }
            preview = Camera.open(videoParams.id);
            setCameraDisplayOrientation(videoParams.id, preview);
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
            setVideoFrame(data, videoWidth, heigth, rotation);
            preview.addCallbackBuffer(data);
        });

        // enqueue first buffer
        int bufferSize = parameters.getPreviewSize().width * parameters.getPreviewSize().height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        preview.addCallbackBuffer(new byte[bufferSize]);

        preview.setErrorCallback((error, cam) -> {
            Log.w(TAG, "Camera onError " + error);
            if (preview == cam) {
                stopCapture();
            }
        });
        try {
            preview.startPreview();
        } catch (RuntimeException e) {
            Log.e(TAG, "startPreview: " + e.getMessage());
            return;
        }

        previewCamera = preview;
        previewParams = videoParams;

        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_STARTED, true);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, videoParams.rotWidth);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, videoParams.rotHeight);
        setChanged();
        notifyObservers(event);
    }

    @Override
    public void stopCapture() {
        Log.d(TAG, "stopCapture: " + previewCamera);
        if (previewCamera != null) {
            final Camera preview = previewCamera;
            final VideoParams params = previewParams;
            previewCamera = null;
            try {
                preview.setPreviewCallback(null);
                preview.setErrorCallback(null);
                preview.stopPreview();
                preview.release();
            } catch (Exception e) {
                Log.e(TAG, "stopCapture error" + e);
            }

            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
            event.addEventInput(ServiceEvent.EventInput.VIDEO_STARTED, false);
            event.addEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, params.width);
            event.addEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, params.height);
            setChanged();
            notifyObservers(event);
        }
    }

    @Override
    public void addVideoSurface(String id, Object holder) {
        if (!(holder instanceof SurfaceHolder)) {
            return;
        }

        Log.w(TAG, "addVideoSurface " + id + holder.hashCode());

        Shm shm = videoInputs.get(id);
        WeakReference<SurfaceHolder> surfaceHolder = new WeakReference<>((SurfaceHolder) holder);
        videoSurfaces.put(id, surfaceHolder);
        if (shm != null && shm.window == 0) {
            shm.window = startVideo(shm.id, surfaceHolder.get().getSurface(), shm.w, shm.h);
        }

        if (shm == null || shm.window == 0) {
            Log.i(TAG, "DRingService.addVideoSurface() no window !");

            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
            event.addEventInput(ServiceEvent.EventInput.VIDEO_START, true);
            setChanged();
            notifyObservers(event);
            return;
        }

        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_CALL, shm.id);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_STARTED, true);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_WIDTH, shm.w);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_HEIGHT, shm.h);
        setChanged();
        notifyObservers(event);

    }

    @Override
    public void addPreviewVideoSurface(Object holder) {
        if (!(holder instanceof SurfaceHolder)) {
            return;
        }

        Log.w(TAG, "addPreviewVideoSurface " + holder.hashCode());

        mCameraPreviewSurface = new WeakReference<>((SurfaceHolder) holder);
    }

    @Override
    public void removeVideoSurface(String id) {
        Log.i(TAG, "removeVideoSurface " + id);
        Shm shm = videoInputs.get(id);
        if (shm == null) {
            return;
        }
        if (shm.window != 0) {
            try {
                stopVideo(shm.id, shm.window);
            } catch (Exception e) {
                Log.e(TAG, "removeVideoSurface error" + e);
            }

            shm.window = 0;
        }

        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.VIDEO_EVENT);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_CALL, shm.id);
        event.addEventInput(ServiceEvent.EventInput.VIDEO_STARTED, false);
        setChanged();
        notifyObservers(event);
    }

    @Override
    public void removePreviewVideoSurface() {
        Log.w(TAG, "removePreviewVideoSurface");
        mCameraPreviewSurface.clear();
    }

    @Override
    public void switchInput(String id) {
        Log.w(TAG, "switchInput " + id);

        final int camId;
        if (currentCamera == cameraBack) {
            camId = cameraFront;
        } else {
            camId = cameraBack;
        }
        currentCamera = camId;

        final String uri = "camera://" + camId;
        final StringMap map = mNativeParams.get(camId).toMap(mContext.getResources().getConfiguration().orientation);
        this.switchInput(id, uri, map);
    }

    @Override
    public void restartCamera(String id) {
        stopCapture();
        setPreviewSettings();
        final String uri = "camera://" + currentCamera;
        final StringMap map = mNativeParams.get(currentCamera).toMap(mContext.getResources().getConfiguration().orientation);
        this.switchInput(id, uri, map);
    }

    @Override
    public void setPreviewSettings() {
        Map<String, StringMap> camSettings = new HashMap<>();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            if (mNativeParams.get(i) != null) {
                camSettings.put(Integer.toString(i), mNativeParams.get(i).toMap(mContext.getResources().getConfiguration().orientation));
                Log.w(TAG, "setPreviewSettings camera:" + Integer.toString(i));
            }
        }
        this.setPreviewSettings(camSettings);
    }

    @Override
    public int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

    @Override
    public boolean isPreviewFromFrontCamera() {
        return Camera.getNumberOfCameras() == 1 || currentCamera == cameraFront;
    }

    private void setCameraDisplayOrientation(int camId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(camId, info);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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

    private int rotationToDegrees(int rotation) {
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

    private static class Shm {
        String id;
        int w, h;
        long window = 0;
    }

    private static class VideoParams {
        public int id;
        public int format;
        // size as captured by Android
        public int width;
        public int height;
        //size, rotated, as seen by the daemon
        public int rotWidth;
        public int rotHeight;
        public int rate;
        public int rotation;

        public VideoParams(int id, int format, int width, int height, int rate) {
            this.id = id;
            this.format = format;
            this.width = width;
            this.height = height;
            this.rate = rate;
        }
    }

    private static class DeviceParams {
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
}
