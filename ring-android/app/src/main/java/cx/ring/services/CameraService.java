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
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.utils.Log;
import cx.ring.views.AutoFitTextureView;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class CameraService {
    private static final String TAG = CameraService.class.getSimpleName();
    private static final int FPS_MAX = 30;
    private static final int FPS_TARGET = 15;
    private static final Set<String> addedDevices = new HashSet<>();
    private final CameraManager manager;

    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    private final Map<String, DeviceParams> mNativeParams = new HashMap<>();
    private final HandlerThread t = new HandlerThread("videoHandler");
    private Handler videoHandler = null;
    private CameraDevice previewCamera;
    private MediaProjection currentMediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec currentCodec;

    CameraService(@NonNull Context c) {
        manager = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
    }

    public Handler getVideoHandler() {
        if (t.getState() == Thread.State.NEW)
            t.start();
        if (videoHandler == null)
            videoHandler = new Handler(t.getLooper());
        return videoHandler;
    }

    static class VideoDevices {
        final List<String> cameras = new ArrayList<>();
        String currentId;
        int currentIndex;
        String cameraFront;

        String switchInput(boolean setDefaultCamera) {
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
            //params.format = format;
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
        sizes.add((long) p.size.x);
        sizes.add((long) p.size.y);
        sizes.add((long) p.size.y);
        sizes.add((long) p.size.x);

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
        //public int format;
        // size as captured by Android
        public int width;
        public int height;
        public int rate;
        public int rotation;
        public String codec;

        public VideoParams(String id, int format, int width, int height, int rate) {
            this.id = id;
            //this.format = format;
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
            map.put("size", size.x + "x" + size.y);
            map.put("rate", Long.toString(rate));
            return map;
        }
    }

    static private Observable<Pair<String, CameraCharacteristics>> filterCompatibleCamera(String[] cameraIds, CameraManager cameraManager) {
        return Observable.fromArray(cameraIds)
                .map(id -> new Pair<>(id, cameraManager.getCameraCharacteristics(id)))
                .filter(camera -> {
                    try {
                        for (int c : camera.second.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES))
                            if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                                return true;
                    } catch (Exception e) {
                        return false;
                    }
                    return false;
                });
    }

    static private Observable<String> filterCameraIdsFacing(List<Pair<String, CameraCharacteristics>> cameras, int facing) {
        return Observable.fromIterable(cameras)
                .filter(camera -> camera.second.get(CameraCharacteristics.LENS_FACING) == facing)
                .map(camera -> camera.first);
    }

    private Single<VideoDevices> loadDevices(CameraManager manager) {
        return Single.fromCallable(() -> {
            VideoDevices devices = new VideoDevices();
            List<Pair<String, CameraCharacteristics>> cameras = filterCompatibleCamera(manager.getCameraIdList(), manager).toList().blockingGet();
            Maybe<String> backCamera = filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_BACK).firstElement();
            Maybe<String> frontCamera = filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_FRONT).firstElement();
            Observable<String> externalCameras;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                externalCameras = filterCameraIdsFacing(cameras, CameraCharacteristics.LENS_FACING_EXTERNAL);
            } else  {
                externalCameras = Observable.empty();
            }
            Observable.concat(
                    frontCamera.toObservable(),
                    backCamera.toObservable(),
                    externalCameras).blockingSubscribe(devices.cameras::add);
            if (!devices.cameras.isEmpty())
                devices.currentId = devices.cameras.get(0);
            devices.cameraFront = frontCamera.blockingGet();
            Log.w(TAG, "Loading video devices: found " + devices.cameras.size());
            return devices;
        }).subscribeOn(Schedulers.io());
    }

    Completable init() {
        if (manager == null)
            return Completable.error(new IllegalStateException("Video manager not available"));
        return loadDevices(manager)
                .map(devs -> {
                    synchronized (addedDevices) {
                        VideoDevices old = devices;
                        devices = devs;
                        // Removed devices
                        if (old != null) {
                            for (String oldId : old.cameras) {
                                if (!devs.cameras.contains(oldId)) {
                                    if (addedDevices.remove(oldId))
                                        RingserviceJNI.removeVideoDevice(oldId);
                                }
                            }
                        }
                        // Added devices
                        for (String camera : devs.cameras) {
                            if (addedDevices.add(camera))
                                RingserviceJNI.addVideoDevice(camera);
                        }
                        // New default
                        if (devs.currentId != null) {
                            RingserviceJNI.setDefaultDevice(devs.currentId);
                        }
                    }
                    return devs;
                })
                .ignoreElement()
                .doOnError(e -> Log.e(TAG, "Error initializing video device", e))
                .onErrorComplete();
    }

    interface CameraListener {
        void onOpened();
        void onError();
    }

    public void closeCamera() {
        CameraDevice camera = previewCamera;
        if (camera != null) {
            previewCamera = null;
            camera.close();
            currentCodec = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void listSupportedCodecs(MediaCodecList list) {
        try {
            for (MediaCodecInfo codecInfo : list.getCodecInfos()) {
                for (String type : codecInfo.getSupportedTypes()) {
                    try {
                        MediaCodecInfo.CodecCapabilities codecCaps = codecInfo.getCapabilitiesForType(type);
                        MediaCodecInfo.EncoderCapabilities caps = codecCaps.getEncoderCapabilities();
                        if (caps == null)
                            continue;
                        MediaCodecInfo.VideoCapabilities video_caps = codecCaps.getVideoCapabilities();
                        if (video_caps == null)
                            continue;
                        Log.w(TAG, "Codec info:" + codecInfo.getName() + " type: " + type);
                        Log.w(TAG, "Encoder capabilities: complexityRange: " + caps.getComplexityRange());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Log.w(TAG, "Encoder capabilities: qualityRange: " + caps.getQualityRange());
                        }
                        Log.w(TAG, "Encoder capabilities: VBR: " + caps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR));
                        Log.w(TAG, "Encoder capabilities: CBR: " + caps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR));
                        Log.w(TAG, "Encoder capabilities: CQ: " + caps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ));
                        Log.w(TAG, "Bitrate range: " + video_caps.getBitrateRange());
                        for (int format : codecCaps.colorFormats) {
                            Log.w(TAG, "Supported color format: " + format);
                        }

                        Range<Integer> widths = video_caps.getSupportedWidths();
                        Range<Integer> heights = video_caps.getSupportedHeights();
                        Log.w(TAG, "Supported sizes: " + widths.getLower() + "x" + heights.getLower() + " -> " + widths.getUpper() + "x" + heights.getUpper());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Log.w(TAG, "AchievableFrameRates: " + video_caps.getAchievableFrameRatesFor(1920, 1080));
                        }
                        Log.w(TAG, "SupportedFrameRates: " + video_caps.getSupportedFrameRatesFor(/*widths.getUpper(), heights.getUpper()*/1920, 1080));

                        for (MediaCodecInfo.CodecProfileLevel profileLevel : codecCaps.profileLevels)
                            Log.w(TAG, "profileLevels: " + profileLevel);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Log.w(TAG, "FEATURE_IntraRefresh: " + codecCaps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_IntraRefresh));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Can't query codec info", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Can't query codec info", e);
        }
    }

    public static Pair<MediaCodec, Surface> openEncoder(VideoParams videoParams, String mimeType, Handler handler, int resolution, int bitrate) {
        Log.d(TAG, "Video with codec " + mimeType + " resolution: " + videoParams.width + "x" + videoParams.height + " Bitrate: " + bitrate);
        int bitrateValue;
        if(bitrate == 0)
            bitrateValue = resolution >= 720 ? 192 * 8 * 1024 : 100 * 8 * 1024;
        else
            bitrateValue = bitrate * 8 * 1024;

        int frameRate = 30; // 30 fps

        MediaFormat format = MediaFormat.createVideoFormat(mimeType, videoParams.width, videoParams.height);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrateValue);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1);
            format.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, 5);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        }*/
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        //format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.CodecCapabilities.BITRATE_MODE_VBR);

        MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        //listSupportedCodecs(codecs);
        String codecName = codecs.findEncoderForFormat(format);

        format.setInteger(MediaFormat.KEY_FRAME_RATE, 24);

        Surface encoderInput = null;
        MediaCodec codec = null;
        if (codecName != null) {
            try {
                codec = MediaCodec.createByCodecName(codecName);
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrateValue);
                codec.setParameters(params);
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoderInput = codec.createInputSurface();
                MediaCodec.Callback callback = new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {}

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                        try {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0) {
                                ByteBuffer buffer = codec.getOutputBuffer(index);
                                RingserviceJNI.captureVideoPacket(buffer, info.size, info.offset, (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0, info.presentationTimeUs, videoParams.rotation);
                                codec.releaseOutputBuffer(index, false);
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "MediaCodec can't process buffer", e);
                        }
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        Log.e(TAG, "MediaCodec onError", e);
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        Log.e(TAG, "MediaCodec onOutputFormatChanged " + format);
                    }
                };

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setCodecCallback(codec, callback, handler);
                } else {
                    setCodecCallback(codec, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't open codec", e);
                if (codec != null) {
                    codec.release();
                    codec = null;
                }
                if (encoderInput != null) {
                    encoderInput.release();
                    encoderInput = null;
                }
            }
        }
        return Pair.create(codec, encoderInput);
    }

    private static void setCodecCallback(@NonNull MediaCodec codec, MediaCodec.Callback callback) {
        codec.setCallback(callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void setCodecCallback(@NonNull MediaCodec codec, MediaCodec.Callback callback, Handler handler) {
        codec.setCallback(callback, handler);
    }

    public void requestKeyFrame() {
        Log.w(TAG, "requestKeyFrame()");
        try {
            if (currentCodec != null) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                currentCodec.setParameters(params);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Can't send keyframe request", e);
        }
    }

    public void setBitrate(int bitrate) {
        Log.w(TAG, "setBitrate() " + bitrate);
        try {
            if (currentCodec != null) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate * 1024);
                currentCodec.setParameters(params);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Can't set bitrate", e);
        }
    }

    private static @NonNull Size chooseOptimalSize(@Nullable Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size target) {
        if (choices == null)
            return target;
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = target.getWidth();
        int h = target.getHeight();
        for (Size option : choices) {
            Log.w(TAG, "supportedSize: " + option);
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            android.util.Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private static @NonNull Range<Integer> chooseOptimalFpsRange(Range<Integer>[] ranges) {
        Range<Integer> range = null;
        if (ranges != null && ranges.length > 0) {
            for (Range<Integer> r : ranges) {
                if (r.getUpper() > FPS_MAX)
                    continue;
                if (range != null) {
                    int d = Math.abs(r.getUpper() - FPS_TARGET) - Math.abs(range.getUpper() - FPS_TARGET);
                    if (d > 0)
                        continue;
                    if (d == 0 && r.getLower() > range.getLower())
                        continue;
                }
                range = r;
            }
            if (range == null)
                range = ranges[0];
        }
        return range == null ? new Range<>(FPS_TARGET, FPS_TARGET) : range;
    }

    private Pair<MediaCodec, VirtualDisplay> createVirtualDisplay(MediaProjection projection, DisplayMetrics metrics) {
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        Handler handler = getVideoHandler();

        Pair<MediaCodec, Surface> r = null;
        while (screenWidth >= 320) {
            CameraService.VideoParams params = new CameraService.VideoParams(null, 0, screenWidth, screenHeight, 24);
            r = CameraService.openEncoder(params, MediaFormat.MIMETYPE_VIDEO_AVC, handler, 720, 0);
            if (r.first == null) {
                screenWidth /= 2;
                screenHeight /= 2;
            } else
                break;
        }

        final Surface surface = r.second;
        final MediaCodec codec = r.first;
        if (codec != null)
            codec.start();
        try {
            return Pair.create(codec, projection.createVirtualDisplay("ScreenSharingDemo",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface
                    , new VirtualDisplay.Callback() {
                        @Override
                        public void onPaused() {
                            Log.w(TAG, "VirtualDisplay.onPaused");
                        }

                        @Override
                        public void onResumed() {
                            Log.w(TAG, "VirtualDisplay.onResumed");
                        }

                        @Override
                        public void onStopped() {
                            Log.w(TAG, "VirtualDisplay.onStopped");
                            if (surface != null) {
                                surface.release();
                                if (codec != null)
                                    codec.release();
                                if (currentCodec == codec)
                                    currentCodec = null;
                            }
                        }
                    }, handler));
        } catch (Exception e) {
            if (codec != null) {
                codec.stop();
                codec.release();
            }
            if (surface != null) {
                surface.release();
            }
            return null;
        }
    }

    boolean startScreenSharing(MediaProjection mediaProjection, DisplayMetrics metrics) {
        Pair<MediaCodec, VirtualDisplay> r = createVirtualDisplay(mediaProjection, metrics);
        if (r != null)  {
            currentMediaProjection = mediaProjection;
            currentCodec = r.first;
            virtualDisplay = r.second;
            return true;
        }
        return false;
    }

    void stopScreenSharing() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (currentMediaProjection != null) {
            currentMediaProjection.stop();
            currentMediaProjection = null;
        }
    }

    void openCamera(VideoParams videoParams, TextureView surface, CameraListener listener, boolean hw_accel, int resolution, int bitrate) {
        CameraDevice camera = previewCamera;
        if (camera != null) {
            camera.close();
        }

        if (manager == null)
            return;
        Handler handler = getVideoHandler();
        try {
            AutoFitTextureView view = (AutoFitTextureView) surface;
            boolean flip = videoParams.rotation % 180 != 0;

            CameraCharacteristics cc = manager.getCameraCharacteristics(videoParams.id);
            final Range<Integer> fpsRange = chooseOptimalFpsRange(cc.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES));

            StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            final Size previewSize = chooseOptimalSize(streamConfigs == null ? null : streamConfigs.getOutputSizes(SurfaceHolder.class),
                    flip ? view.getHeight() : view.getWidth(), flip ? view.getWidth() : view.getHeight(),
                    videoParams.width, videoParams.height,
                    new Size(videoParams.width, videoParams.height));
            Log.d(TAG, "Selected preview size: " + previewSize + ", fps range: " + fpsRange + " rate: "+videoParams.rate);
            view.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());

            SurfaceTexture texture = view.getSurfaceTexture();
            Surface s = new Surface(texture);

            final Pair<MediaCodec, Surface> codec = (hw_accel && videoParams.getCodec() != null) ? openEncoder(videoParams, videoParams.getCodec(), handler, resolution, bitrate) : null;

            final List<Surface> targets = new ArrayList<>(2);
            targets.add(s);
            ImageReader tmpReader = null;
            if (codec != null && codec.second != null) {
                targets.add(codec.second);
            } else {
                tmpReader = ImageReader.newInstance(videoParams.width, videoParams.height, ImageFormat.YUV_420_888, 8);
                tmpReader.setOnImageAvailableListener(r -> {
                    Image image = r.acquireLatestImage();
                    if (image != null)
                        RingserviceJNI.captureVideoFrame(image, videoParams.rotation);
                }, handler);
                targets.add(tmpReader.getSurface());
            }
            final ImageReader reader = tmpReader;
            final boolean[] codecStarted = {false};

            manager.openCamera(videoParams.id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        Log.w(TAG, "onOpened");
                        previewCamera = camera;
                        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                        CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(s);
                        if (codec != null && codec.second != null) {
                            builder.addTarget(codec.second);
                        } else if (reader != null) {
                            builder.addTarget(reader.getSurface());
                        }
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                        final CaptureRequest request = builder.build();

                        camera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.w(TAG, "onConfigured");
                                listener.onOpened();
                                try {
                                    session.setRepeatingRequest(request, (codec != null && codec.second != null) ? new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                            if (frameNumber == 1) {
                                                try {
                                                    codec.first.start();
                                                    codecStarted[0] = true;
                                                } catch (Exception e) {
                                                    listener.onError();
                                                }
                                            }
                                        }
                                    } : null, handler);
                                    if (codec != null && codec.first != null) {
                                        currentCodec = codec.first;
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "onConfigured error:", e);
                                    camera.close();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                listener.onError();
                                Log.w(TAG, "onConfigureFailed");
                            }

                            @Override
                            public void onClosed(@NonNull CameraCaptureSession session) {
                                Log.w(TAG, "CameraCaptureSession onClosed");
                            }
                        }, handler);
                    } catch (Exception e) {
                        Log.w(TAG, "onOpened error:", e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "onDisconnected");
                    camera.close();
                    listener.onError();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.w(TAG, "onError: " + error);
                    camera.close();
                    listener.onError();
                }

                @Override
                public void onClosed(@NonNull CameraDevice camera) {
                    Log.w(TAG, "onClosed");
                    if (previewCamera == camera)
                        previewCamera = null;
                    if (codec != null) {
                        if (codec.first != null) {
                            if (codecStarted[0])
                                codec.first.signalEndOfInputStream();
                            codec.first.release();
                            if (codec.first == currentCodec)
                                currentCodec = null;
                        }
                        if (codec.second != null)
                            codec.second.release();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                    s.release();
                }
            }, handler);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while settings preview parameters", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception while settings preview parameters", e);
        }
    }

    public boolean isOpen() {
        return previewCamera != null;
    }

    List<String> getCameraIds() {
        return devices == null ? new ArrayList<>() : devices.cameras;
    }

    public int getCameraCount() {
        try {
            return devices == null ? manager.getCameraIdList().length : devices.cameras.size();
        } catch (CameraAccessException e) {
            return 0;
        }
    }

    void fillCameraInfo(DeviceParams p, String camId, IntVect formats, UintVect sizes, UintVect rates, Point minVideoSize) {
        if (manager == null)
            return;
        try {
            final CameraCharacteristics cc = manager.getCameraCharacteristics(camId);
            StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamConfigs == null)
                return;
            Size[] rawSizes = streamConfigs.getOutputSizes(ImageFormat.YUV_420_888);
            Size newSize = rawSizes[0];
            for (Size s : rawSizes) {
                if (s.getWidth() < s.getHeight()) {
                    continue;
                }
                if ((s.getWidth() == minVideoSize.x && s.getHeight() == minVideoSize.y)
                        // Has height closer but still higher than target
                        || (newSize.getHeight() < minVideoSize.y
                            ? s.getHeight() > newSize.getHeight()
                            : (s.getHeight() >= minVideoSize.y && s.getHeight() < newSize.getHeight()))
                        // Has width closer but still higher than target
                        || (s.getHeight() == newSize.getHeight()
                            && newSize.getWidth() < minVideoSize.x
                                    ? s.getWidth() > newSize.getWidth()
                                    : (s.getWidth() >= minVideoSize.x && s.getWidth() < newSize.getWidth()))) {
                    newSize = s;
                }
            }
            p.size.x = newSize.getWidth();
            p.size.y = newSize.getHeight();

            long minDuration = streamConfigs.getOutputMinFrameDuration(ImageFormat.YUV_420_888, newSize);
            double maxfps = 1000e9d / minDuration;
            long fps = (long) maxfps;
            rates.add(fps);
            p.rate = fps;

            int facing = cc.get(CameraCharacteristics.LENS_FACING);
            p.infos.orientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            p.infos.facing = facing == CameraCharacteristics.LENS_FACING_FRONT ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        } catch (Exception e) {
            Log.e(TAG, "An error occurred getting camera info", e);
        }
    }

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

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
