/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.model.SipCall;
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
    private static final Point VIDEO_SIZE_ULTRA_HD = new Point(3840, 2160);

    private static final String TAG = HardwareServiceImpl.class.getSimpleName();
    private static WeakReference<TextureView> mCameraPreviewSurface = new WeakReference<>(null);
    private static WeakReference<SipCall> mCameraPreviewCall = new WeakReference<>(null);

    private static final Map<String, WeakReference<SurfaceHolder>> videoSurfaces = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Shm> videoInputs = new HashMap<>();
    private final Context mContext;
    private final CameraServiceCamera2 cameraService;
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
        cameraService = new CameraServiceCamera2(mContext);
    }

    public void initVideo() {
        Log.i(TAG, "initVideo()");
        mExecutor.submit(cameraService::init);
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
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                recorder.setOutputFile(testFile.getAbsolutePath());
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
        Log.d(TAG, "updateAudioState: Call state updated to " + state + " Call is incoming: " + incomingCall + " Call is video: " + isOngoingVideo);
        boolean callEnded = state.equals(State.HUNGUP) || state.equals(State.FAILURE) || state.equals(State.OVER);
        if (mBluetoothWrapper == null && !callEnded) {
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
                if (incomingCall) {
                    // ringtone for incoming calls
                    mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                    setAudioRouting(true);
                    mShouldSpeakerphone = isOngoingVideo;
                }
                else
                    setAudioRouting(isOngoingVideo);
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
                closeAudioState();
                break;
        }
    }

    /*
    This is required in the case where a call is incoming. If you have an incoming call, and no bluetooth device is connected, the ringer should always be played through the speaker.
    However, this results in the call starting in a state where the speaker is always on and the UI is in an incorrect state.
    If it is a bluetooth device, it takes priority and does not play on speaker regardless. Otherwise, it returns mShouldSpeakerphone which was updated in updateaudiostate.
     */
    @Override
    public boolean shouldPlaySpeaker() {
        if(mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth() && mBluetoothWrapper.isBTHeadsetConnected() )
            return false;
        else
            return mShouldSpeakerphone;
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
        // prioritize bluetooth by checking for bluetooth device first
        if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth() && mBluetoothWrapper.isBTHeadsetConnected()) {
            routeToBTHeadset();
        } else if (mHasSpeakerPhone && mShouldSpeakerphone) {
            routeToSpeaker();
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

    /**
     * Routes audio to a bluetooth headset.
     */
    private void routeToBTHeadset() {
        Log.d(TAG, "routeToBTHeadset: Try to enable bluetooth");
        mAudioManager.setSpeakerphoneOn(false);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mBluetoothWrapper.setBluetoothOn(true);
    }

    /**
     * Routes audio to the device's speaker and takes into account whether the transition is coming from bluetooth.
     */
    private void routeToSpeaker() {

        // if we are returning from bluetooth mode, switch to mode normal, otherwise, we switch to mode in communication
        if (mAudioManager.isBluetoothScoOn()) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mBluetoothWrapper.setBluetoothOn(false);
        } else
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mAudioManager.setSpeakerphoneOn(true);
    }

    /**
     * Returns to earpiece audio
     */
    private void resetAudio() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (mBluetoothWrapper != null)
            mBluetoothWrapper.setBluetoothOn(false);
        mAudioManager.setSpeakerphoneOn(false);
    }


    @Override
    public void toggleSpeakerphone(boolean checked) {
        if (!mHasSpeakerPhone && checked) {
            return;
        }
        mShouldSpeakerphone = checked;
        Log.w(TAG, "toggleSpeakerphone setSpeakerphoneOn " + checked);
        if (checked) {
            routeToSpeaker();
        } else if (mBluetoothWrapper != null && mBluetoothWrapper.canBluetooth() && mBluetoothWrapper.isBTHeadsetConnected()) {
            routeToBTHeadset();
        } else {
            resetAudio();
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
            if (mShouldSpeakerphone)
                routeToSpeaker();
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
        mShouldCapture = true;
        if (mIsCapturing && mCapturingId != null && mCapturingId.equals(camId)) {
            return;
        }
        CameraService.VideoParams videoParams = cameraService.getParams(camId);
        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        final TextureView surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            cameraService.setPreviewParams(videoParams);
            VideoEvent event = new VideoEvent();
            event.start = true;
            videoEvents.onNext(event);
            return;
        }
        final SipCall call = mCameraPreviewCall.get();
        if (call != null) {
            call.setDetails(Ringservice.getCallDetails(call.getCallId()).toNative());
            videoParams.codec = call.getVideoCodec();
        }
        Log.w(TAG, "startCapture: call " + camId + " " + videoParams.codec);

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
            }, mPreferenceService.getUserSettings().isHwEncoding(), mPreferenceService.getUserSettings().isHD());
        });
        cameraService.setPreviewParams(videoParams);
        VideoEvent event = new VideoEvent();
        event.started = true;
        event.w = videoParams.width;
        event.h = videoParams.height;
        event.rot = videoParams.rotation;
        videoEvents.onNext(event);
    }

    @Override
    public void stopCapture() {
        Log.d(TAG, "stopCapture: " + cameraService.isOpen());
        mShouldCapture = false;
        endCapture();
    }

    public void requestKeyFrame() {
        cameraService.requestKeyFrame();
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
    public void addPreviewVideoSurface(Object oholder, SipCall call) {
        if (!(oholder instanceof TextureView)) {
            return;
        }
        TextureView holder = (TextureView) oholder;
        Log.w(TAG, "addPreviewVideoSurface " + holder.hashCode() + " mCapturingId " + mCapturingId);
        if (mCameraPreviewSurface.get() == oholder)
            return;
        mCameraPreviewSurface = new WeakReference<>(holder);
        mCameraPreviewCall = new WeakReference<>(call);
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
    public void switchInput(String id, boolean setDefaultCamera) {
        Log.w(TAG, "switchInput " + id);
        String camId = cameraService.switchInput(setDefaultCamera);
        try {
            final StringMap map = cameraService.getNativeParams(camId).toMap();
            final String uri = "camera://" + camId;
            switchInput(id, uri, map);
        } catch (Exception e) {
            Log.e(TAG, "Error with hardware service, switchInput: " + e.getMessage());
        }
    }

    @Override
    public void restartCamera(String id) {
        Log.w(TAG, "restartCamera " + id);
        endCapture();
        setPreviewSettings();
        String currentCamera = cameraService.getCurrentCamera();
        final String uri = "camera://" + currentCamera;
        final StringMap map = cameraService.getNativeParams(currentCamera).toMap();
        switchInput(id, uri, map);
    }

    @Override
    public void setPreviewSettings() {
        setPreviewSettings(cameraService.getPreviewSettings());
    }

    @Override
    public int getCameraCount() {
        return cameraService.getCameraCount();
    }

    @Override
    public boolean isPreviewFromFrontCamera() {
        return cameraService.isPreviewFromFrontCamera();
    }

    @Override
    public void setDeviceOrientation(int rotation) {
        cameraService.setOrientation(rotation);
        if (mCapturingId != null) {
            CameraService.VideoParams videoParams = cameraService.getParams(mCapturingId);
            VideoEvent event = new VideoEvent();
            event.started = true;
            event.w = videoParams.width;
            event.h = videoParams.height;
            event.rot = videoParams.rotation;
            videoEvents.onNext(event);
        }
    }

    @Override
    protected String[] getVideoDevices() {
        return cameraService.getCameraIds();
    }

    private static class Shm {
        String id;
        int w, h;
        long window = 0;
    }
}
