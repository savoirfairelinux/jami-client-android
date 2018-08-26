/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.utils.BluetoothWrapper;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Ringer;

import static cx.ring.model.SipCall.State;

public class HardwareServiceImpl extends HardwareService implements AudioManager.OnAudioFocusChangeListener, BluetoothWrapper.BluetoothChangeListener {

    private static final Point VIDEO_SIZE_LOW = new Point(320, 240);
    private static final Point VIDEO_SIZE_DEFAULT = new Point(720, 480);
    private static final Point VIDEO_SIZE_HD = new Point(1280, 720);
    private static final Point VIDEO_SIZE_FULL_HD = new Point(1920, 1080);

    //private static final int VIDEO_WIDTH_HD = 1280;
    public static final int VIDEO_WIDTH = 640;
    //private static final int VIDEO_WIDTH_MIN = 320;
    public static final int VIDEO_HEIGHT = 480;
    //private static final int MIN_VIDEO_HEIGHT = 240;

    public static final String TAG = HardwareServiceImpl.class.getName();
    private static WeakReference<SurfaceHolder> mCameraPreviewSurface = new WeakReference<>(null);
    private static Map<String, WeakReference<SurfaceHolder>> videoSurfaces = Collections.synchronizedMap(new HashMap<String, WeakReference<SurfaceHolder>>());
    private final Map<String, Shm> videoInputs = new HashMap<>();
    private final HashMap<String, VideoParams> mParams = new HashMap<>();
    private final Map<String, DeviceParams> mNativeParams = new HashMap<>();
    private Context mContext;
    private String cameraFront = null;
    private String cameraBack = null;
    private String cameraExternal = null;

    private String currentCamera = null;
    private VideoParams previewParams = null;

    private Object previewCamera = null;

    private Ringer mRinger;
    private AudioManager mAudioManager;
    private BluetoothWrapper mBluetoothWrapper;
    private final HandlerThread t = new HandlerThread("videoHandler");

    public HardwareServiceImpl(Context mContext) {
        this.mContext = mContext;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRinger = new Ringer(mContext);
    }

    public void initVideo() {
        Log.i(TAG, "initVideo()");
        mNativeParams.clear();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null)
                return;
            try {
                for (String id : manager.getCameraIdList()) {
                    currentCamera = id;
                    addVideoDevice(id);
                    CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                    int facing = cc.get(CameraCharacteristics.LENS_FACING);
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraFront = id;
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraBack = id;
                    } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        cameraExternal = id;
                    }
                }
                if (!TextUtils.isEmpty(cameraFront))
                    currentCamera = cameraFront;
                setDefaultVideoDevice(currentCamera);
            } catch (Exception e) {
                Log.w(TAG, "initVideo: can't enumerate devices", e);
            }
        } else {
            int numberCameras = Camera.getNumberOfCameras();
            if (numberCameras > 0) {
                Camera.CameraInfo camInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberCameras; i++) {
                    addVideoDevice(Integer.toString(i));
                    Camera.getCameraInfo(i, camInfo);
                    if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        cameraFront = Integer.toString(i);
                    } else {
                        cameraBack = Integer.toString(i);
                    }
                }
                currentCamera = cameraFront;
                setDefaultVideoDevice(cameraFront);
            } else {
                Log.w(TAG, "initVideo: No camera available");
                currentCamera = null;
                cameraFront = null;
            }
        }
    }

    private String[] getCameraIds() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                return manager.getCameraIdList();
            } catch (Exception e) {
                Log.w(TAG, "getCameraIds: can't enumerate devices", e);
                return new String[0];
            }
        } else {
            int numberCameras = Camera.getNumberOfCameras();
            String[] ids = new String[numberCameras];
            for (int i = 0; i < numberCameras; i++)
                ids[i] = Integer.toString(i);
            return ids;
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
            File testFile = new File(mContext.getCacheDir(), "MediaUtil#micAvailTestFile");
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(testFile.getAbsolutePath());
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
                testFile.delete();
            }
        }

        return hasMicrophone;
    }

    @Override
    public boolean isSpeakerPhoneOn() {
        return mAudioManager.isSpeakerphoneOn();
    }

    @Override
    public void updateAudioState(final State state, final boolean isOngoingVideo) {
        if (mBluetoothWrapper == null) {
            mBluetoothWrapper = new BluetoothWrapper(mContext);
            mBluetoothWrapper.registerScoUpdate();
            mBluetoothWrapper.registerBtConnection();
            mBluetoothWrapper.setBluetoothChangeListener(this);
        }
        switch (state) {
            case RINGING:
                startRinging();
                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                setAudioRouting(true);
                mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                break;
            case CURRENT:
                stopRinging();
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                setAudioRouting(isOngoingVideo);
                break;
            case HOLD:
            case UNHOLD:
                break;
            default:
                stopRinging();
                mAudioManager.abandonAudioFocus(this);
                break;
        }
    }

    @Override
    public void closeAudioState() {
        stopRinging();
        abandonAudioFocus();
    }

    @Override
    public void startRinging() {
        mRinger.ring();
    }

    @Override
    public void stopRinging() {
        mRinger.stopRing();
    }

    @Override
    public void onAudioFocusChange(int arg0) {
        Log.i(TAG, "onAudioFocusChange " + arg0);
    }

    @Override
    public void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(this);
        if (mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(false);
        }
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        if (mBluetoothWrapper != null) {
            mBluetoothWrapper.unregister();
            mBluetoothWrapper.setBluetoothOn(false);
            mBluetoothWrapper = null;
        }
    }

    private void setAudioRouting(boolean requestSpeakerOn) {
        if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
            Log.d(TAG, "setAudioRouting: Try to enable bluetooth");
            mBluetoothWrapper.setBluetoothOn(true);
        } else if (!mAudioManager.isWiredHeadsetOn() && hasSpeakerphone() && !DeviceUtils.isTv(mContext)) {
            mAudioManager.setSpeakerphoneOn(requestSpeakerOn);
        }
    }

    private boolean hasSpeakerphone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
            PackageManager packageManager = mContext.getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false;
            }

            AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void routeToBTHeadset() {
        Log.d(TAG, "routeToBTHeadset: Try to enable bluetooth");
        mAudioManager.setSpeakerphoneOn(false);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mBluetoothWrapper.setBluetoothOn(true);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @Override
    public void toggleSpeakerphone(boolean checked) {
        if (!hasSpeakerphone() || checked == mAudioManager.isSpeakerphoneOn()) {
            return;
        }
        if (checked) {
            mAudioManager.setSpeakerphoneOn(true);
        } else {
            mAudioManager.setSpeakerphoneOn(false);
            if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
                routeToBTHeadset();
            }
        }
    }

    @Override
    public void onBluetoothStateChanged(int status) {
        Log.d(TAG, "bluetoothStateChanged to: " + status);
        if (mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            routeToBTHeadset();
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

                    VideoEvent event = new VideoEvent();
                    event.start = true;
                    event.callId = shm.id;
                    videoEvents.onNext(event);
                    return;
                }

                VideoEvent event = new VideoEvent();
                event.callId = shm.id;
                event.started = true;
                event.w = shm.w;
                event.h = shm.h;
                videoEvents.onNext(event);
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
        // Use a larger resolution for Android 6.0+, 64 bits devices
        final boolean useLargerSize = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.SUPPORTED_64_BIT_ABIS.length > 0;
        final boolean useHD = DeviceUtils.isTv(mContext) || mPreferenceService.getSettings().isHD();
        //int MIN_WIDTH = useLargerSize ? (useHD ? VIDEO_WIDTH_HD : VIDEO_WIDTH) : VIDEO_WIDTH_MIN;
        final Point minVideoSize = useLargerSize ? (useHD ? VIDEO_SIZE_HD : VIDEO_SIZE_DEFAULT) : VIDEO_SIZE_LOW;

        Log.d(TAG, "getCameraInfo: " + camId + " largerSize:" + useLargerSize + " HD: " + useHD + " min. size: " + minVideoSize);

        DeviceParams p = new DeviceParams();
        p.size = new Point(0, 0);
        p.infos = new Camera.CameraInfo();

        rates.clear();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null)
                return;
            try {
                CameraCharacteristics cc = manager.getCameraCharacteristics(camId);
                StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigs == null)
                    return;
                Size[] rawSizes = streamConfigs.getOutputSizes(ImageFormat.YUV_420_888);
                Size newSize = rawSizes[0];
                for (Size s : rawSizes) {
                    if (s.getWidth() < s.getHeight()) {
                        continue;
                    }
                    if ((s.getWidth() == minVideoSize.x && s.getHeight() == minVideoSize.y) ||
                            (newSize.getWidth() < minVideoSize.x
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
        } else {
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
        Log.d(TAG, "getCameraInfo: using resolution " + p.size.x + "x" + p.size.y + " " + p.rate + " FPS orientation: " + p.infos.orientation);

        sizes.clear();
        sizes.add(p.size.x);
        sizes.add(p.size.y);
        sizes.add(p.size.y);
        sizes.add(p.size.x);

        mNativeParams.put(camId, p);
    }

    @Override
    public void setParameters(String camId, int format, int width, int height, int rate) {
        Log.d(TAG, "setParameters: " + camId + ", " + format + ", " + width + ", " + height + ", " + rate);
        DeviceParams deviceParams = mNativeParams.get(camId);
        VideoParams newParams = new VideoParams(camId, format, deviceParams.size.x, deviceParams.size.y, rate);
        newParams.rotWidth = width;
        newParams.rotHeight = height;
        if (deviceParams.infos != null) {
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
            if (deviceParams.infos.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                newParams.rotation = (deviceParams.infos.orientation + rotation + 360) % 360;
            } else {
                newParams.rotation = (deviceParams.infos.orientation - rotation + 360) % 360;
            }
        }
        mParams.put(camId, newParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void listSupportedCodecs() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            try {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                for (String type : codecInfo.getSupportedTypes()) {
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

                    Range<Integer> widths = video_caps.getSupportedWidths();
                    Range<Integer> heights = video_caps.getSupportedHeights();
                    Log.w(TAG, "Supported sizes: " + widths.getLower() + "x" + heights.getLower() + " -> " + widths.getUpper() + "x" + heights.getUpper());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.w(TAG, "AchievableFrameRates: " + video_caps.getAchievableFrameRatesFor(widths.getUpper(), heights.getUpper()));
                    }
                    Log.w(TAG, "SupportedFrameRates: " + video_caps.getSupportedFrameRatesFor(widths.getUpper(), heights.getUpper()));

                    for (MediaCodecInfo.CodecProfileLevel profileLevel : codecCaps.profileLevels)
                        Log.w(TAG, "profileLevels: " + profileLevel);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Log.w(TAG, "FEATURE_IntraRefresh: " + codecCaps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_IntraRefresh));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Can't query codec info", e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Pair<MediaCodec, Surface> openCameraWithEncoder(VideoParams videoParams, String mimeType) {
        final int BITRATE = 1600 * 1024;
        MediaFormat format = MediaFormat.createVideoFormat(mimeType, videoParams.width, videoParams.height);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1);
            format.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, 5);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        }*/
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        //format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.CodecCapabilities.BITRATE_MODE_VBR);

        String codecName = new MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format);
        Surface encoderInput = null;
        MediaCodec codec = null;
        if (codecName != null) {
            try {
                codec = MediaCodec.createByCodecName(codecName);
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, BITRATE);
                codec.setParameters(params);
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoderInput = codec.createInputSurface();
                codec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                        ByteBuffer buffer = codec.getOutputBuffer(index);
                        RingserviceJNI.captureVideoPacket(buffer, info.size, info.offset, (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0, info.presentationTimeUs);
                        codec.releaseOutputBuffer(index, false);
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        Log.e(TAG, "MediaCodec onError", e);
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        Log.e(TAG, "MediaCodec onOutputFormatChanged " + format);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Can't open codec", e);
                codec = null;
                encoderInput = null;
            }
        }
        return new Pair<>(codec, encoderInput);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera(VideoParams videoParams, SurfaceHolder surface) {
        Log.e(TAG, "openCamera " + videoParams.width + "x" + videoParams.height);

        CameraDevice camera = (CameraDevice) previewCamera;
        if (camera != null) {
            camera.close();
        }

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null)
            return;
        if (t.getState() == Thread.State.NEW)
            t.start();
        Handler handler = new Handler(t.getLooper());
        try {
            surface.setFixedSize(videoParams.width, videoParams.height);
            Surface s = surface.getSurface();

            ImageReader reader = ImageReader.newInstance(videoParams.width, videoParams.height, ImageFormat.YUV_420_888, 8);
            reader.setOnImageAvailableListener(r -> {
                Image image = r.acquireLatestImage();
                if (image != null)
                    RingserviceJNI.captureVideoFrame(image, videoParams.rotation);
            }, handler);

            List<Surface> targets = new ArrayList<>(2);
            targets.add(s);
            targets.add(reader.getSurface());
            /*if (encoderInput != null)
                targets.add(encoderInput);*/

            manager.openCamera(videoParams.id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        Log.w(TAG, "onOpened");
                        previewCamera = camera;
                        CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(s);
                        builder.addTarget(reader.getSurface());
                        /*if (encoderInput != null)
                            builder.addTarget(encoderInput);*/
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                        final CaptureRequest request = builder.build();

                        camera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.w(TAG, "onConfigured");
                                try {
                                    session.setRepeatingRequest(request, /*new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                                            if (codec != null && frameNumber == 1) {
                                                codec.start();
                                            }
                                        }
                                    }*/null, handler);
                                } catch (CameraAccessException e) {
                                    Log.w(TAG, "onConfigured error:", e);
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.w(TAG, "onConfigureFailed");
                            }
                        }, handler);
                    } catch (CameraAccessException e) {
                        Log.w(TAG, "onOpened error:", e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "onDisconnected");
                    if (previewCamera == camera) {
                        previewCamera = null;
                    }
                    camera.close();
                    /*if (codec != null) {
                        codec.signalEndOfInputStream();
                        codec.release();
                    }*/
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.w(TAG, "onError: " + error);
                    if (previewCamera == camera)
                        previewCamera = null;
                    /*if (codec != null) {
                        codec.release();
                    }*/
                }
            }, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error while settings preview parameters", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Error while settings preview parameters", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while settings preview parameters", e);
        }
    }

    private void openCameraKitkat(VideoParams videoParams, SurfaceHolder surface) {
        final Camera preview;
        try {
            if (previewCamera != null) {
                ((Camera) previewCamera).release();
                previewCamera = null;
            }
            int id = Integer.parseInt(videoParams.id);
            preview = Camera.open(id);
            setCameraDisplayOrientation(id, preview);
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
                endCapture();
            }
        });
        try {
            preview.startPreview();
        } catch (RuntimeException e) {
            Log.e(TAG, "startPreview: " + e.getMessage());
            return;
        }
        previewCamera = preview;
    }

    private String mCapturingId = null;
    private boolean mIsCapturing = false;
    private boolean mShouldCapture = false;

    @Override
    public void startCapture(@Nullable String camId) {
        Log.w(TAG, "startCapture: call " + camId);
        mShouldCapture = true;
        if (mIsCapturing && mCapturingId != null && mCapturingId.equals(camId)) {
            return;
        }
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
        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        final SurfaceHolder surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            previewParams = videoParams;
            VideoEvent event = new VideoEvent();
            event.start = true;
            videoEvents.onNext(event);
            return;
        }

        mIsCapturing = true;
        mCapturingId = videoParams.id;
        Log.d(TAG, "startCapture: startCapture " + videoParams.id + " " + videoParams.width + "x" + videoParams.height + " rot" + videoParams.rotation);

        mUiScheduler.scheduleDirect(() -> {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                openCamera(videoParams, surface);
            } else {
                openCameraKitkat(videoParams, surface);
            }

            previewParams = videoParams;

            VideoEvent event = new VideoEvent();
            event.started = true;
            boolean s = videoParams.rotation % 180 != 0;
            event.w = s ? videoParams.height : videoParams.width;
            event.h = s ? videoParams.width : videoParams.height;
            videoEvents.onNext(event);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeCamera() {
        CameraDevice camera = (CameraDevice) previewCamera;
        previewCamera = null;
        camera.close();
    }

    private void closeCameraKitkat() {
        final Camera preview = (Camera) previewCamera;
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

    @Override
    public void stopCapture() {
        Log.d(TAG, "stopCapture: " + previewCamera);
        mShouldCapture = false;
        endCapture();
    }

    public void endCapture() {
        if (previewCamera != null) {
            final VideoParams params = previewParams;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                closeCamera();
            } else {
                closeCameraKitkat();
            }

            VideoEvent event = new VideoEvent();
            event.started = false;
            event.w = params.width;
            event.h = params.height;
            videoEvents.onNext(event);
        }
        mIsCapturing = false;
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

            VideoEvent event = new VideoEvent();
            event.start = true;
            videoEvents.onNext(event);
            return;
        }

        VideoEvent event = new VideoEvent();
        event.callId = shm.id;
        event.started = true;
        event.w = shm.w;
        event.h = shm.h;
        videoEvents.onNext(event);

    }

    @Override
    public void addPreviewVideoSurface(Object oholder) {
        if (!(oholder instanceof SurfaceHolder)) {
            return;
        }
        SurfaceHolder holder = (SurfaceHolder)oholder;
        Log.w(TAG, "addPreviewVideoSurface " + holder.hashCode() + " mCapturingId " + mCapturingId);
        if (mCameraPreviewSurface.get() == holder)
            return;
        mCameraPreviewSurface = new WeakReference<>(holder);
        if (mShouldCapture && !mIsCapturing) {
            startCapture(mCapturingId);
        }
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

        VideoEvent event = new VideoEvent();
        event.callId = shm.id;
        event.started = false;
        videoEvents.onNext(event);
    }

    @Override
    public void removePreviewVideoSurface() {
        Log.w(TAG, "removePreviewVideoSurface");
        mCameraPreviewSurface.clear();
    }

    @Override
    public void switchInput(String id) {
        Log.w(TAG, "switchInput " + id);

        String camId;
        if (currentCamera.equals(cameraBack)) {
            camId = cameraExternal;
        } else if (currentCamera.equals(cameraFront)) {
            camId = cameraBack;
        } else {
            camId = cameraFront;
        }
        currentCamera = camId;

        final String uri = "camera://" + camId;
        final StringMap map = mNativeParams.get(camId).toMap(mContext.getResources().getConfiguration().orientation);
        switchInput(id, uri, map);
    }

    @Override
    public void restartCamera(String id) {
        Log.w(TAG, "restartCamera " + id);
        endCapture();
        setPreviewSettings();
        final String uri = "camera://" + currentCamera;
        final StringMap map = mNativeParams.get(currentCamera).toMap(mContext.getResources().getConfiguration().orientation);
        switchInput(id, uri, map);
    }

    @Override
    public void setPreviewSettings() {
        Map<String, StringMap> camSettings = new HashMap<>();
        for (String id : getCameraIds()) {
            DeviceParams params = mNativeParams.get(id);
            if (params != null) {
                camSettings.put(id, params.toMap(mContext.getResources().getConfiguration().orientation));
                Log.w(TAG, "setPreviewSettings camera:" + id);
            }
        }
        setPreviewSettings(camSettings);
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
