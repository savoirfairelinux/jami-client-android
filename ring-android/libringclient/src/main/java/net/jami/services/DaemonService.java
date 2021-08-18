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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.services;

import net.jami.daemon.Blob;
import net.jami.daemon.Callback;
import net.jami.daemon.ConfigurationCallback;
import net.jami.daemon.ConversationCallback;
import net.jami.daemon.DataTransferCallback;
import net.jami.daemon.IntVect;
import net.jami.daemon.IntegerMap;
import net.jami.daemon.JamiService;
import net.jami.daemon.PresenceCallback;
import net.jami.daemon.StringMap;
import net.jami.daemon.StringVect;
import net.jami.daemon.UintVect;
import net.jami.daemon.VectMap;
import net.jami.daemon.VideoCallback;
import net.jami.model.Uri;
import net.jami.utils.Log;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

public class DaemonService {

    private static final String TAG = DaemonService.class.getSimpleName();

    @Named("DaemonExecutor")
    private final ScheduledExecutorService mExecutor;
    private final CallService mCallService;
    private final HardwareService mHardwareService;
    private final AccountService mAccountService;
    private final SystemInfoCallbacks mSystemInfoCallbacks;

    // references must be kept to avoid garbage collection while pointers are stored in the daemon.
    private DaemonVideoCallback mHardwareCallback;
    private DaemonPresenceCallback mPresenceCallback;
    private DaemonCallAndConferenceCallback mCallAndConferenceCallback;
    private DaemonConfigurationCallback mConfigurationCallback;
    private DaemonDataTransferCallback mDataCallback;
    private ConversationCallback mConversationCallback;

    private boolean mDaemonStarted = false;

    public DaemonService(SystemInfoCallbacks systemInfoCallbacks, ScheduledExecutorService executor, CallService callService, HardwareService hardwareService, AccountService accountService) {
        mSystemInfoCallbacks = systemInfoCallbacks;
        mExecutor = executor;
        mCallService = callService;
        mHardwareService = hardwareService;
        mAccountService = accountService;
    }

    public interface SystemInfoCallbacks {
        void getHardwareAudioFormat(IntVect ret);

        void getAppDataPath(String name, StringVect ret);

        void getDeviceName(StringVect ret);
    }

    public boolean isStarted() {
        return mDaemonStarted;
    }

    synchronized public void startDaemon() {
        if (!mDaemonStarted) {
            mDaemonStarted = true;
            Log.i(TAG, "Starting daemon ...");
            mHardwareCallback = new DaemonVideoCallback();
            mPresenceCallback = new DaemonPresenceCallback();
            mCallAndConferenceCallback = new DaemonCallAndConferenceCallback();
            mConfigurationCallback = new DaemonConfigurationCallback();
            mDataCallback = new DaemonDataTransferCallback();
            mConversationCallback = new ConversationCallbackImpl();
            JamiService.init(mConfigurationCallback, mCallAndConferenceCallback, mPresenceCallback, mDataCallback, mHardwareCallback, mConversationCallback);
            Log.i(TAG, "DaemonService started");
        }
    }

    synchronized public void stopDaemon() {
        mExecutor.shutdown();
        if (mDaemonStarted) {
            Log.i(TAG, "stopping daemon ...");
            JamiService.fini();
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
            mExecutor.submit(() -> mAccountService.accountsChanged());
        }

        @Override
        public void stunStatusFailure(String accountId) {
            mAccountService.stunStatusFailure(accountId);
        }

        @Override
        public void registrationStateChanged(String accountId, String newState, int code, String detailString) {
            mExecutor.submit(() -> mAccountService.registrationStateChanged(accountId, newState, code, detailString));
        }

        @Override
        public void volatileAccountDetailsChanged(String account_id, StringMap details) {
            Map<String, String> jdetails = details.toNative();
            mExecutor.submit(() -> mAccountService.volatileAccountDetailsChanged(account_id, jdetails));
        }

        @Override
        public void accountDetailsChanged(String account_id, StringMap details) {
            Map<String, String> jdetails = details.toNative();
            mExecutor.submit(() -> mAccountService.accountDetailsChanged(account_id, jdetails));
        }

        @Override
        public void profileReceived(String accountId, String peerId, String path) {
            mExecutor.submit(() -> mAccountService.profileReceived(accountId, peerId, path));
        }

        @Override
        public void accountProfileReceived(String account_id, String name, String photo) {
            mAccountService.accountProfileReceived(account_id, name, photo);
        }

        @Override
        public void incomingAccountMessage(String accountId, String from, String messageId, StringMap messages) {
            if (messages == null || messages.isEmpty())
                return;
            Map<String, String> jmessages = messages.toNativeFromUtf8();
            mExecutor.submit(() -> mAccountService.incomingAccountMessage(accountId, messageId, null, from, jmessages));
        }

        @Override
        public void accountMessageStatusChanged(String accountId, String conversationId, String peer, String messageId, int status) {
            mExecutor.submit(() -> mAccountService.accountMessageStatusChanged(accountId, conversationId, messageId, peer, status));
        }

        @Override
        public void composingStatusChanged(String accountId, String conversationId, String contactUri, int status) {
            mExecutor.submit(() -> mAccountService.composingStatusChanged(accountId, conversationId, contactUri, status));
        }

        @Override
        public void errorAlert(int alert) {
            mExecutor.submit(() -> mAccountService.errorAlert(alert));
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
            Map<String, String> jdevices = devices.toNativeFromUtf8();
            mExecutor.submit(() -> mAccountService.knownDevicesChanged(accountId, jdevices));
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
        public void userSearchEnded(String accountId, int state, String query, VectMap results) {
            mAccountService.userSearchEnded(accountId, state, query, results.toNative());
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
        public void incomingTrustRequest(String accountId, String conversationId, String from, Blob message, long received) {
            String jmessage = message.toJavaString();
            mExecutor.submit(() -> mAccountService.incomingTrustRequest(accountId, conversationId, from, jmessage, received));
        }

        @Override
        public void contactAdded(String accountId, String uri, boolean confirmed) {
            mExecutor.submit(() -> mAccountService.contactAdded(accountId, uri, confirmed));
        }

        @Override
        public void contactRemoved(String accountId, String uri, boolean banned) {
            mExecutor.submit(() -> mAccountService.contactRemoved(accountId, uri, banned));
        }

        @Override
        public void messageSend(String message) {
            mHardwareService.logMessage(message);
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
        public void connectionUpdate(String id, int state) {
            mCallService.connectionUpdate(id, state);
        }

        @Override
        public void remoteRecordingChanged(String call_id, String peer_number, boolean state) {
            mCallService.remoteRecordingChanged(call_id, Uri.fromString(peer_number), state);
        }

        @Override
        public void onConferenceInfosUpdated(String confId, VectMap infos) {
            mCallService.onConferenceInfoUpdated(confId, infos.toNative());
        }

        @Override
        public void incomingMessage(String callId, String from, StringMap messages) {
            if (messages == null || messages.isEmpty())
                return;
            Map<String, String> jmessages = messages.toNativeFromUtf8();
            mExecutor.submit(() -> mCallService.incomingMessage(callId, from, jmessages));
        }

        @Override
        public void conferenceCreated(final String confId) {
            mCallService.conferenceCreated(confId);
        }

        @Override
        public void conferenceRemoved(String confId) {
            mCallService.conferenceRemoved(confId);
        }

        @Override
        public void conferenceChanged(String confId, String state) {
            mCallService.conferenceChanged(confId, state);
        }

        @Override
        public void recordPlaybackFilepath(String id, String filename) {
            mCallService.recordPlaybackFilepath(id, filename);
        }

        @Override
        public void onRtcpReportReceived(String callId, IntegerMap stats) {
            mCallService.onRtcpReportReceived(callId);
        }
    }

    class DaemonPresenceCallback extends PresenceCallback {

        @Override
        public void newServerSubscriptionRequest(String remote) {
            Log.d(TAG, "newServerSubscriptionRequest: " + remote);
        }

        @Override
        public void serverError(String accountId, String error, String message) {
            Log.d(TAG, "serverError: " + accountId + ", " + error + ", " + message);
        }

        @Override
        public void newBuddyNotification(String accountId, String buddyUri, int status, String lineStatus) {
            mAccountService.getAccount(accountId).presenceUpdate(buddyUri, status == 1);
        }

        @Override
        public void subscriptionStateChanged(String accountId, String buddyUri, int state) {
            Log.d(TAG, "subscriptionStateChanged: " + accountId + ", " + buddyUri + ", " + state);
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
        public void requestKeyFrame() {
            mHardwareService.requestKeyFrame();
        }

        @Override
        public void setBitrate(String device, int bitrate) {
            mHardwareService.setBitrate(device, bitrate);
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
        public void dataTransferEvent(String accountId, String conversationId, String interactionId, String fileId, int eventCode) {
            Log.d(TAG, "dataTransferEvent: conversationId=" + conversationId + ", fileId=" + fileId + ", eventCode=" + eventCode);
            mAccountService.dataTransferEvent(accountId, conversationId, interactionId, fileId, eventCode);
        }
    }

    class ConversationCallbackImpl extends ConversationCallback {
        @Override
        public void conversationLoaded(long id, String accountId, String conversationId, VectMap messages) {
            mAccountService.conversationLoaded(accountId, conversationId, messages.toNative());
        }

        @Override
        public void conversationReady(String accountId, String conversationId) {
            mAccountService.conversationReady(accountId, conversationId);
        }

        @Override
        public void conversationRemoved(String accountId, String conversationId) {
            mAccountService.conversationRemoved(accountId, conversationId);
        }

        @Override
        public void conversationRequestReceived(String accountId, String conversationId, StringMap metadata) {
            mAccountService.conversationRequestReceived(accountId, conversationId, metadata.toNative());
        }

        @Override
        public void conversationRequestDeclined(String accountId, String conversationId) {
            mAccountService.conversationRequestDeclined(accountId, conversationId);
        }
        @Override
        public void conversationMemberEvent(String accountId, String conversationId, String uri, int event) {
            mAccountService.conversationMemberEvent(accountId, conversationId, uri, event);
        }

        @Override
        public void messageReceived(String accountId, String conversationId, StringMap message) {
            mAccountService.messageReceived(accountId, conversationId, message.toNative());
        }
    }

}
