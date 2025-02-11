/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.services

import net.jami.daemon.*
import net.jami.model.Uri
import net.jami.utils.Log
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Named

class DaemonService(
    private val mSystemInfoCallbacks: SystemInfoCallbacks,
    @Named("DaemonExecutor") private val mExecutor: ScheduledExecutorService,
    private val mCallService: CallService,
    private val mHardwareService: HardwareService,
    private val mAccountService: AccountService,
    private val eventService: EventService,
) {
    // references must be kept to avoid garbage collection while pointers are stored in the daemon.
    private var mHardwareCallback: DaemonVideoCallback? = null
    private var mPresenceCallback: DaemonPresenceCallback? = null
    private var mCallAndConferenceCallback: DaemonCallAndConferenceCallback? = null
    private var mConfigurationCallback: DaemonConfigurationCallback? = null
    private var mDataCallback: DaemonDataTransferCallback? = null
    private var mConversationCallback: ConversationCallback? = null
    var isStarted = false
        private set

    interface SystemInfoCallbacks {
        fun getHardwareAudioFormat(ret: IntVect)
        fun getAppDataPath(name: String, ret: StringVect)
        fun getDeviceName(ret: StringVect)
    }

    @Synchronized
    fun startDaemon() {
        if (!isStarted) {
            isStarted = true
            Log.i(TAG, "Starting daemon ...")
            mHardwareCallback = DaemonVideoCallback()
            mPresenceCallback = DaemonPresenceCallback()
            mCallAndConferenceCallback = DaemonCallAndConferenceCallback()
            mConfigurationCallback = DaemonConfigurationCallback()
            mDataCallback = DaemonDataTransferCallback()
            mConversationCallback = ConversationCallbackImpl()
            JamiService.init(
                mConfigurationCallback,
                mCallAndConferenceCallback,
                mPresenceCallback,
                mDataCallback,
                mHardwareCallback,
                mConversationCallback
            )
            Log.i(TAG, "DaemonService started")
        }
    }

    @Synchronized
    fun stopDaemon() {
        mExecutor.shutdown()
        if (isStarted) {
            Log.i(TAG, "stopping daemon ...")
            JamiService.fini()
            isStarted = false
            Log.i(TAG, "DaemonService stopped")
        }
    }

    internal inner class DaemonConfigurationCallback : ConfigurationCallback() {
        override fun volumeChanged(device: String, value: Int) {
            mAccountService.volumeChanged(device, value)
        }

        override fun accountsChanged() {
            eventService.logEvent("accountsChanged")
            mExecutor.submit { mAccountService.accountsChanged() }
        }

        override fun stunStatusFailure(accountId: String) {
            mAccountService.stunStatusFailure(accountId)
        }

        override fun registrationStateChanged(accountId: String, newState: String, code: Int, detailString: String) {
            eventService.logEvent("registrationStateChanged", mapOf("accountId" to accountId, "newState" to newState, "code" to code.toString(), "detailString" to detailString))
            mExecutor.submit { mAccountService.registrationStateChanged(accountId, newState, code, detailString) }
        }

        override fun volatileAccountDetailsChanged(account_id: String, details: StringMap) {
            val jdetails: Map<String, String> = details.toNative()
            mExecutor.submit { mAccountService.volatileAccountDetailsChanged(account_id, jdetails) }
        }

        override fun accountDetailsChanged(account_id: String, details: StringMap) {
            val jdetails: Map<String, String> = details.toNative()
            mExecutor.submit { mAccountService.accountDetailsChanged(account_id, jdetails) }
        }

        override fun activeCallsChanged(
            accountId: String,
            conversationId: String,
            activeCalls: VectMap,
        ) {
            eventService.logEvent("activeCallsChanged", mapOf("accountId" to accountId, "conversationId" to conversationId, "activeCalls" to activeCalls.toString()))
            mAccountService.activeCallsChanged(accountId, conversationId, activeCalls.toNative())
        }

        override fun profileReceived(accountId: String, peerId: String, path: String) {
            eventService.logEvent("profileReceived", mapOf("accountId" to accountId, "peerId" to peerId, "path" to path))
            mExecutor.submit { mAccountService.profileReceived(accountId, peerId, path) }
        }

        override fun accountProfileReceived(account_id: String, name: String, photo: String) {
            eventService.logEvent("accountProfileReceived", mapOf("accountId" to account_id, "name" to name, "photo" to photo))
            mAccountService.accountProfileReceived(account_id, name, photo)
        }

        override fun incomingAccountMessage(accountId: String, from: String, messageId: String, messages: StringMap) {
            eventService.logEvent("incomingAccountMessage", mapOf("accountId" to accountId, "from" to from, "messageId" to messageId))
            if (messages.isEmpty()) return
            val jmessages: Map<String, String> = messages.toNativeFromUtf8()
            mExecutor.submit { mAccountService.incomingAccountMessage(accountId, messageId, null, from, jmessages) }
        }

        override fun accountMessageStatusChanged(accountId: String, conversationId: String, peer: String, messageId: String, status: Int) {
            mExecutor.submit {
                mAccountService.accountMessageStatusChanged(accountId, conversationId, messageId, peer, status)
            }
        }

        override fun composingStatusChanged(accountId: String, conversationId: String, contactUri: String, status: Int) {
            mExecutor.submit { mAccountService.composingStatusChanged(accountId, conversationId, contactUri, status) }
        }

        override fun errorAlert(alert: Int) {
            mExecutor.submit { mAccountService.errorAlert(alert) }
        }

        override fun getHardwareAudioFormat(ret: IntVect) {
            mSystemInfoCallbacks.getHardwareAudioFormat(ret)
        }

        override fun getAppDataPath(name: String, ret: StringVect) {
            mSystemInfoCallbacks.getAppDataPath(name, ret)
        }

        override fun getDeviceName(ret: StringVect) {
            mSystemInfoCallbacks.getDeviceName(ret)
        }

        override fun knownDevicesChanged(accountId: String, devices: StringMap) {
            eventService.logEvent("knownDevicesChanged", mapOf("accountId" to accountId, "devices" to devices.toString()))
            val jdevices: Map<String, String> = devices.toNativeFromUtf8()
            mExecutor.submit { mAccountService.knownDevicesChanged(accountId, jdevices) }
        }

        override fun exportOnRingEnded(accountId: String, code: Int, pin: String) {
            mAccountService.exportOnRingEnded(accountId, code, pin)
        }

        override fun nameRegistrationEnded(accountId: String, state: Int, name: String) {
            mAccountService.nameRegistrationEnded(accountId, state, name)
        }

        override fun registeredNameFound(accountId: String, state: Int, address: String, name: String) {
            mAccountService.registeredNameFound(accountId, state, address, name)
        }

        override fun userSearchEnded(accountId: String, state: Int, query: String, results: VectMap) {
            mAccountService.userSearchEnded(accountId, state, query, results.toNative())
        }

        override fun migrationEnded(accountId: String, state: String) {
            mAccountService.migrationEnded(accountId, state)
        }

        override fun deviceRevocationEnded(accountId: String, device: String, state: Int) {
            mAccountService.deviceRevocationEnded(accountId, device, state)
        }

        override fun incomingTrustRequest(accountId: String, conversationId: String, from: String, message: Blob, received: Long) {
            eventService.logEvent("incomingTrustRequest", mapOf("accountId" to accountId, "conversationId" to conversationId, "from" to from, "received" to received.toString()))
        }

        override fun contactAdded(accountId: String, uri: String, confirmed: Boolean) {
            eventService.logEvent("contactAdded", mapOf("accountId" to accountId, "uri" to uri, "confirmed" to confirmed.toString()))
            mExecutor.submit { mAccountService.contactAdded(accountId, uri, confirmed) }
        }

        override fun contactRemoved(accountId: String, uri: String, blocked: Boolean) {
            mExecutor.submit { mAccountService.contactRemoved(accountId, uri, blocked) }
        }

        override fun messageSend(message: String) {
            mHardwareService.logMessage(message)
        }
    }

    internal inner class DaemonCallAndConferenceCallback : Callback() {
        override fun callStateChanged(accountId: String, callId: String, newState: String, detailCode: Int) {
            eventService.logEvent("callStateChanged", mapOf("accountId" to accountId, "callId" to callId, "newState" to newState, "detailCode" to detailCode.toString()))
            mCallService.callStateChanged(accountId, callId, newState, detailCode)
        }

        override fun audioMuted(callId: String, muted: Boolean) {
            mCallService.audioMuted(callId, muted)
        }

        override fun videoMuted(callId: String, muted: Boolean) {
            mCallService.videoMuted(callId, muted)
        }

        override fun incomingCall(accountId: String, callId: String, from: String) {
            eventService.logEvent("incomingCall", mapOf("accountId" to accountId, "callId" to callId, "from" to from))
            // Should be kept while multi-stream is not enabled for Android by default
            mCallService.incomingCallWithMedia(accountId, callId, from, null)
        }

        override fun incomingCallWithMedia(accountId: String, callId: String, from: String, mediaList: VectMap) {
            eventService.logEvent("incomingCallWithMedia", mapOf("accountId" to accountId, "callId" to callId, "from" to from, "mediaList" to mediaList.toMap().toString()))
            mCallService.incomingCallWithMedia(accountId, callId, from, mediaList)
        }

        override fun mediaChangeRequested(accountId: String, callId: String, mediaList: VectMap) {
            eventService.logEvent("mediaChangeRequested", mapOf("accountId" to accountId, "callId" to callId, "mediaList" to mediaList.toMap().toString()))
            mCallService.mediaChangeRequested(accountId, callId, mediaList)
        }

        override fun mediaNegotiationStatus(callId: String, event: String, mediaList: VectMap) {
            eventService.logEvent("mediaNegotiationStatus", mapOf("callId" to callId, "event" to event, "mediaList" to mediaList.toMap().toString()))
            mCallService.mediaNegotiationStatus(callId, event, mediaList)
        }

        override fun connectionUpdate(id: String, state: Int) {
            eventService.logEvent("connectionUpdate", mapOf("id" to id, "state" to state.toString()))
            mCallService.connectionUpdate(id, state)
        }

        override fun remoteRecordingChanged(call_id: String, peer_number: String, state: Boolean) {
            mCallService.remoteRecordingChanged(call_id, Uri.fromString(peer_number), state)
        }

        override fun onConferenceInfosUpdated(confId: String, infos: VectMap) {
            mCallService.onConferenceInfoUpdated(confId, infos.toNative())
        }

        override fun incomingMessage(accountId: String, callId: String, from: String, messages: StringMap) {
            eventService.logEvent("incomingMessage", mapOf("accountId" to accountId, "callId" to callId, "from" to from))
            if (messages.isEmpty()) return
            val jmessages: Map<String, String> = messages.toNativeFromUtf8()
            mExecutor.submit { mCallService.incomingMessage(accountId, callId, from, jmessages) }
        }

        override fun conferenceCreated(accountId: String, conversationId: String, confId: String) {
            eventService.logEvent("conferenceCreated", mapOf("accountId" to accountId, "conversationId" to conversationId, "confId" to confId))
            mCallService.conferenceCreated(accountId, conversationId, confId)
        }

        override fun conferenceRemoved(accountId: String, confId: String) {
            eventService.logEvent("conferenceRemoved", mapOf("accountId" to accountId, "confId" to confId))
            mCallService.conferenceRemoved(accountId, confId)
        }

        override fun conferenceChanged(accountId: String, confId: String, state: String) {
            eventService.logEvent("conferenceChanged", mapOf("accountId" to accountId, "confId" to confId, "state" to state))
            mCallService.conferenceChanged(accountId, confId, state)
        }

        override fun recordPlaybackFilepath(id: String, filename: String) {
            mCallService.recordPlaybackFilepath(id, filename)
        }

        override fun onRtcpReportReceived(callId: String, stats: IntegerMap) {
            mCallService.onRtcpReportReceived(callId)
        }
    }

    internal inner class DaemonPresenceCallback : PresenceCallback() {
        override fun newServerSubscriptionRequest(remote: String) {
            eventService.logEvent("newServerSubscriptionRequest", mapOf("remote" to remote))
            Log.d(TAG, "newServerSubscriptionRequest: $remote")
        }

        override fun serverError(accountId: String, error: String, message: String) {
            eventService.logEvent("serverError", mapOf("accountId" to accountId, "error" to error, "message" to message))
            Log.d(TAG, "serverError: $accountId, $error, $message")
        }

        override fun newBuddyNotification(accountId: String, buddyUri: String, status: Int, lineStatus: String) {
            eventService.logEvent("newBuddyNotification", mapOf("accountId" to accountId, "buddyUri" to buddyUri, "status" to status.toString(), "lineStatus" to lineStatus))
            mAccountService.getAccount(accountId)?.presenceUpdate(buddyUri, status)
        }

        override fun subscriptionStateChanged(accountId: String, buddyUri: String, state: Int) {
            eventService.logEvent("subscriptionStateChanged", mapOf("accountId" to accountId, "buddyUri" to buddyUri, "state" to state.toString()))
            Log.d(TAG, "subscriptionStateChanged: $accountId, $buddyUri, $state")
        }
    }

    private inner class DaemonVideoCallback : VideoCallback() {
        override fun decodingStarted(id: String, shmPath: String, width: Int, height: Int, isMixer: Boolean) {
            eventService.logEvent("decodingStarted", mapOf("id" to id, "shmPath" to shmPath, "width" to width.toString(), "height" to height.toString(), "isMixer" to isMixer.toString()))
            mHardwareService.decodingStarted(id, shmPath, width, height, isMixer)
        }

        override fun decodingStopped(id: String, shmPath: String, isMixer: Boolean) {
            eventService.logEvent("decodingStopped", mapOf("id" to id, "shmPath" to shmPath, "isMixer" to isMixer.toString()))
            mHardwareService.decodingStopped(id, shmPath, isMixer)
        }

        override fun getCameraInfo(camId: String, formats: IntVect, sizes: UintVect, rates: UintVect) {
            mHardwareService.getCameraInfo(camId, formats, sizes, rates)
        }

        override fun setParameters(camId: String, format: Int, width: Int, height: Int, rate: Int) {
            mHardwareService.setParameters(camId, format, width, height, rate)
        }

        override fun requestKeyFrame(camId: String) {
            mHardwareService.requestKeyFrame(camId)
        }

        override fun setBitrate(camId: String, bitrate: Int) {
            mHardwareService.setBitrate(camId, bitrate)
        }

        override fun startCapture(camId: String) {
            mHardwareService.startCapture(camId)
        }

        override fun stopCapture(camId: String) {
            mHardwareService.stopCapture(camId)
        }
    }

    internal inner class DaemonDataTransferCallback : DataTransferCallback() {
        override fun dataTransferEvent(accountId: String, conversationId: String, interactionId: String, fileId: String, eventCode: Int) {
            Log.d(TAG, "dataTransferEvent: conversationId=$conversationId, fileId=$fileId, eventCode=$eventCode")
            mAccountService.dataTransferEvent(accountId, conversationId, interactionId, fileId, eventCode)
        }
    }

    internal inner class ConversationCallbackImpl : ConversationCallback() {
        override fun swarmLoaded(id: Long, accountId: String, conversationId: String, messages: SwarmMessageVect) {
            eventService.logEvent("swarmLoaded", mapOf("id" to id.toString(), "accountId" to accountId, "conversationId" to conversationId))
            mAccountService.swarmLoaded(id, accountId, conversationId, messages)
        }

        override fun messagesFound(id: Long, accountId: String, conversationId: String, messages: VectMap) {
            eventService.logEvent("messagesFound", mapOf("id" to id.toString(), "accountId" to accountId, "conversationId" to conversationId))
            mAccountService.messagesFound(id, accountId, conversationId, messages.toNative())
        }

        override fun conversationReady(accountId: String, conversationId: String) {
            eventService.logEvent("conversationReady", mapOf("accountId" to accountId, "conversationId" to conversationId))
            mAccountService.conversationReady(accountId, conversationId)
        }

        override fun conversationRemoved(accountId: String, conversationId: String) {
            eventService.logEvent("conversationRemoved", mapOf("accountId" to accountId, "conversationId" to conversationId))
            mAccountService.conversationRemoved(accountId, conversationId)
        }

        override fun conversationRequestReceived(accountId: String, conversationId: String, metadata: StringMap) {
            eventService.logEvent("conversationRequestReceived", mapOf("accountId" to accountId, "conversationId" to conversationId, "metadata" to metadata.toString()))
            mAccountService.conversationRequestReceived(accountId, conversationId, metadata.toNativeFromUtf8())
        }

        override fun conversationRequestDeclined(accountId: String, conversationId: String) {
            mAccountService.conversationRequestDeclined(accountId, conversationId)
        }

        override fun conversationMemberEvent(accountId: String, conversationId: String, uri: String, event: Int) {
            mAccountService.conversationMemberEvent(accountId, conversationId, uri, event)
        }

        override fun conversationProfileUpdated(accountId: String, conversationId: String, profile: StringMap) {
            mAccountService.conversationProfileUpdated(accountId, conversationId, profile)
        }

        override fun conversationPreferencesUpdated(accountId: String, conversationId: String, preferences: StringMap) {
            mAccountService.conversationPreferencesUpdated(accountId, conversationId, preferences)
        }

        override fun swarmMessageReceived(accountId: String, conversationId: String, message: SwarmMessage) {
            mAccountService.swarmMessageReceived(accountId, conversationId, message)
        }

        override fun swarmMessageUpdated(accountId: String, conversationId: String, message: SwarmMessage) {
            mAccountService.swarmMessageUpdated(accountId, conversationId, message)
        }

        override fun reactionAdded(accountId: String, conversationId: String, messageId: String, reaction: StringMap) {
            mAccountService.reactionAdded(accountId, conversationId, messageId, reaction)
        }

        override fun reactionRemoved(accountId: String, conversationId: String, messageId: String, reactionId: String) {
            mAccountService.reactionRemoved(accountId, conversationId, messageId, reactionId)
        }
    }

    companion object {
        private val TAG = DaemonService::class.simpleName!!
    }
}

fun VectMap.toMap() {
    toList().map { it.toMap() }.reduce { acc, map -> acc + map }
}