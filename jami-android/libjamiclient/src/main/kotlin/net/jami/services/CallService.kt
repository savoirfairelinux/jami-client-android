/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.VectMap
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Call.Direction
import net.jami.model.Conference
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Media
import net.jami.model.Uri
import net.jami.utils.Log
import java.util.*
import java.util.concurrent.ScheduledExecutorService

abstract class CallService(
    private val mExecutor: ScheduledExecutorService,
    val mContactService: ContactService,
    val mAccountService: AccountService,
    val mDeviceRuntimeService: DeviceRuntimeService
) {
    private val calls: MutableMap<String, Call> = HashMap()
    private val conferences: MutableMap<String, Conference> = HashMap()
    private val callSubject = PublishSubject.create<Call>()
    private val conferenceSubject = PublishSubject.create<Conference>()

    val confsUpdates: Observable<Conference>
        get() = conferenceSubject

    val callsUpdates: Observable<Call>
        get() = callSubject

    fun currentConferences(): List<Conference> =
        synchronized(calls) {
            conferences.values.filter { it.state == CallStatus.CURRENT }
        }

    private fun getConfCallUpdates(conf: Conference): Observable<Conference> =
        conferenceSubject
            .filter { c -> c == conf }
            .startWithItem(conf)
            .map(Conference::participants)
            .switchMap { list: List<Call> -> Observable.fromIterable(list)
                    .flatMap { call: Call -> callSubject.filter { c -> c == call } } }
            .map { conf }
            .startWithItem(conf)

    fun getConfUpdates(confId: String): Observable<Conference> =
        calls[confId]?.let { getConfUpdates(it) }
            ?: conferences[confId]?.let { getConfUpdates(it) } ?: Observable.error(IllegalArgumentException())

    /*public Observable<Boolean> getConnectionUpdates() {
        return connectionSubject
                .map(i -> i > 0)
                .distinctUntilChanged();
    }*/
    private fun updateConnectionCount() {
        //connectionSubject.onNext(currentConnections.size() - 2*currentCalls.size());
    }

    fun setIsComposing(accountId: String?, uri: String?, isComposing: Boolean) {
        mExecutor.execute { JamiService.setIsComposing(accountId, uri, isComposing) }
    }

    fun onConferenceInfoUpdated(confId: String, info: List<Map<String, String>>) {
        Log.w(TAG, "onConferenceInfoUpdated $confId $info")

        val conference = getConference(confId)
        var isModerator = false
        if (conference != null) {
            val newInfo: MutableList<ParticipantInfo> = ArrayList(info.size)
            val account = mAccountService.getAccount(conference.accountId) ?: return

            for (i in info) {
                val uri = i["uri"]!!
                val confInfo = if (uri.isEmpty()) {
                    ParticipantInfo(null, mContactService.getLoadedContact(account.accountId, account.getContactFromCache(Uri.fromId(account.username!!))).blockingGet(), i)
                } else {
                    val contactUri = Uri.fromString(uri)
                    val call = conference.findCallByContact(contactUri)
                    if (call != null) {
                        ParticipantInfo(call, mContactService.getLoadedContact(call.account, call.contact!!).blockingGet(), i)
                    } else {
                        ParticipantInfo(null, mContactService.getLoadedContact(account.accountId, account.getContactFromCache(contactUri)).blockingGet(), i)
                    }
                }
                if (confInfo.isEmpty) {
                    Log.w(TAG, "onConferenceInfoUpdated: ignoring empty entry $i")
                    continue
                }
                if (confInfo.contact.contact.isUser && confInfo.isModerator) {
                    isModerator = true
                }
                newInfo.add(confInfo)
            }

            conference.isModerator = isModerator
            conference.setInfo(newInfo)
        } else {
            Log.w(TAG, "onConferenceInfoUpdated can't find conference $confId")
        }
    }

    fun setConfMaximizedParticipant(accountId: String, confId: String, uri: Uri) {
        mExecutor.execute {
            JamiService.setActiveParticipant(accountId, confId, uri.rawRingId)
            JamiService.setConferenceLayout(accountId, confId, 1)
        }
    }

    fun setConfGridLayout(accountId: String, confId: String) {
        mExecutor.execute { JamiService.setConferenceLayout(accountId, confId, 0) }
    }

    fun remoteRecordingChanged(callId: String, peerNumber: Uri, state: Boolean) {
        Log.w(TAG, "remoteRecordingChanged $callId $peerNumber $state")
        var conference = getConference(callId)
        val call: Call?
        if (conference == null) {
            call = calls[callId]
            if (call != null) {
                conference = getConference(call)
            }
        } else {
            call = conference.firstCall
        }
        val account = if (call == null) null else mAccountService.getAccount(call.account)
        val contact = account?.getContactFromCache(peerNumber)
        if (conference != null && contact != null) {
            conference.setParticipantRecording(contact, state)
        }
    }

    private class ConferenceEntity(var conference: Conference)

    private fun getConferenceHost(call: Call): Conference =
        if (call.conversationUri == null) {
            getConference(call)
        } else {
            val conference = conferences.values.firstOrNull { it.conversationId == call.conversationUri.rawRingId } ?: throw IllegalArgumentException()
            Log.w(TAG, "getConferenceHost ${call.conversationUri} confId:${conference.id}")
            call.confId = conference.id
            conference.apply {
                hostCall = call
            }
        }

    fun getConfUpdates(call: Call): Observable<Conference> = if (call.id == null && call.conversationUri != null)
        getConfUpdates(getConferenceHost(call))
        else getConfUpdates(getConference(call))

    private fun getConfUpdates(conference: Conference): Observable<Conference> {
        Log.w(TAG, "getConfUpdates ${conference.id}")
        val conferenceEntity = ConferenceEntity(conference)
        return conferenceSubject
            .startWithItem(conference)
            .filter { conf: Conference ->
                Log.w(TAG, "getConfUpdates filter " + conf.id + " " + conf.participants.size + " (tracked " + conferenceEntity.conference.id + " " + conferenceEntity.conference.participants.size + ")")
                if (conf == conferenceEntity.conference) {
                    return@filter true
                }
                if (conf.contains(conferenceEntity.conference.id)) {
                    Log.w(TAG, "Switching tracked conference (up) to " + conf.id)
                    conferenceEntity.conference = conf
                    return@filter true
                }
                if (conferenceEntity.conference.participants.size == 1 && conf.participants.size == 1 && conferenceEntity.conference.call == conf.call && conf.call!!.id == conf.id) {
                    Log.w(TAG, "Switching tracked conference (down) to " + conf.id)
                    conferenceEntity.conference = conf
                    return@filter true
                }
                false
            }
            .switchMap { conf: Conference -> getConfCallUpdates(conf) }
    }

    private fun getCallUpdates(call: Call): Observable<Call> =
        callSubject.filter { c: Call -> c == call }
            .startWithItem(call)
            .takeWhile { c: Call -> c.callStatus !== CallStatus.OVER }


    /** Use a system API, if available, to request to start a call. */
    open class SystemCall(val allowed: Boolean) {
        open fun setCall(call: Call?) {
            call?.setSystemConnection(null)
        }
    }

    open fun requestPlaceCall(
        accountId: String,
        conversationUri: Uri?,
        contactUri: Uri,
        hasVideo: Boolean
    ): Single<SystemCall> = CALL_ALLOWED

    open fun requestIncomingCall(
        call: Call
    ): Single<SystemCall> = CALL_ALLOWED

    fun placeCallObservable(accountId: String, conversationUri: Uri?, number: Uri, hasVideo: Boolean): Observable<Call> =
        placeCall(accountId, conversationUri, number, hasVideo)
            .flatMapObservable { call: Call -> getCallUpdates(call) }

    fun placeCallIfAllowed(
        account: String, conversationUri: Uri?, numberUri: Uri, hasVideo: Boolean
    ): Single<Call> =
        // Need to check if the call is allowed by system before placing it.
        requestPlaceCall(account, conversationUri, numberUri, hasVideo)
            .onErrorReturnItem(CALL_ALLOWED_VAL) // Fallback to not block the call.
            .flatMap { result: SystemCall ->
                if (!result.allowed)
                    Single.error(SecurityException())
                else
                    placeCall(account, conversationUri, numberUri, hasVideo)
                        .doOnSuccess { result.setCall(it) }
                        .doOnError { result.setCall(null) }
            }

    private fun placeCall(
        account: String, conversationUri: Uri?, numberUri: Uri, hasVideo: Boolean
    ): Single<Call> =
        Single.fromCallable<Call> {
            Log.i(TAG, "placeCall() account=$account conversationUri=$conversationUri numberUri=$numberUri hasVideo=$hasVideo")

            // Create a media list with audio and video (optional).
            val mediaList =
                if (hasVideo) listOf(Media.DEFAULT_AUDIO, Media.DEFAULT_VIDEO)
                else listOf(Media.DEFAULT_AUDIO)
            val mediaMap = VectMap().apply {
                mediaList.let {
                    reserve(it.size)
                    it.map { media -> this.add(media.toMap()) }
                }
            }

            val callId = JamiService.placeCallWithMedia(account, numberUri.uri, mediaMap)

            if (callId.isEmpty()) {
                if (numberUri.isSwarm && conversationUri?.isSwarm == true) {
                    return@fromCallable Call(account, null, numberUri, false, conversationUri)
                } else {
                    throw IllegalStateException("Call ID is empty")
                }
            }

            // Add the call to the list.
            addCall(account, callId, numberUri, Direction.OUTGOING, mediaList, if (conversationUri?.isSwarm == true) conversationUri else null)
        }.subscribeOn(Schedulers.from(mExecutor))

    fun refuse(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "refuse() running… $callId")
            JamiService.refuse(accountId, callId)
            JamiService.hangUp(accountId, callId)
        }
    }

    fun accept(accountId:String, callId: String, hasVideo: Boolean = false) {
        mExecutor.execute {
            Log.i(TAG, "accept() running… $callId")
            val call = calls[callId] ?: return@execute
            val mediaList = call.mediaList
            val vectMapMedia = mediaList.mapTo(VectMap().apply { reserve(mediaList.size) }) { media ->
                if (!hasVideo && media.mediaType == Media.MediaType.MEDIA_TYPE_VIDEO)
                    media.copy(isMuted = true).toMap()
                else
                    media.toMap()
            }

            JamiService.acceptWithMedia(accountId, callId, vectMapMedia)
        }
    }

    fun hangUp(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "hangUp() running… $callId")
            JamiService.hangUp(accountId, callId)
        }
    }

    fun muteStream(
        accountId: String, confId: String, peerId: String, deviceId: String, mute: Boolean
    ) {
        mExecutor.execute {
            Log.i(TAG, "mute stream… peerId:$peerId deviceId:$deviceId")
            JamiService.muteStream(accountId, confId, peerId, deviceId, "", mute)
        }
    }

    fun hangupParticipant(accountId:String, confId: String, peerId: String, deviceId: String = "") {
        mExecutor.execute {
            Log.i(TAG, "hangup participant… $peerId")
            JamiService.hangupParticipant(accountId, confId, peerId, deviceId)
        }
    }

    fun raiseHand(accountId: String, confId: String, peerId: String, state: Boolean, deviceId:String?){
        mExecutor.execute {
            Log.i(TAG, "participant $peerId raise hand…")
            JamiService.raiseHand(accountId, confId, peerId, deviceId ?: "", state)
        }
    }

    fun holdCallOrConference(conf: Conference) {
        if (conf.isSimpleCall)
            hold(conf.accountId, conf.id)
        else
            holdConference(conf.accountId, conf.id)
    }
    fun unholdCallOrConference(conf: Conference) {
        if (conf.isSimpleCall)
            unhold(conf.accountId, conf.id)
        else
            unholdConference(conf.accountId, conf.id)
    }

    fun hold(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "hold() running… $callId")
            JamiService.hold(accountId, callId)
        }
    }

    fun unhold(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "unhold() running… $callId")
            JamiService.unhold(accountId, callId)
        }
    }

    fun muteRingTone(mute: Boolean) {
        Log.d(TAG, (if (mute) "Muting." else "Unmuting.") + " ringtone.")
        JamiService.muteRingtone(mute)
    }

    fun restartAudioLayer() {
        mExecutor.execute {
            Log.i(TAG, "restartAudioLayer() running…")
            JamiService.setAudioPlugin(JamiService.getCurrentAudioOutputPlugin())
        }
    }

    fun setAudioExtension(audioExtension: String) {
        mExecutor.execute {
            Log.i(TAG, "setAudioExtension() running…")
            JamiService.setAudioPlugin(audioExtension)
        }
    }

    val currentAudioOutputExtension: String?
        get() {
            try {
                return mExecutor.submit<String> {
                    Log.i(TAG, "getCurrentAudioOutputExtension() running…")
                    JamiService.getCurrentAudioOutputPlugin()
                }.get()
            } catch (e: Exception) {
                Log.e(TAG, "Error running getCallDetails()", e)
            }
            return null
        }

    fun playDtmf(key: String) {
        mExecutor.execute {
            Log.i(TAG, "playDTMF() running… $key")
            JamiService.playDTMF(key)
        }
    }

    fun setMuted(mute: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "muteCapture() running…")
            JamiService.muteCapture(mute)
        }
    }

    fun setLocalMediaMuted(accountId:String, callId: String, mediaType: String, mute: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "muteCapture() running…")
            JamiService.muteLocalMedia(accountId, callId, mediaType, mute)
        }
    }

    val isCaptureMuted: Boolean
        get() = JamiService.isCaptureMuted()

    fun transfer(accountId:String, callId: String, to: String) {
        mExecutor.execute {
            Log.i(TAG, "transfer() thread running…")
            if (JamiService.transfer(accountId, callId, to)) {
                Log.i(TAG, "OK")
            } else {
                Log.i(TAG, "NOT OK")
            }
        }
    }

    fun attendedTransfer(accountId:String, transferId: String, targetID: String) {
        mExecutor.execute {
            Log.i(TAG, "attendedTransfer() thread running…")
            if (JamiService.attendedTransfer(accountId, transferId, targetID)) {
                Log.i(TAG, "OK")
            } else {
                Log.i(TAG, "NOT OK")
            }
        }
    }

    var recordPath: String?
        get() {
            try {
                return mExecutor.submit<String> { JamiService.getRecordPath() }.get()
            } catch (e: Exception) {
                Log.e(TAG, "Error running isCaptureMuted()", e)
            }
            return null
        }
        set(path) {
            mExecutor.execute { JamiService.setRecordPath(path) }
        }

    fun toggleRecordingCall(accountId:String, callId: String): Boolean {
        mExecutor.execute { JamiService.toggleRecording(accountId, callId) }
        return false
    }

    fun startRecordedFilePlayback(filepath: String): Boolean {
        mExecutor.execute { JamiService.startRecordedFilePlayback(filepath) }
        return false
    }

    fun stopRecordedFilePlayback() {
        mExecutor.execute { JamiService.stopRecordedFilePlayback() }
    }

    fun sendTextMessage(accountId: String, callId: String, msg: String) {
        mExecutor.execute {
            Log.i(TAG, "sendTextMessage() thread running…")
            val messages = StringMap().apply { setUnicode("text/plain", msg) }
            JamiService.sendTextMessage(accountId, callId, messages, "", false)
        }
    }

    fun sendAccountTextMessage(accountId: String, to: String, msg: String, flags: Int = 0): Single<Long> =
        Single.fromCallable {
            Log.i(TAG, "sendAccountTextMessage() running… $accountId $to $msg")
            JamiService.sendAccountTextMessage(accountId, to, StringMap().apply {
                setUnicode("text/plain", msg)
            }, flags)
        }.subscribeOn(Schedulers.from(mExecutor))

    fun cancelMessage(accountId: String, messageID: Long): Completable =
        Completable.fromAction {
            Log.i(TAG, "CancelMessage() running…   Account ID:  $accountId Message ID  $messageID")
            JamiService.cancelMessage(accountId, messageID)
        }.subscribeOn(Schedulers.from(mExecutor))

    private fun addCall(accountId: String, callId: String, from: Uri, direction: Direction, media: List<Media>, conversationUri: Uri? = null): Call =
        synchronized(calls) {
            calls.getOrPut(callId) {
                val account = mAccountService.getAccount(accountId)!!
                val contact = mContactService.findContact(account, from)
                val conversationUri = conversationUri ?: contact.conversationUri.blockingFirst()
                val conversation =
                    if (conversationUri == from) account.getByUri(from) else account.getSwarm(conversationUri.rawRingId)
                Call(callId, from.uri, accountId, conversation, contact, direction)
            }.apply { setMediaList(media) }
        }

    private fun addConference(call: Call): Conference {
        val confId = call.confId ?: call.id!!
        var conference = conferences[confId]
        if (conference == null) {
            conference = Conference(call)
            conferences[confId] = conference
            conferenceSubject.onNext(conference)
        }
        return conference
    }

    private fun parseCallState(accountId: String, callId: String, newState: String, callDetails: Map<String, String>): Call? {
        val callState = CallStatus.fromString(newState)
        val call = calls[callId]
        if (call != null) {
            call.setCallState(callState)
            call.setDetails(callDetails)
            return call
        } else if (callState !== CallStatus.OVER && callState !== CallStatus.FAILURE) {
            val call = Call(callId, callDetails)
            call.setCallState(callState)
            val account = mAccountService.getAccount(call.account) ?: return null
            val contact = mContactService.findContact(account, call.peerUri)
            call.contact = contact
            Log.w(TAG, "parseCallState " + contact + " " + contact.conversationUri.blockingFirst())
            calls[callId] = call
            updateConnectionCount()
            return call
        }
        return null
    }

    fun connectionUpdate(id: String?, state: Int) {
        // Log.d(TAG, "connectionUpdate: " + id + " " + state);
        /*switch(state) {
            case 0:
                currentConnections.add(id);
                break;
            case 1:
            case 2:
                currentConnections.remove(id);
                break;
        }
        updateConnectionCount();*/
    }

    fun callStateChanged(accountId: String, callId: String, newState: String, detailCode: Int) {
        val callDetails = JamiService.getCallDetails(accountId, callId)
        Log.d(TAG, "call state changed: $callId, $newState, $detailCode")
        try {
            synchronized(calls) {
                parseCallState(accountId, callId, newState, callDetails)?.let { call ->
                    if (newState == "INCOMING") {
                        Log.d(TAG, "call state changed: ignoring ringing call, waiting for signal")
                        return
                    }
                    callSubject.onNext(call)
                    if (call.callStatus === CallStatus.OVER) {
                        calls.remove(call.id)
                        conferences.remove(call.id)
                        updateConnectionCount()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during state change: ", e)
        }
    }

    fun audioMuted(callId: String, muted: Boolean) {
        val call = calls[callId]
        if (call != null) {
            call.isAudioMuted = muted
            if (call.callStatus == CallStatus.CURRENT)
                callSubject.onNext(call)
        } else {
            conferences[callId]?.let { conf ->
                conf.isAudioMuted = muted
                conferenceSubject.onNext(conf)
            }
        }
    }

    fun videoMuted(callId: String, muted: Boolean) {
        calls[callId]?.let { call ->
            call.isVideoMuted = muted
            if (call.callStatus == CallStatus.CURRENT)
                callSubject.onNext(call)
        }
        conferences[callId]?.let { conf ->
            conf.isVideoMuted = muted
            conferenceSubject.onNext(conf)
        }
    }

    fun incomingCallWithMedia(accountId: String, callId: String, from: String, mediaList: VectMap?) {
        Log.d(TAG, "incoming call: $accountId, $callId, $from")
        val nMediaList = mediaList ?: emptyList()
        val medias = nMediaList.mapTo(ArrayList(nMediaList.size)) { mediaMap -> Media(mediaMap) }
        val call = addCall(accountId, callId, Uri.fromStringWithName(from).first, Direction.INCOMING, medias)
        callSubject.onNext(call)
        updateConnectionCount()
    }

    fun mediaChangeRequested(accountId: String, callId: String, mediaList: VectMap) {
        calls[callId]?.let { call ->
            if (!call.hasActiveMedia(Media.MediaType.MEDIA_TYPE_VIDEO)) {
                for (e in mediaList) {
                    if (e[Media.MEDIA_TYPE_KEY] == MEDIA_TYPE_VIDEO)
                        e[Media.MUTED_KEY] = true.toString()
                }
            }
            for (e in mediaList) {
                if (e[Media.MEDIA_TYPE_KEY] == MEDIA_TYPE_AUDIO)
                    e[Media.MUTED_KEY] = call.isAudioMuted.toString()
            }
            JamiService.answerMediaChangeRequest(accountId, callId, mediaList)
        }
    }

    fun mediaNegotiationStatus(callId: String, event: String, ml: VectMap) {
        val media = ml.mapTo(ArrayList(ml.size)) { media -> Media(media) }
        val call = synchronized(calls) {
            calls[callId]?.apply {
                setMediaList(media)
            }
        }
        callSubject.onNext(call ?: return)
    }

    /**
     * Replaces the current video media with a given source.
     * Creates the video media if none exists.
     * @param conf The conference to request a video media change on.
     * @param uri The uri for the source.
     * @param mute Whether to mute or un-mute the source.
     */
    fun replaceVideoMedia(conf: Conference, uri: String, mute: Boolean) {
        val call = conf.firstCall ?: return
        val mediaList = call.mediaList

        var videoExists = false
        val proposedMediaList = mediaList.map {
            if(it.mediaType == Media.MediaType.MEDIA_TYPE_VIDEO) {
                videoExists = true
                it.copy(source = uri, isMuted = mute)
            } else {
                it
            }
        } as MutableList
        if(!videoExists)
            proposedMediaList.add(Media.DEFAULT_VIDEO.copy(source = uri))

        mExecutor.execute {
            JamiService.requestMediaChange(
                call.account,
                call.id,
                proposedMediaList.mapTo(VectMap().apply {
                    reserve(proposedMediaList.size)
                }) { it.toMap() }
            )
        }
    }

    fun incomingMessage(accountId: String, callId: String, from: String, messages: Map<String, String>) {
        val call = calls[callId]
        if (call == null) {
            Log.w(TAG, "incomingMessage: unknown call or no message: $callId $from")
            return
        }
        if (messages.containsKey(MIME_TEXT_PLAIN)) {
            mAccountService.incomingAccountMessage(accountId, null, callId, from, messages)
        }
    }

    fun recordPlaybackFilepath(id: String, filename: String) {
        Log.d(TAG, "record playback filepath: $id, $filename")
        // todo needs more explanations on that
    }

    fun onRtcpReportReceived(callId: String) {
        Log.i(TAG, "on RTCP report received: $callId")
    }

    fun joinParticipant(accountId: String, selCallId: String, account2Id: String, dragCallId: String): Single<Boolean> {
        return Single.fromCallable { JamiService.joinParticipant(accountId, selCallId, account2Id, dragCallId) }
            .subscribeOn(Schedulers.from(mExecutor))
    }

    fun addParticipant(accountId: String, callId: String, account2Id: String, confId: String) {
        mExecutor.execute { JamiService.addParticipant(accountId, callId, account2Id, confId) }
    }

    fun addMainParticipant(accountId: String, confId: String) {
        mExecutor.execute { JamiService.addMainParticipant(accountId, confId) }
    }

    fun detachParticipant(accountId: String, callId: String) {
        mExecutor.execute { JamiService.detachParticipant(accountId, callId) }
    }

    fun joinConference(accountId: String, selConfId: String, account2Id: String, dragConfId: String) {
        mExecutor.execute { JamiService.joinConference(accountId, selConfId, account2Id, dragConfId) }
    }

    fun hangUpConference(accountId: String, confId: String) {
        Log.i(TAG, "hangUpConference() running… $confId")
        mExecutor.execute { JamiService.hangUpConference(accountId, confId) }
    }

    fun holdConference(accountId: String, confId: String) {
        mExecutor.execute { JamiService.holdConference(accountId, confId) }
    }

    fun unholdConference(accountId: String, confId: String) {
        mExecutor.execute { JamiService.unholdConference(accountId, confId) }
    }

    private fun getConference(call: Call): Conference = addConference(call)

    fun getConference(id: String): Conference? = conferences[id]

    fun conferenceCreated(accountId: String, conversationId: String, confId: String) {
        Log.d(TAG, "conference created: $confId $conversationId")
        val conf = conferences.getOrPut(confId) { Conference(accountId, confId).apply {
            if (conversationId.isNotEmpty())
                this.conversationId = conversationId
        } }
        val participants = JamiService.getParticipantList(accountId, confId)
        val map = JamiService.getConferenceDetails(accountId, confId)
        conf.setState(map["STATE"]!!)
        for (callId in participants) {
            calls[callId]?.let { call ->
                Log.d(TAG, "conferenceCreated: adding participant $callId ${call.contact}")
                call.confId = confId
                conf.addParticipant(call)
            }
            conferences.remove(callId)
        }
        if (conversationId.isNotEmpty())
            mAccountService.getAccount(accountId)?.let { account ->
                val conversation = account.getSwarm(conversationId)
                //call.conversation = conversation
                conversation?.addConference(conf)
            }
        conferenceSubject.onNext(conf)
    }

    fun conferenceRemoved(accountId: String, confId: String) {
        Log.d(TAG, "conference removed: $confId")
        conferences.remove(confId)?.let { conf ->
            for (call in conf.participants) {
                call.confId = null
            }
            conf.removeParticipants()
            conf.hostCall?.let {
                it.setCallState(CallStatus.OVER)
                callSubject.onNext(it)
                conf.hostCall = null
            }
            conf.conversationId?.let { conversationId ->
                mAccountService.getAccount(accountId)
                    ?.getSwarm(conversationId)
                    ?.removeConference(conf)
            }
            conferenceSubject.onNext(conf)
        }
    }

    fun conferenceChanged(accountId: String, confId: String, state: String) {
        Log.d(TAG, "conference changed: $confId, $state")
        try {
            val participants: Set<String> = JamiService.getParticipantList(accountId, confId).toHashSet()

            val conf = conferences.getOrPut(confId) { Conference(accountId, confId) }
            conf.setState(state)
            // Add new participants
            for (callId in participants) {
                if (!conf.contains(callId)) {
                    calls[callId]?.let { call ->
                        Log.d(TAG, "conference changed: adding participant " + callId + " " + call.contact)
                        call.confId = confId
                        conf.addParticipant(call)
                    }
                    conferences.remove(callId)
                }
            }

            // Remove participants
            val calls = conf.participants
            var removed = false
            val i = calls.iterator()
            while (i.hasNext()) {
                val call = i.next()
                if (!participants.contains(call.id)) {
                    Log.d(TAG, "conference changed: removing participant " + call.id + " " + call.contact)
                    call.confId = null
                    i.remove()
                    removed = true
                }
            }

            conf.hostCall?.let {
                it.setCallState(conf.hostConfState)
                callSubject.onNext(it)
            }

            conferenceSubject.onNext(conf)
            if (removed && conf.participants.size == 1) {
                val call = conf.participants[0]
                call.confId = null
                addConference(call)
            }
        } catch (e: Exception) {
            Log.w(TAG, "exception in conferenceChanged", e)
        }
    }

    fun hangUpAny(accountId: String, callId: String) {
        calls.filterValues { it.id == callId }.forEach {
            val confId = it.value.confId
            if(confId != null)
                hangUpConference(accountId, confId)
            else
                hangUp(accountId, callId)
        }
    }

    companion object {
        @JvmStatic
        protected val TAG = CallService::class.simpleName!!
        const val MIME_TEXT_PLAIN = "text/plain"
        const val MIME_GEOLOCATION = "application/geo"
        const val MEDIA_TYPE_AUDIO = "MEDIA_TYPE_AUDIO"
        const val MEDIA_TYPE_VIDEO = "MEDIA_TYPE_VIDEO"

        val CALL_ALLOWED_VAL = SystemCall(true)
        val CALL_DISALLOWED_VAL = SystemCall(false)
        val CALL_ALLOWED = Single.just(CALL_ALLOWED_VAL)
        val CALL_DISALLOWED = Single.just(CALL_DISALLOWED_VAL)
    }
}