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

import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.Nullable;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.media.AudioAttributesCompat;
import cx.ring.daemon.IntVect;
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

    public static final int VIDEO_WIDTH = 640;
    public static final int VIDEO_HEIGHT = 480;

    private static final String TAG = HardwareServiceImpl.class.getName();
    private static WeakReference<SurfaceHolder> mCameraPreviewSurface = new WeakReference<>(null);
    private static final Map<String, WeakReference<SurfaceHolder>> videoSurfaces = Collections.synchronizedMap(new HashMap<String, WeakReference<SurfaceHolder>>());
    private final Map<String, Shm> videoInputs = new HashMap<>();
    private final Context mContext;
    private final CameraService cameraService;
    private final Ringer mRinger;
    private final AudioManager mAudioManager;
    private BluetoothWrapper mBluetoothWrapper;

    private String mCapturingId = null;
    private boolean mIsCapturing = false;
    private boolean mShouldCapture = false;
    private boolean mShouldSpeakerphone = false;
    private final boolean mHasSpeakerPhone;

    public HardwareServiceImpl(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHasSpeakerPhone = hasSpeakerphone();
        mRinger = new Ringer(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraService = new CameraServiceCamera2(mContext);
        } else {
            cameraService = new CameraServiceKitKat();
        }
    }

    public void initVideo() {
        Log.i(TAG, "initVideo()");
        cameraService.init();
    }

    public boolean isVideoAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) || cameraService.hasCamera();
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

    private static final AudioAttributesCompat RINGTONE_ATTRIBUTES = new AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE)
            .setLegacyStreamType(AudioManager.STREAM_RING)
            .build();

    private static final AudioAttributesCompat CALL_ATTRIBUTES = new AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributesCompat.USAGE_VOICE_COMMUNICATION)
            .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
            .build();

    @Override
    public void updateAudioState(final State state, final boolean incomingCall, final boolean isOngoingVideo) {
        if (mBluetoothWrapper == null) {
            mBluetoothWrapper = new BluetoothWrapper(mContext);
            mBluetoothWrapper.setBluetoothChangeListener(this);
            mBluetoothWrapper.registerScoUpdate();
            mBluetoothWrapper.registerBtConnection();
        }
        switch (state) {
            case RINGING:
                if (incomingCall)
                    startRinging();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    AudioFocusRequest req = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes((AudioAttributes) RINGTONE_ATTRIBUTES.unwrap())
                            .build();
                    mAudioManager.requestAudioFocus(req);
                } else {
                    mAudioManager.requestAudioFocus(this, RINGTONE_ATTRIBUTES.getLegacyStreamType(), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
                mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                setAudioRouting(true);
                break;
            case CURRENT:
                stopRinging();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    AudioFocusRequest req = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes((AudioAttributes) CALL_ATTRIBUTES.unwrap())
                            .build();
                    mAudioManager.requestAudioFocus(req);
                } else {
                    mAudioManager.requestAudioFocus(this, CALL_ATTRIBUTES.getLegacyStreamType(), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
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
        mShouldSpeakerphone = requestSpeakerOn;
        if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth()) {
            Log.d(TAG, "setAudioRouting: Try to enable bluetooth");
            mBluetoothWrapper.setBluetoothOn(true);
        } else if (!mAudioManager.isWiredHeadsetOn()
                && !DeviceUtils.isTv(mContext)
                && mHasSpeakerPhone) {
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
        if (!mHasSpeakerPhone || checked == mAudioManager.isSpeakerphoneOn()) {
            return;
        }
        if (checked) {
            mAudioManager.setSpeakerphoneOn(true);
        } else {
            mAudioManager.setSpeakerphoneOn(false);
            if (mBluetoothWrapper != null && mBluetoothWrapper.isBTHeadsetConnected()) {
                mBluetoothWrapper.setBluetoothOn(true);
            }
        }
    }

    @Override
    public void onBluetoothStateChanged(int status) {
        Log.d(TAG, "bluetoothStateChanged to: " + status);
        BluetoothEvent event = new BluetoothEvent();
        if (status == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            Log.d(TAG, "BluetoothHeadset Connected");
            event.connected = true;
        } else if (status == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            Log.d(TAG, "BluetoothHeadset Disconnected");
            event.connected = false;
            if (mShouldSpeakerphone) {
                mAudioManager.setSpeakerphoneOn(true);
            }
        }
        bluetoothEvents.onNext(event);
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

        cameraService.getCameraInfo(camId, formats, sizes, rates, minVideoSize);
    }

    @Override
    public void setParameters(String camId, int format, int width, int height, int rate) {
        Log.d(TAG, "setParameters: " + camId + ", " + format + ", " + width + ", " + height + ", " + rate);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        cameraService.setParameters(camId, format, width, height, rate, windowManager.getDefaultDisplay().getRotation());
    }

    @Override
    public void startCapture(@Nullable String camId) {
        Log.w(TAG, "startCapture: call " + camId);
        mShouldCapture = true;
        if (mIsCapturing && mCapturingId != null && mCapturingId.equals(camId)) {
            return;
        }
        CameraService.VideoParams videoParams = cameraService.getParams(camId);
        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        final SurfaceHolder surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            cameraService.setPreviewParams(videoParams);
            VideoEvent event = new VideoEvent();
            event.start = true;
            videoEvents.onNext(event);
            return;
        }

        mIsCapturing = true;
        mCapturingId = videoParams.id;
        Log.d(TAG, "startCapture: startCapture " + videoParams.id + " " + videoParams.width + "x" + videoParams.height + " rot" + videoParams.rotation);

        mUiScheduler.scheduleDirect(() -> {
            cameraService.openCamera(mContext, videoParams, surface, new CameraService.CameraListener() {
                @Override
                public void onOpened() {
                }

                @Override
                public void onError() {
                    stopCapture();
                }
            });
        });
        cameraService.setPreviewParams(videoParams);
        VideoEvent event = new VideoEvent();
        event.started = true;
        boolean s = videoParams.rotation % 180 != 0;
        event.w = s ? videoParams.height : videoParams.width;
        event.h = s ? videoParams.width : videoParams.height;
        videoEvents.onNext(event);

    }

    @Override
    public void stopCapture() {
        Log.d(TAG, "stopCapture: " + cameraService.isOpen());
        mShouldCapture = false;
        endCapture();
    }

    public void endCapture() {
        if (cameraService.isOpen()) {
            //final CameraService.VideoParams params = previewParams;
            cameraService.closeCamera();
            VideoEvent event = new VideoEvent();
            event.started = false;
            //event.w = params.width;
            //event.h = params.height;
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
        String camId = cameraService.switchInput();
        final StringMap map = cameraService.getNativeParams(camId).toMap(mContext.getResources().getConfiguration().orientation);
        final String uri = "camera://" + camId;
        switchInput(id, uri, map);
    }

    @Override
    public void restartCamera(String id) {
        Log.w(TAG, "restartCamera " + id);
        endCapture();
        setPreviewSettings();
        String currentCamera = cameraService.getCurrentCamera();
        final String uri = "camera://" + currentCamera;
        final StringMap map = cameraService.getNativeParams(currentCamera).toMap(mContext.getResources().getConfiguration().orientation);
        switchInput(id, uri, map);
    }

    @Override
    public void setPreviewSettings() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        setPreviewSettings(cameraService.getPreviewSettings(orientation));
    }

    @Override
    public int getCameraCount() {
        return cameraService.getCameraCount();
    }

    @Override
    public boolean isPreviewFromFrontCamera() {
        return cameraService.isPreviewFromFrontCamera();
    }

    private static class Shm {
        String id;
        int w, h;
        long window = 0;
    }
}
