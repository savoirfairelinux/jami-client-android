/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.jami.daemon.IntVect;
import net.jami.daemon.JamiService;
import net.jami.daemon.StringMap;
import net.jami.daemon.UintVect;
import net.jami.model.Conference;
import net.jami.model.Call;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.Tuple;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public abstract class HardwareService {

    private static final String TAG = HardwareService.class.getSimpleName();

    private final ScheduledExecutorService mExecutor;
    protected final PreferencesService mPreferenceService;
    protected final Scheduler mUiScheduler;

    public HardwareService(ScheduledExecutorService executor, PreferencesService preferenceService, Scheduler uiScheduler) {
        mExecutor = executor;
        mPreferenceService = preferenceService;
        mUiScheduler = uiScheduler;
    }

    public static class VideoEvent {
        public boolean start = false;
        public boolean started = false;
        public int w = 0, h = 0;
        public int rot = 0;
        public String callId = null;
    }
    public static class BluetoothEvent {
        public boolean connected;
    }
    public enum AudioOutput {
        INTERNAL, SPEAKERS, BLUETOOTH
    }
    public static class AudioState {
        private final AudioOutput outputType;
        private final String outputName;

        public AudioState(AudioOutput ot) { outputType = ot; outputName = null; }
        public AudioState(AudioOutput ot, String name) { outputType = ot; outputName = name; }

        public AudioOutput getOutputType() { return outputType; }
        public String getOutputName() { return outputName; }
    }
    protected static final AudioState STATE_SPEAKERS = new AudioState(AudioOutput.SPEAKERS);
    protected static final AudioState STATE_INTERNAL = new AudioState(AudioOutput.INTERNAL);

    protected final Subject<VideoEvent> videoEvents = PublishSubject.create();
    protected final Subject<BluetoothEvent> bluetoothEvents = PublishSubject.create();
    protected final Subject<AudioState> audioStateSubject = BehaviorSubject.createDefault(STATE_INTERNAL);
    protected final Subject<Boolean> connectivityEvents = BehaviorSubject.create();

    public Observable<VideoEvent> getVideoEvents() {
        return videoEvents;
    }
    public Observable<BluetoothEvent> getBluetoothEvents() {
        return bluetoothEvents;
    }
    public Observable<AudioState> getAudioState() {
        return audioStateSubject;
    }
    public Observable<Boolean> getConnectivityState() {
        return connectivityEvents;
    }

    public abstract Completable initVideo();

    public abstract boolean isVideoAvailable();

    public abstract void updateAudioState(Call.CallStatus state, boolean incomingCall, boolean isOngoingVideo);

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
    public abstract boolean startScreenShare(Object mediaProjection);

    public abstract boolean hasMicrophone();

    public abstract void stopCapture();
    public abstract void endCapture();
    public abstract void stopScreenShare();

    public abstract void requestKeyFrame();
    public abstract void setBitrate(String device, int bitrate);

    public abstract void addVideoSurface(String id, Object holder);
    public abstract void updateVideoSurfaceId(String currentId, String newId);
    public abstract void removeVideoSurface(String id);

    public abstract void addPreviewVideoSurface(Object holder, Conference conference);
    public abstract void updatePreviewVideoSurface(Conference conference);
    public abstract void removePreviewVideoSurface();

    public abstract void switchInput(String id, boolean setDefaultCamera);

    public abstract void setPreviewSettings();

    public abstract boolean hasCamera();
    public abstract int getCameraCount();
    public abstract Observable<Tuple<Integer, Integer>> getMaxResolutions();

    public abstract boolean isPreviewFromFrontCamera();

    public abstract boolean shouldPlaySpeaker();

    public abstract void unregisterCameraDetectionCallback();

    public abstract void startMediaHandler(String mediaHandlerId);

    public abstract void stopMediaHandler();

    public void connectivityChanged(boolean isConnected) {
        Log.i(TAG, "connectivityChange() " + isConnected);
        connectivityEvents.onNext(isConnected);
        mExecutor.execute(JamiService::connectivityChanged);
    }

    protected void switchInput(final String id, final String uri) {
        Log.i(TAG, "switchInput() " + uri);
        mExecutor.execute(() -> JamiService.switchInput(id, uri));
    }

    public void setPreviewSettings(final Map<String, StringMap> cameraMaps) {
        mExecutor.execute(() -> {
            Log.i(TAG, "applySettings() thread running...");
            for (Map.Entry<String, StringMap> entry : cameraMaps.entrySet()) {
                JamiService.applySettings(entry.getKey(), entry.getValue());
            }
        });
    }

    public long startVideo(final String inputId, final Object surface, final int width, final int height) {
        long inputWindow = JamiService.acquireNativeWindow(surface);
        if (inputWindow == 0) {
            return inputWindow;
        }
        JamiService.setNativeWindowGeometry(inputWindow, width, height);
        JamiService.registerVideoCallback(inputId, inputWindow);
        return inputWindow;
    }

    public void stopVideo(final String inputId, long inputWindow) {
        if (inputWindow == 0) {
            return;
        }
        JamiService.unregisterVideoCallback(inputId, inputWindow);
        JamiService.releaseNativeWindow(inputWindow);
    }

    public abstract void setDeviceOrientation(int rotation);

    protected abstract List<String> getVideoDevices();

    private Observable<String> logs = null;
    private Emitter<String> logEmitter = null;

    synchronized public boolean isLogging() {
        return logs != null;
    }

    synchronized public Observable<String> startLogs() {
        if (logs == null) {
            logs = Observable.create((ObservableOnSubscribe<String>) emitter -> {
                Log.w(TAG, "ObservableOnSubscribe JamiService.monitor(true)");
                logEmitter = emitter;
                JamiService.monitor(true);
                emitter.setCancellable(() -> {
                    Log.w(TAG, "ObservableOnSubscribe CANCEL JamiService.monitor(false)");
                    synchronized (HardwareService.this) {
                        JamiService.monitor(false);
                        logEmitter = null;
                        logs = null;
                    }
                });
            })
                    .observeOn(Schedulers.io())
                    .scan(new StringBuffer(1024), (sb, message) -> sb.append(message).append('\n'))
                    .throttleLatest(500, TimeUnit.MILLISECONDS)
                    .map(StringBuffer::toString)
                    .replay(1)
                    .autoConnect();
        }
        return logs;
    }

    synchronized public void stopLogs() {
        if (logEmitter != null) {
            Log.w(TAG, "stopLogs JamiService.monitor(false)");
            JamiService.monitor(false);
            logEmitter.onComplete();
            logEmitter = null;
            logs = null;
        }
    }

    void logMessage(String message) {
        if (logEmitter != null && !StringUtils.isEmpty(message)) {
            logEmitter.onNext(message);
        }
    }
}
