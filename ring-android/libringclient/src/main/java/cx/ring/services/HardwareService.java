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

public class HardwareService extends Observable {

    private static final String TAG = HardwareService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private VideoCallback mVideoCallback;

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

    public long startVideo(final String inputId, Object surface, int width, int height) {
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

    public void setVideoFrame(byte[] data, int width, int height, int rotation) {
        long frame = RingserviceJNI.obtainFrame(data.length);
        if (frame != 0) {
            RingserviceJNI.setVideoFrame(data, data.length, frame, width, height, rotation);
        }
        RingserviceJNI.releaseFrame(frame);
    }

    public void addVideoDevice(String deviceId) {
        RingserviceJNI.addVideoDevice(deviceId);
    }

    public void setDefaultVideoDevice(String deviceId) {
        RingserviceJNI.setDefaultDevice(deviceId);
    }

    private class VideoCallbackHandler extends VideoCallback {

        public VideoCallbackHandler() {
        }

        @Override
        public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
            Log.d(TAG, "decodingStarted: " + id + ", " + shmPath + ", " + width + ", " + height + ", " + isMixer);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DECODING_STARTED);
            event.addEventInput(ServiceEvent.EventInput.ID, id);
            event.addEventInput(ServiceEvent.EventInput.PATHS, shmPath);
            event.addEventInput(ServiceEvent.EventInput.WIDTH, width);
            event.addEventInput(ServiceEvent.EventInput.HEIGHT, height);
            event.addEventInput(ServiceEvent.EventInput.IS_MIXER, isMixer);
            notifyObservers(event);
        }

        @Override
        public void decodingStopped(String id, String shmPath, boolean isMixer) {
            Log.d(TAG, "decodingStopped: " + id + ", " + shmPath + ", " + isMixer);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.DECODING_STOPPED);
            event.addEventInput(ServiceEvent.EventInput.ID, id);
            event.addEventInput(ServiceEvent.EventInput.PATHS, shmPath);
            event.addEventInput(ServiceEvent.EventInput.IS_MIXER, isMixer);
            notifyObservers(event);
        }

        @Override
        public void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates) {
            Log.d(TAG, "getCameraInfo: " + camId + ", " + formats + ", " + sizes + ", " + rates);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.GET_CAMERA_INFO);
            event.addEventInput(ServiceEvent.EventInput.CAMERA_ID, camId);
            event.addEventInput(ServiceEvent.EventInput.FORMATS, formats);
            event.addEventInput(ServiceEvent.EventInput.SIZES, sizes);
            event.addEventInput(ServiceEvent.EventInput.RATES, rates);
            notifyObservers(event);
        }

        @Override
        public void setParameters(String camId, int format, int width, int height, int rate) {
            Log.d(TAG, "setParameters: " + camId + ", " + format + ", " + width + ", " + height + ", " + rate);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.SET_PARAMETERS);
            event.addEventInput(ServiceEvent.EventInput.CAMERA_ID, camId);
            event.addEventInput(ServiceEvent.EventInput.FORMATS, format);
            event.addEventInput(ServiceEvent.EventInput.WIDTH, width);
            event.addEventInput(ServiceEvent.EventInput.HEIGHT, height);
            event.addEventInput(ServiceEvent.EventInput.RATES, rate);
            notifyObservers(event);
        }

        @Override
        public void startCapture(String camId) {
            Log.d(TAG, "startCapture: " + camId);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.START_CAPTURE);
            event.addEventInput(ServiceEvent.EventInput.CAMERA_ID, camId);
            notifyObservers(event);
        }

        @Override
        public void stopCapture() {
            Log.d(TAG, "stopCapture");
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.STOP_CAPTURE);
            notifyObservers(event);
        }
    }

}
