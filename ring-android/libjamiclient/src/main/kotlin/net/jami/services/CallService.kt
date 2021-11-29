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
package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.daemon.Blob
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.VectMap
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Conference
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Media
import net.jami.model.Uri
import net.jami.utils.Log
import net.jami.utils.StringUtils
import java.util.*
import java.util.concurrent.ScheduledExecutorService

class CallService(
    private val mExecutor: ScheduledExecutorService,
    private val mContactService: ContactService,
    private val mAccountService: AccountService
) {
    private val currentCalls: MutableMap<String, Call> = HashMap()
    private val currentConferences: MutableMap<String, Conference> = HashMap()
    private val callSubject = PublishSubject.create<Call>()
    private val conferenceSubject = PublishSubject.create<Conference>()

    // private final Set<String> currentConnections = new HashSet<>();
    // private final BehaviorSubject<Integer> connectionSubject = BehaviorSubject.createDefault(0);
    val confsUpdates: Observable<Conference>
        get() = conferenceSubject

    private fun getConfCallUpdates(conf: Conference): Observable<Conference> {
        Log.w(TAG, "getConfCallUpdates " + conf.id)
        return conferenceSubject
            .filter { c -> c == conf }
            .startWithItem(conf)
            .map(Conference::participants)
            .switchMap { list: List<Call> -> Observable.fromIterable(list)
                    .flatMap { call: Call -> callSubject.filter { c -> c == call } } }
            .map { conf }
            .startWithItem(conf)
    }

    fun getConfUpdates(confId: String): Observable<Conference> {
        return getCurrentCallForId(confId)?.let { getConfUpdates(it) }
            ?: Observable.error(IllegalArgumentException())
        /*Conference call = currentConferences.get(confId);
        return call == null ? Observable.error(new IllegalArgumentException()) : conferenceSubject
                .filter(c -> c.getId().equals(confId));//getConfUpdates(call);*/
    }

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
            val account = mAccountService.getAccount(conference.firstCall?.account) ?: return

            for (i in info) {
                val uri = i["uri"]!!
                val confInfo = if (uri.isEmpty()) {
                    ParticipantInfo(null, account.getContactFromCache(Uri.fromId(account.username!!)), i)
                } else {
                    val contactUri = Uri.fromString(uri)
                    val call = conference.findCallByContact(contactUri)
                    if (call != null) {
                        ParticipantInfo(call, call.contact!!, i)
                    } else {
                        ParticipantInfo(null, account.getContactFromCache(contactUri), i)
                    }
                }
                if (confInfo.isEmpty) {
                    Log.w(TAG, "onConferenceInfoUpdated: ignoring empty entry $i")
                    continue
                }
                if (confInfo.contact.isUser && confInfo.isModerator) {
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
            call = getCurrentCallForId(callId)
            if (call != null) {
                conference = getConference(call)
            }
        } else {
            call = conference.firstCall
        }
        val account = if (call == null) null else mAccountService.getAccount(call.account!!)
        val contact = account?.getContactFromCache(peerNumber)
        if (conference != null && contact != null) {
            conference.setParticipantRecording(contact, state)
        }
    }

    private class ConferenceEntity internal constructor(var conference: Conference)

    fun getConfUpdates(call: Call): Observable<Conference> {
        return getConfUpdates(getConference(call))
    }

    private fun getConfUpdates(conference: Conference): Observable<Conference> {
        Log.w(TAG, "getConfUpdates " + conference.id)
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
                if (conferenceEntity.conference.participants.size == 1 && conf.participants.size == 1 && conferenceEntity.conference.call == conf.call && conf.call!!.daemonIdString == conf.id) {
                    Log.w(TAG, "Switching tracked conference (down) to " + conf.id)
                    conferenceEntity.conference = conf
                    return@filter true
                }
                false
            }
            .switchMap { conf: Conference -> getConfCallUpdates(conf) }
    }

    val callsUpdates: Observable<Call>
        get() = callSubject

    private fun getCallUpdates(call: Call): Observable<Call> {
        return callSubject.filter { c: Call -> c == call }
            .startWithItem(call)
            .takeWhile { c: Call -> c.callStatus !== CallStatus.OVER }
    }

    /*public Observable<SipCall> getCallUpdates(final String callId) {
        SipCall call = getCurrentCallForId(callId);
        return call == null ? Observable.error(new IllegalArgumentException()) : getCallUpdates(call);
    }*/
    fun placeCallObservable(accountId: String, conversationUri: Uri?, number: Uri, hasVideo: Boolean): Observable<Call> {
        return placeCall(accountId, conversationUri, number, hasVideo)
            .flatMapObservable { call: Call -> getCallUpdates(call) }
    }

    fun placeCall(account: String, conversationUri: Uri?, number: Uri, hasVideo: Boolean): Single<Call> {
        return Single.fromCallable<Call> {
            Log.i(TAG, "placeCall() thread running... $number hasVideo: $hasVideo")
            val media = VectMap()
            media.reserve(if (hasVideo) 2L else 1L)
            media.add(Media.DEFAULT_AUDIO.toMap())
            if (hasVideo)
                media.add(Media.DEFAULT_VIDEO.toMap())
            val callId = JamiService.placeCallWithMedia(account, number.uri, media)
            if (callId == null || callId.isEmpty()) return@fromCallable null
            //Log.w(TAG, "DEBUG fn placeCall() -> La valeur de hasVideo est $hasVideo et la valeur de !hasVideo est ${!hasVideo}")

            val call = addCall(account, callId, number, Call.Direction.OUTGOING, if (hasVideo) listOf(Media.DEFAULT_AUDIO, Media.DEFAULT_VIDEO) else listOf(Media.DEFAULT_AUDIO))
            if (conversationUri != null && conversationUri.isSwarm) call.setSwarmInfo(conversationUri.rawRingId)
            updateConnectionCount()
            call
        }.subscribeOn(Schedulers.from(mExecutor))
    }

    fun refuse(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "refuse() running... $callId")
            JamiService.refuse(accountId, callId)
            JamiService.hangUp(accountId, callId)
        }
    }

/*    fun accept(callId: String) {
        mExecutor.execute {
            Log.i(TAG, "accept() running... $callId")
            JamiService.muteCapture(false)
            JamiService.accept(callId)
        }
    }*/

    fun accept(accountId:String, callId: String, hasVideo: Boolean = false) {
        mExecutor.execute {
            Log.i(TAG, "accept() running... $callId")
            val call = currentCalls[callId] ?: return@execute
            val mediaList = call.mediaList ?: return@execute
            val vectMapMedia = mediaList.mapTo(VectMap().apply { reserve(mediaList.size.toLong()) }) { media ->
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
            Log.i(TAG, "hangUp() running... $callId")
            JamiService.hangUp(accountId, callId)
        }
    }

    fun muteParticipant(accountId:String, confId: String, peerId: String, mute: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "mute participant... $peerId")
            JamiService.muteParticipant(accountId, confId, peerId, mute)
        }
    }

    fun hangupParticipant(accountId:String, confId: String?, peerId: String) {
        mExecutor.execute {
            Log.i(TAG, "hangup participant... $peerId")
            JamiService.hangupParticipant(accountId, confId, peerId)
        }
    }

    fun raiseParticipantHand(accountId: String, confId: String, peerId: String, state: Boolean){
        mExecutor.execute {
            Log.i(TAG, "participant $peerId raise hand... ")
            JamiService.raiseParticipantHand(accountId, confId, peerId, state)
        }
    }

    fun hold(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "hold() running... $callId")
            JamiService.hold(accountId, callId)
        }
    }

    fun unhold(accountId:String, callId: String) {
        mExecutor.execute {
            Log.i(TAG, "unhold() running... $callId")
            JamiService.unhold(accountId, callId)
        }
    }

    fun muteRingTone(mute: Boolean) {
        Log.d(TAG, (if (mute) "Muting." else "Unmuting.") + " ringtone.")
        JamiService.muteRingtone(mute)
    }

    fun restartAudioLayer() {
        mExecutor.execute {
            Log.i(TAG, "restartAudioLayer() running...")
            JamiService.setAudioPlugin(JamiService.getCurrentAudioOutputPlugin())
        }
    }

    fun setAudioPlugin(audioPlugin: String) {
        mExecutor.execute {
            Log.i(TAG, "setAudioPlugin() running...")
            JamiService.setAudioPlugin(audioPlugin)
        }
    }

    val currentAudioOutputPlugin: String?
        get() {
            try {
                return mExecutor.submit<String> {
                    Log.i(TAG, "getCurrentAudioOutputPlugin() running...")
                    JamiService.getCurrentAudioOutputPlugin()
                }.get()
            } catch (e: Exception) {
                Log.e(TAG, "Error running getCallDetails()", e)
            }
            return null
        }

    fun playDtmf(key: String) {
        mExecutor.execute {
            Log.i(TAG, "playDTMF() running...")
            JamiService.playDTMF(key)
        }
    }

    fun setMuted(mute: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "muteCapture() running...")
            JamiService.muteCapture(mute)
        }
    }

    fun setLocalMediaMuted(accountId:String, callId: String, mediaType: String, mute: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "muteCapture() running...")
            JamiService.muteLocalMedia(accountId, callId, mediaType, mute)
        }
    }

    val isCaptureMuted: Boolean
        get() = JamiService.isCaptureMuted()

    fun transfer(accountId:String, callId: String, to: String) {
        mExecutor.execute {
            Log.i(TAG, "transfer() thread running...")
            if (JamiService.transfer(accountId, callId, to)) {
                Log.i(TAG, "OK")
            } else {
                Log.i(TAG, "NOT OK")
            }
        }
    }

    fun attendedTransfer(accountId:String, transferId: String, targetID: String) {
        mExecutor.execute {
            Log.i(TAG, "attendedTransfer() thread running...")
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
            Log.i(TAG, "sendTextMessage() thread running...")
            val messages = StringMap()
            messages.setRaw("text/plain", Blob.fromString(msg))
            JamiService.sendTextMessage(accountId, callId, messages, "", false)
        }
    }

    fun sendAccountTextMessage(accountId: String, to: String, msg: String): Single<Long> {
        return Single.fromCallable {
            Log.i(TAG, "sendAccountTextMessage() running... $accountId $to $msg")
            val msgs = StringMap()
            msgs.setRaw("text/plain", Blob.fromString(msg))
            JamiService.sendAccountTextMessage(accountId, to, msgs)
        }.subscribeOn(Schedulers.from(mExecutor))
    }

    fun cancelMessage(accountId: String, messageID: Long): Completable {
        return Completable.fromAction {
            Log.i(TAG, "CancelMessage() running...   Account ID:  $accountId Message ID  $messageID")
            JamiService.cancelMessage(accountId, messageID)
        }.subscribeOn(Schedulers.from(mExecutor))
    }

    private fun getCurrentCallForId(callId: String): Call? {
        return currentCalls[callId]
    }

    private fun addCall(accountId: String, callId: String, from: Uri, direction: Call.Direction, mediaList: List<Media>): Call {
        synchronized(currentCalls) {
            var call = currentCalls[callId]
            if (call == null) {
                val account = mAccountService.getAccount(accountId)!!
                val contact = mContactService.findContact(account, from)
                val conversationUri = contact.conversationUri.blockingFirst()
                val conversation =
                    if (conversationUri.equals(from)) account.getByUri(from) else account.getSwarm(conversationUri.rawRingId)
                call = Call(callId, from.uri, accountId, conversation, contact, direction, mediaList)
                currentCalls[callId] = call
            } else {
                Log.w(TAG, "Call already existed ! $callId $from")
                call.mediaList = mediaList
            }
            return call
        }
    }

    private fun addConference(call: Call): Conference {
        val confId = call.confId ?: call.daemonIdString!!
        var conference = currentConferences[confId]
        if (conference == null) {
            conference = Conference(call)
            currentConferences[confId] = conference
            conferenceSubject.onNext(conference)
        }
        return conference
    }

    private fun parseCallState(accountId:String, callId: String, newState: String): Call? {
        val callState = CallStatus.fromString(newState)
        var call = currentCalls[callId]
        if (call != null) {
            call.setCallState(callState)
            call.setDetails(JamiService.getCallDetails(accountId, callId).toNative())
        } else if (callState !== CallStatus.OVER && callState !== CallStatus.FAILURE) {
            val callDetails: Map<String, String> = JamiService.getCallDetails(accountId, callId)
            call = Call(callId, callDetails)
            if (StringUtils.isEmpty(call.contactNumber)) {
                Log.w(TAG, "No number")
                return null
            }
            call.setCallState(callState)
            val account = mAccountService.getAccount(call.account!!)!!
            val contact = mContactService.findContact(account, Uri.fromString(call.contactNumber!!))
            val registeredName = callDetails[Call.KEY_REGISTERED_NAME]
            if (registeredName != null && registeredName.isNotEmpty()) {
                contact.setUsername(registeredName)
            }
            val conversation = account.getByUri(contact.conversationUri.blockingFirst())
            call.contact = contact
            call.conversation = conversation
            Log.w(TAG, "parseCallState " + contact + " " + contact.conversationUri.blockingFirst() + " " + conversation + " " + conversation?.participant)
            currentCalls[callId] = call
            updateConnectionCount()
        }
        return call
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
        Log.d(TAG, "call state changed: $callId, $newState, $detailCode")
        try {
            synchronized(currentCalls) {
                parseCallState(accountId, callId, newState)?.let { call ->
                    callSubject.onNext(call)
                    if (call.callStatus === CallStatus.OVER) {
                        currentCalls.remove(call.daemonIdString)
                        currentConferences.remove(call.daemonIdString)
                        updateConnectionCount()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during state change: ", e)
        }
    }

    fun audioMuted(callId: String, muted: Boolean) {
        val call = currentCalls[callId]
        if (call != null) {
            call.isAudioMuted = muted
            callSubject.onNext(call)
        } else {
            currentConferences[callId]?.let { conf ->
                conf.isAudioMuted = muted
                conferenceSubject.onNext(conf)
            }
        }
    }

    fun videoMuted(callId: String, muted: Boolean) {
        currentCalls[callId]?.let { call ->
            call.isVideoMuted = muted
            callSubject.onNext(call)
        }
        currentConferences[callId]?.let { conf ->
            conf.isVideoMuted = muted
            conferenceSubject.onNext(conf)
        }
    }

    fun incomingCallWithMedia(accountId: String, callId: String, from: String, mediaList: VectMap?) {
        Log.d(TAG, "incoming call: $accountId, $callId, $from")
        val nMediaList = mediaList ?: emptyList()
        val medias = nMediaList.mapTo(ArrayList(nMediaList.size)) { mediaMap -> Media(mediaMap) }
        val call = addCall(accountId, callId, Uri.fromStringWithName(from).first, Call.Direction.INCOMING, medias)
        callSubject.onNext(call)
        updateConnectionCount()
    }

    fun mediaChangeRequested(accountId: String, callId: String, mediaList: VectMap) {
        currentCalls[callId]?.let { call ->
            if (!call.hasActiveMedia(Media.MediaType.MEDIA_TYPE_VIDEO)) {
                for (e in mediaList)
                    if (e[Media.MEDIA_TYPE_KEY]!! == MEDIA_TYPE_VIDEO)
                        e[Media.MUTED_KEY] = true.toString()
            }
            JamiService.answerMediaChangeRequest(accountId, callId, mediaList)
        }
    }

    fun mediaNegotiationStatus(callId: String, event: String, mediaList: VectMap) {
        synchronized(currentCalls) {
            currentCalls[callId]?.let { call ->
                call.mediaList = mediaList.mapTo(ArrayList(mediaList.size)) { media -> Media(media) }
                callSubject.onNext(call)
            }
        }
    }

    fun requestVideoMedia(conf: Conference, enable: Boolean) {
        if (conf.isConference || conf.hasVideo()) {
            JamiService.muteLocalMedia(conf.accountId, conf.id,  Media.MediaType.MEDIA_TYPE_VIDEO.name, !enable)
        } else if (enable) {
            val call = conf.firstCall ?: return
            val mediaList = call.mediaList ?: return
            JamiService.requestMediaChange(call.account, call.daemonIdString, mediaList.mapTo(VectMap()
                    .apply { reserve(mediaList.size.toLong() + 1L) }
            ) { media -> media.toMap() }
                .apply { add(Media.DEFAULT_VIDEO.toMap()) })
        }
    }

    fun incomingMessage(accountId: String, callId: String, from: String, messages: Map<String, String>) {
        val call = currentCalls[callId]
        if (call == null) {
            Log.w(TAG, "incomingMessage: unknown call or no message: $callId $from")
            return
        }
        call.appendToVCard(messages)?.let { vcard ->
            mContactService.saveVCardContactData(call.contact!!, call.account!!, vcard)
        }
        if (messages.containsKey(MIME_TEXT_PLAIN)) {
            mAccountService.incomingAccountMessage(call.account!!, null, callId, from, messages)
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
        mExecutor.execute { JamiService.hangUpConference(accountId, confId) }
    }

    fun holdConference(accountId: String, confId: String) {
        mExecutor.execute { JamiService.holdConference(accountId, confId) }
    }

    fun unholdConference(accountId: String, confId: String) {
        mExecutor.execute { JamiService.unholdConference(accountId, confId) }
    }

    fun getConference(call: Call): Conference {
        return addConference(call)
    }

    fun getConference(id: String): Conference? {
        return currentConferences[id]
    }

    fun conferenceCreated(accountId: String, confId: String) {
        Log.d(TAG, "conference created: $confId")
        var conf = currentConferences[confId]
        if (conf == null) {
            conf = Conference(accountId, confId)
            currentConferences[confId] = conf
        }
        val participants = JamiService.getParticipantList(accountId, confId)
        val map = JamiService.getConferenceDetails(accountId, confId)
        conf.setState(map["STATE"]!!)
        for (callId in participants) {
            val call = getCurrentCallForId(callId)
            if (call != null) {
                Log.d(TAG, "conference created: adding participant " + callId + " " + call.contact!!.displayName)
                call.confId = confId
                conf.addParticipant(call)
            }
            val rconf = currentConferences.remove(callId)
            Log.d(TAG, "conference created: removing conference " + callId + " " + rconf + " now " + currentConferences.size)
        }
        conferenceSubject.onNext(conf)
    }

    fun conferenceRemoved(accountId: String, confId: String) {
        Log.d(TAG, "conference removed: $confId")
        currentConferences.remove(confId)?.let { conf ->
            for (call in conf.participants) {
                call.confId = null
            }
            conf.removeParticipants()
            conferenceSubject.onNext(conf)
        }
    }

    fun conferenceChanged(accountId: String, confId: String, state: String) {
        Log.d(TAG, "conference changed: $confId, $state")
        try {
            var conf = currentConferences[confId]
            if (conf == null) {
                conf = Conference(accountId, confId)
                currentConferences[confId] = conf
            }
            conf.setState(state)
            val participants: Set<String> = JamiService.getParticipantList(accountId, confId).toHashSet()
            // Add new participants
            for (callId in participants) {
                if (!conf.contains(callId)) {
                    val call = getCurrentCallForId(callId)
                    if (call != null) {
                        Log.d(TAG, "conference changed: adding participant " + callId + " " + call.contact!!.displayName)
                        call.confId = confId
                        conf.addParticipant(call)
                    }
                    currentConferences.remove(callId)
                }
            }

            // Remove participants
            val calls = conf.participants
            var removed = false
            val i = calls.iterator()
            while (i.hasNext()) {
                val call = i.next()
                if (!participants.contains(call.daemonIdString)) {
                    Log.d(TAG, "conference changed: removing participant " + call.daemonIdString + " " + call.contact!!.displayName)
                    call.confId = null
                    i.remove()
                    removed = true
                }
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

    companion object {
        private val TAG = CallService::class.simpleName!!
        const val MIME_TEXT_PLAIN = "text/plain"
        const val MIME_GEOLOCATION = "application/geo"
        const val MEDIA_TYPE_AUDIO = "MEDIA_TYPE_AUDIO"
        const val MEDIA_TYPE_VIDEO = "MEDIA_TYPE_VIDEO"
    }
}