/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.IntVect;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.UintVect;
import cx.ring.daemon.VideoCallback;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

public abstract class HardwareService extends Observable {

    private static final String TAG = HardwareService.class.getName();
    public final static String VIDEO_EVENT = "VIDEO_EVENT";

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private VideoCallback mVideoCallback;

    public abstract void initVideo();

    public abstract boolean isSpeakerPhoneOn();

    public abstract void switchSpeakerPhone();

    public abstract void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer);

    public abstract void decodingStopped(String id, String shmPath, boolean isMixer);

    public abstract void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates);

    public abstract void setParameters(String camId, int format, int width, int height, int rate);

    public abstract void startCapture(String camId);

    public abstract void stopCapture();

    public abstract void addVideoSurface(String id, Object holder);

    public abstract void addPreviewVideoSurface(Object holder);

    public abstract void removeVideoSurface(String id);

    public abstract void removePreviewVideoSurface();

    public abstract void switchInput(String id, boolean front);

    public abstract void setPreviewSettings();

    public HardwareService() {
        mVideoCallback = new VideoCallbackHandler();
    }

    public VideoCallback getCallbackHandler() {
        return mVideoCallback;
    }

    public void connectivityChanged() {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "connectivityChange() thread running...");
                        Ringservice.connectivityChanged();
                        return true;
                    }
                }
        );
    }

    public void switchInput(final String id, final String uri, final StringMap map) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "switchInput() thread running..." + uri);
                        Ringservice.applySettings(id, map);
                        Ringservice.switchInput(id, uri);
                        return true;
                    }
                }
        );
    }

    public void setPreviewSettings(final Map<String, StringMap> cameraMaps) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "applySettings() thread running...");
                        for (Map.Entry<String, StringMap> entry : cameraMaps.entrySet()) {
                            Ringservice.applySettings(entry.getKey(), entry.getValue());
                        }
                        return true;
                    }
                }
        );
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
        RingserviceJNI.unregisterVideoCallback(inputId, inputWindow);
        RingserviceJNI.releaseNativeWindow(inputWindow);
    }

    public void setVideoFrame(final byte[] data, final int width, final int height, final int rotation) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        long frame = RingserviceJNI.obtainFrame(data.length);
                        if (frame != 0) {
                            RingserviceJNI.setVideoFrame(data, data.length, frame, width, height, rotation);
                        }
                        RingserviceJNI.releaseFrame(frame);
                        return true;
                    }
                }
        );
    }

    public void addVideoDevice(String deviceId) {
        Log.d(TAG, "addVideoDevice: " + deviceId);
        RingserviceJNI.addVideoDevice(deviceId);
    }

    public void setDefaultVideoDevice(String deviceId) {
        Log.d(TAG, "setDefaultVideoDevice: " + deviceId);
        RingserviceJNI.setDefaultDevice(deviceId);
    }

    private class VideoCallbackHandler extends VideoCallback {

        public VideoCallbackHandler() {
        }

        @Override
        public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
            HardwareService.this.decodingStarted(id, shmPath, width, height, isMixer);
        }

        @Override
        public void decodingStopped(String id, String shmPath, boolean isMixer) {
            HardwareService.this.decodingStopped(id, shmPath, isMixer);
        }

        @Override
        public void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates) {
            HardwareService.this.getCameraInfo(camId, formats, sizes, rates);
        }

        @Override
        public void setParameters(String camId, int format, int width, int height, int rate) {
            HardwareService.this.setParameters(camId, format, width, height, rate);
        }

        @Override
        public void startCapture(String camId) {
            Log.d(TAG, "startCapture: " + camId);
            HardwareService.this.startCapture(camId);
        }

        @Override
        public void stopCapture() {
            HardwareService.this.stopCapture();
        }
    }

}
