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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.SWIGTYPE_p_time_t;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.VideoCallback;
import cx.ring.utils.Log;

public class DaemonService {

    private static final String TAG = DaemonService.class.getName();

    private static final int POLLING_TIMEOUT = 50;

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    ScheduledExecutorService mScheduledExecutor;

    @Inject
    HistoryService mHistoryService;

    private boolean mDaemonStarted = false;

    public DaemonService() {
    }

    public Callback getDaemonCallbackHandler(CallService.CallbackHandler callCallbackHandler,
                                             ConferenceService.ConferenceCallbackHandler confCallbackHandler) {
        DaemonCallback callbackHandler = new DaemonCallback();
        callbackHandler.setCallbackHandler(callCallbackHandler);
        callbackHandler.setConferenceCallbackHandler(confCallbackHandler);
        return callbackHandler;
    }

    public ConfigurationCallback getDaemonConfigurationCallbackHandler(AccountService.ConfigurationCallbackHandler accountCallbackHandler,
                                                                       ContactService.ConfigurationCallbackHandler contactCallbackHandler) {
        DaemonConfigurationCallback callbackHandler = new DaemonConfigurationCallback();
        callbackHandler.setAccountCallbackHandler(accountCallbackHandler);
        callbackHandler.setContactCallbackHandler(contactCallbackHandler);
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

    class DaemonConfigurationCallback extends ConfigurationCallback {
        private AccountService.ConfigurationCallbackHandler mAccountCallbackHandler;
        private ContactService.ConfigurationCallbackHandler mContactCallbackHandler;

        void setAccountCallbackHandler(AccountService.ConfigurationCallbackHandler callbackHandler) {
            mAccountCallbackHandler = callbackHandler;
        }

        void setContactCallbackHandler(ContactService.ConfigurationCallbackHandler callbackHandler) {
            mContactCallbackHandler = callbackHandler;
        }

        @Override
        public void volumeChanged(String device, int value) {
            mAccountCallbackHandler.volumeChanged(device, value);
        }

        @Override
        public void accountsChanged() {
            mAccountCallbackHandler.accountsChanged();
        }

        @Override
        public void stunStatusFailure(String accountId) {
            mAccountCallbackHandler.stunStatusFailure(accountId);
        }

        @Override
        public void registrationStateChanged(String accountId, String newState, int code, String detailString) {
            mAccountCallbackHandler.registrationStateChanged(accountId, newState, code, detailString);
        }

        @Override
        public void incomingAccountMessage(String accountId, String from, StringMap messages) {
            mHistoryService.incomingMessage(accountId, null, from, messages);
        }

        @Override
        public void accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
            mAccountCallbackHandler.accountMessageStatusChanged(accountId, messageId, to, status);
        }

        @Override
        public void errorAlert(int alert) {
            mAccountCallbackHandler.errorAlert(alert);
        }

        @Override
        public void getHardwareAudioFormat(IntVect ret) {
            mAccountCallbackHandler.getHardwareAudioFormat(ret);
        }

        @Override
        public void getAppDataPath(String name, StringVect ret) {
            mAccountCallbackHandler.getAppDataPath(name, ret);
        }

        @Override
        public void knownDevicesChanged(String accountId, StringMap devices) {
            mAccountCallbackHandler.knownDevicesChanged(accountId, devices);
        }

        @Override
        public void exportOnRingEnded(String accountId, int code, String pin) {
            mAccountCallbackHandler.exportOnRingEnded(accountId, code, pin);
        }

        @Override
        public void nameRegistrationEnded(String accountId, int state, String name) {
            mAccountCallbackHandler.nameRegistrationEnded(accountId, state, name);
        }

        @Override
        public void registeredNameFound(String accountId, int state, String address, String name) {
            mAccountCallbackHandler.registeredNameFound(accountId, state, address, name);
        }

        @Override
        public void migrationEnded(String accountId, String state) {
            mAccountCallbackHandler.migrationEnded(accountId, state);
        }

        @Override
        public void incomingTrustRequest(String accountId, String from, Blob message, SWIGTYPE_p_time_t received) {
            mAccountCallbackHandler.incomingTrustRequest(accountId, from, message, received);
        }

        @Override
        public void contactAdded(String accountId, String uri, boolean confirmed) {
            mContactCallbackHandler.contactAdded(accountId, uri, confirmed);
        }

        @Override
        public void contactRemoved(String accountId, String uri, boolean banned) {
            mContactCallbackHandler.contactRemoved(accountId, uri, banned);
        }
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
            mHistoryService.incomingMessage(null, callId, from, messages);
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
