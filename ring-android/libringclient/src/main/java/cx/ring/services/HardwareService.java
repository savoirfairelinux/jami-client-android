/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.model.SipCall;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public abstract class HardwareService {

    private static final String TAG = HardwareService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ScheduledExecutorService mExecutor;

    protected final ScheduledExecutorService mVideoExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "VideoThread"));

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    PreferencesService mPreferenceService;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    public class VideoEvent {
        public boolean start = false;
        public boolean started = false;
        public int w = 0, h = 0;
        public int rot = 0;
        public String callId = null;
    }
    public class BluetoothEvent {
        public boolean connected;
    }

    protected final Subject<VideoEvent> videoEvents = PublishSubject.create();
    protected final Subject<BluetoothEvent> bluetoothEvents = PublishSubject.create();

    public Observable<VideoEvent> getVideoEvents() {
        return videoEvents;
    }
    public Observable<BluetoothEvent> getBluetoothEvents() {
        return bluetoothEvents;
    }

    public abstract void initVideo();

    public abstract boolean isVideoAvailable();

    public abstract void updateAudioState(SipCall.CallStatus state, boolean incomingCall, boolean isOngoingVideo);

    public abstract void closeAudioState();

    public abstract boolean isSpeakerPhoneOn();

    public abstract void toggleSpeakerphone(boolean checked);

    public abstract void startRinging();

    public abstract void stopRinging();

    public abstract void abandonAudioFocus();

    public abstract void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer);

    public abstract void decodingStopped(String id, String shmPath, boolean isMixer);

    public abstract void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates);

    public abstract void setParameters(String camId, int format, int width, int height, int rate);

    public abstract void startCapture(String camId);

    public abstract boolean hasMicrophone();

    public abstract void stopCapture();
    public abstract void endCapture();
    public abstract void requestKeyFrame();

    public abstract void addVideoSurface(String id, Object holder);

    public abstract void addPreviewVideoSurface(Object holder, SipCall call);

    public abstract void removeVideoSurface(String id);

    public abstract void removePreviewVideoSurface();

    public abstract void switchInput(String id, boolean setDefaultCamera);

    public abstract void restartCamera(String callId);

    public abstract void setPreviewSettings();

    public abstract int getCameraCount();

    public abstract boolean isPreviewFromFrontCamera();

    public abstract boolean shouldPlaySpeaker();

    public void connectivityChanged() {
        Log.i(TAG, "connectivityChange()");
        mExecutor.execute(Ringservice::connectivityChanged);
    }

    public void switchInput(final String id, final String uri, final StringMap map) {
        mExecutor.execute(() -> {
            Log.i(TAG, "switchInput() running..." + uri);
            Ringservice.applySettings(id, map);
            Ringservice.switchInput(id, uri);
        });
    }

    public void setPreviewSettings(final Map<String, StringMap> cameraMaps) {
        mExecutor.execute(() -> {
            Log.i(TAG, "applySettings() thread running...");
            for (Map.Entry<String, StringMap> entry : cameraMaps.entrySet()) {
                Ringservice.applySettings(entry.getKey(), entry.getValue());
            }
        });
    }

    public long startVideo(final String inputId, final Object surface, final int width, final int height) {
        long inputWindow = RingserviceJNI.acquireNativeWindow(surface);
        if (inputWindow == 0) {
            return inputWindow;
        }

        RingserviceJNI.setNativeWindowGeometry(inputWindow, width, height);
        RingserviceJNI.registerVideoCallback(inputId, inputWindow);

        return inputWindow;
    }

    public void stopVideo(final String inputId, long inputWindow) {
        if (inputWindow == 0) {
            return;
        }
        RingserviceJNI.unregisterVideoCallback(inputId, inputWindow);
        RingserviceJNI.releaseNativeWindow(inputWindow);
    }

    public void setVideoFrame(final byte[] data, final int width, final int height, final int rotation) {
        mVideoExecutor.execute(() -> {
            long frame = RingserviceJNI.obtainFrame(data.length);
            if (frame != 0) {
                RingserviceJNI.setVideoFrame(data, data.length, frame, width, height, rotation);
            }
            RingserviceJNI.releaseFrame(frame);
        });
    }

    public void addVideoDevice(String deviceId) {
        Log.d(TAG, "addVideoDevice: " + deviceId);
        RingserviceJNI.addVideoDevice(deviceId);
    }

    public void setDefaultVideoDevice(String deviceId) {
        Log.d(TAG, "setDefaultVideoDevice: " + deviceId);
        RingserviceJNI.setDefaultDevice(deviceId);
    }

    public abstract void setDeviceOrientation(int rotation);

    protected abstract String[] getVideoDevices();
}
