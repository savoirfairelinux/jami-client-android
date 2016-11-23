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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.RingserviceJNI;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.VideoCallback;
import cx.ring.utils.Log;
import cx.ring.utils.SwigNativeConverter;

public abstract class DaemonService {

    private static final String TAG = DaemonService.class.getName();

    private static final int POLLING_TIMEOUT = 50;

    @Inject
    ExecutorService mExecutor;

    @Inject
    ScheduledExecutorService mScheduledExecutor;

    private boolean mDaemonStarted = false;

    public abstract void loadNativeLibrary();

    public abstract File provideFilesDir();

    public abstract String provideDefaultVCardName();

    public DaemonService() {

    }

    public Callback getDaemonCallbackHandler(CallService.CallbackHandler callCallbackHandler,
                                             ConferenceService.ConferenceCallbackHandler confCallbackHandler) {
        DaemonCallback callbackHandler = new DaemonCallback();
        callbackHandler.setCallbackHandler(callCallbackHandler);
        callbackHandler.setConferenceCallbackHandler(confCallbackHandler);
        return callbackHandler;
    }

    public boolean isStarted() {
        return mDaemonStarted;
    }

    public void startDaemon(final Callback callManagerCallback,
                            final ConfigurationCallback configurationManagerCallback,
                            final VideoCallback videoManagerCallBack) {

        if (!mDaemonStarted) {
            Log.i(TAG, "Starting daemon ...");
            Ringservice.init(configurationManagerCallback, callManagerCallback, videoManagerCallBack);
            startRingServicePolling();
            mDaemonStarted = true;
            Log.i(TAG, "DaemonService started");
        }
    }

    private void startRingServicePolling() {
        mScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                mExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        Ringservice.pollEvents();
                    }
                });
            }
        }, 0, POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stopDaemon() {
        mScheduledExecutor.shutdown();

        if (mDaemonStarted) {
            Log.i(TAG, "stopping daemon ...");
            Ringservice.fini();
            mDaemonStarted = false;
            Log.i(TAG, "DaemonService stopped");
        }
    }

    public List<String> getTlsSupportedMethods() {
        Log.i(TAG, "getTlsSupportedMethods()");
        return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
    }

    public void connectivityChanged() {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Ringservice.connectivityChanged();
            }
        });
    }

    public void switchInput(final String id, final String uri, final StringMap map) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "switchInput() thread running..." + uri);
                Ringservice.applySettings(id, map);
                Ringservice.switchInput(id, uri);
            }
        });
    }

    public void setPreviewSettings(final Map<String, StringMap> cameraMaps) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "applySettings() thread running...");
                for (Map.Entry<String, StringMap> entry : cameraMaps.entrySet()) {
                    Ringservice.applySettings(entry.getKey(), entry.getValue());
                }
            }
        });
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

    class DaemonCallback extends Callback {

        private CallService.CallbackHandler mCallbackHandler;
        private ConferenceService.ConferenceCallbackHandler mConferenceCallbackHandler;

        void setCallbackHandler(CallService.CallbackHandler callbackHandler) {
            mCallbackHandler = callbackHandler;
        }

        void setConferenceCallbackHandler(ConferenceService.ConferenceCallbackHandler callbackHandler) {
            mConferenceCallbackHandler = callbackHandler;
        }

        @Override
        public void callStateChanged(String callId, String newState, int detailCode) {
            mCallbackHandler.callStateChanged(callId, newState, detailCode);
        }

        @Override
        public void incomingCall(String accountId, String callId, String from) {
            mCallbackHandler.incomingCall(accountId, callId, from);
        }

        @Override
        public void incomingMessage(String callId, String from, StringMap messages) {
            mCallbackHandler.incomingMessage(callId, from, messages);
        }

        @Override
        public void conferenceCreated(final String confId) {
            mConferenceCallbackHandler.conferenceCreated(confId);
        }

        @Override
        public void conferenceRemoved(String confId) {
            mConferenceCallbackHandler.conferenceRemoved(confId);
        }

        @Override
        public void conferenceChanged(String confId, String state) {
            mConferenceCallbackHandler.conferenceChanged(confId, state);
        }

        @Override
        public void recordPlaybackFilepath(String id, String filename) {
            mCallbackHandler.recordPlaybackFilepath(id, filename);
        }

        @Override
        public void onRtcpReportReceived(String callId, IntegerMap stats) {
            mCallbackHandler.onRtcpReportReceived(callId, stats);
        }

    }
}
