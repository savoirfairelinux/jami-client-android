/*
 *  Copyright (C) 2018 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import cx.ring.daemon.DataTransferCallback;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.IntegerMap;
import cx.ring.daemon.PresenceCallback;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.UintVect;
import cx.ring.daemon.VideoCallback;
import cx.ring.utils.Log;

public class DaemonService {

    private static final String TAG = DaemonService.class.getSimpleName();

    private static final int POLLING_TIMEOUT = 50;

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    ScheduledExecutorService mScheduledExecutor;

    @Inject
    HistoryService mHistoryService;

    @Inject
    protected CallService mCallService;

    @Inject
    protected ConferenceService mConferenceService;

    @Inject
    protected HardwareService mHardwareService;

    @Inject
    protected PresenceService mPresenceService;

    @Inject
    protected AccountService mAccountService;

    private final SystemInfoCallbacks mSystemInfoCallbacks;
    private DaemonVideoCallback mHardwareCallback;
    private DaemonPresenceCallback mPresenceCallback;
    private DaemonCallAndConferenceCallback mCallAndConferenceCallback;
    private DaemonConfigurationCallback mConfigurationCallback;
    private DaemonDataTransferCallback mDataCallback;
    private boolean mDaemonStarted = false;

    public DaemonService(SystemInfoCallbacks systemInfoCallbacks) {
        mSystemInfoCallbacks = systemInfoCallbacks;
    }

    public interface SystemInfoCallbacks {
        void getHardwareAudioFormat(IntVect ret);

        void getAppDataPath(String name, StringVect ret);

        void getDeviceName(StringVect ret);
    }

    public boolean isStarted() {
        return mDaemonStarted;
    }

    public void startDaemon() {
        if (!mDaemonStarted) {
            Log.i(TAG, "Starting daemon ...");
            mHardwareCallback = new DaemonVideoCallback();
            mPresenceCallback = new DaemonPresenceCallback();
            mCallAndConferenceCallback = new DaemonCallAndConferenceCallback();
            mConfigurationCallback = new DaemonConfigurationCallback();
            mDataCallback = new DaemonDataTransferCallback();
            Ringservice.init(mConfigurationCallback, mCallAndConferenceCallback, mPresenceCallback, mDataCallback, mHardwareCallback);
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

        @Override
        public void volumeChanged(String device, int value) {
            mAccountService.volumeChanged(device, value);
        }

        @Override
        public void accountsChanged() {
            mAccountService.accountsChanged();
        }

        @Override
        public void stunStatusFailure(String accountId) {
            mAccountService.stunStatusFailure(accountId);
        }

        @Override
        public void registrationStateChanged(String accountId, String newState, int code, String detailString) {
            mAccountService.registrationStateChanged(accountId, newState, code, detailString);
        }

        @Override
        public void volatileAccountDetailsChanged(String account_id, StringMap details) {
            mAccountService.volatileAccountDetailsChanged(account_id, details);
        }

        @Override
        public void incomingAccountMessage(String accountId, String from, StringMap messages) {
            mHistoryService.incomingMessage(accountId, null, from, messages);
        }

        @Override
        public void accountMessageStatusChanged(String accountId, long messageId, String to, int status) {
            mHistoryService.accountMessageStatusChanged(accountId, messageId, to, status);
        }

        @Override
        public void errorAlert(int alert) {
            mAccountService.errorAlert(alert);
        }

        @Override
        public void getHardwareAudioFormat(IntVect ret) {
            mSystemInfoCallbacks.getHardwareAudioFormat(ret);
        }

        @Override
        public void getAppDataPath(String name, StringVect ret) {
            mSystemInfoCallbacks.getAppDataPath(name, ret);
        }

        @Override
        public void getDeviceName(StringVect ret) {
            mSystemInfoCallbacks.getDeviceName(ret);
        }

        @Override
        public void knownDevicesChanged(String accountId, StringMap devices) {
            mAccountService.knownDevicesChanged(accountId, devices);
        }

        @Override
        public void exportOnRingEnded(String accountId, int code, String pin) {
            mAccountService.exportOnRingEnded(accountId, code, pin);
        }

        @Override
        public void nameRegistrationEnded(String accountId, int state, String name) {
            mAccountService.nameRegistrationEnded(accountId, state, name);
        }

        @Override
        public void registeredNameFound(String accountId, int state, String address, String name) {
            mAccountService.registeredNameFound(accountId, state, address, name);
        }

        @Override
        public void migrationEnded(String accountId, String state) {
            mAccountService.migrationEnded(accountId, state);
        }

        @Override
        public void deviceRevocationEnded(String accountId, String device, int state) {
            mAccountService.deviceRevocationEnded(accountId, device, state);
        }

        @Override
        public void incomingTrustRequest(String accountId, String from, Blob message, long received) {
            mAccountService.incomingTrustRequest(accountId, from, message, received);
        }

        @Override
        public void contactAdded(String accountId, String uri, boolean confirmed) {
            mAccountService.contactAdded(accountId, uri, confirmed);
        }

        @Override
        public void contactRemoved(String accountId, String uri, boolean banned) {
            mAccountService.contactRemoved(accountId, uri, banned);
        }
    }

    class DaemonCallAndConferenceCallback extends Callback {

        @Override
        public void callStateChanged(String callId, String newState, int detailCode) {
            mCallService.callStateChanged(callId, newState, detailCode);
        }

        @Override
        public void incomingCall(String accountId, String callId, String from) {
            mCallService.incomingCall(accountId, callId, from);
        }

        @Override
        public void incomingMessage(String callId, String from, StringMap messages) {
            mCallService.incomingMessage(callId, from, messages);
        }

        @Override
        public void conferenceCreated(final String confId) {
            mConferenceService.conferenceCreated(confId);
        }

        @Override
        public void conferenceRemoved(String confId) {
            mConferenceService.conferenceRemoved(confId);
        }

        @Override
        public void conferenceChanged(String confId, String state) {
            mConferenceService.conferenceChanged(confId, state);
        }

        @Override
        public void recordPlaybackFilepath(String id, String filename) {
            mCallService.recordPlaybackFilepath(id, filename);
        }

        @Override
        public void onRtcpReportReceived(String callId, IntegerMap stats) {
            mCallService.onRtcpReportReceived(callId, stats);
        }

    }

    class DaemonPresenceCallback extends PresenceCallback {

        @Override
        public void newServerSubscriptionRequest(String remote) {
            mPresenceService.newServerSubscriptionRequest(remote);
        }

        @Override
        public void serverError(String accountId, String error, String message) {
            mPresenceService.serverError(accountId, error, message);
        }

        @Override
        public void newBuddyNotification(String accountId, String buddyUri, int status, String lineStatus) {
            mPresenceService.newBuddyNotification(accountId, buddyUri, status, lineStatus);
        }

        @Override
        public void subscriptionStateChanged(String accountId, String buddyUri, int state) {
            mPresenceService.subscriptionStateChanged(accountId, buddyUri, state);
        }
    }

    private class DaemonVideoCallback extends VideoCallback {

        @Override
        public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
            mHardwareService.decodingStarted(id, shmPath, width, height, isMixer);
        }

        @Override
        public void decodingStopped(String id, String shmPath, boolean isMixer) {
            mHardwareService.decodingStopped(id, shmPath, isMixer);
        }

        @Override
        public void getCameraInfo(String camId, IntVect formats, UintVect sizes, UintVect rates) {
            mHardwareService.getCameraInfo(camId, formats, sizes, rates);
        }

        @Override
        public void setParameters(String camId, int format, int width, int height, int rate) {
            mHardwareService.setParameters(camId, format, width, height, rate);
        }

        @Override
        public void startCapture(String camId) {
            Log.d(TAG, "startCapture: " + camId);
            mHardwareService.startCapture(camId);
        }

        @Override
        public void stopCapture() {
            mHardwareService.stopCapture();
        }
    }

    class DaemonDataTransferCallback extends DataTransferCallback {

        @Override
        public void dataTransferEvent(long transferId, int eventCode) {
            Log.d(TAG, "dataTransferEvent: transferId=" + transferId + ", eventCode=" + eventCode);
            mCallService.dataTransferEvent(transferId, eventCode);
        }
    }
}
