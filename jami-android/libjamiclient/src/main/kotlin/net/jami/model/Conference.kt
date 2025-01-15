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
package net.jami.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Call.CallStatus
import java.util.*
import kotlin.math.min

class Conference(val accountId: String, val id: String) {
    class ParticipantInfo(val call: Call?, val contact: ContactViewModel, i: Map<String, String>, val pending: Boolean = false) {
        val x: Int = i["x"]?.toInt() ?: 0
        val y: Int = i["y"]?.toInt() ?: 0
        val w: Int = i["w"]?.toInt() ?: 0
        val h: Int = i["h"]?.toInt() ?: 0
        val videoMuted: Boolean = i["videoMuted"].toBoolean()
        val audioModeratorMuted: Boolean = i["audioModeratorMuted"].toBoolean()
        val audioLocalMuted: Boolean = i["audioLocalMuted"].toBoolean()
        val isModerator: Boolean = i["isModerator"].toBoolean()
        val isHandRaised: Boolean = i["handRaised"].toBoolean()

        val active: Boolean = i["active"].toBoolean()
        val device: String? = i["device"]
        val sinkId: String? = i["sinkId"]

        val isEmpty: Boolean
            get() = x == 0 && y == 0 && w == 0 && h == 0

        val tag: String
            get() = sinkId ?: contact.contact.uri.uri

        override fun hashCode(): Int = Objects.hash(contact.contact.uri, device, call?.id, pending)
    }

    constructor(call: Call) : this(call.account, call.confId ?: call.id!!) {
        mParticipants.add(call)
    }

    private val mParticipantInfo: Subject<List<ParticipantInfo>> = BehaviorSubject.createDefault(emptyList())
    private val mPendingCalls: MutableList<ParticipantInfo> = ArrayList()
    private val mPendingSubject: Subject<List<ParticipantInfo>> = BehaviorSubject.createDefault(mPendingCalls)

    private val mParticipantRecordingSet: MutableSet<Contact> = HashSet()
    private val mParticipantRecording: Subject<Set<Contact>> = BehaviorSubject.createDefault(emptySet())
    private var mConfState: CallStatus? = null
    private val mParticipants: ArrayList<Call> = ArrayList()
    private val mParticipantsSubject: Subject<List<Call>> = BehaviorSubject.createDefault(mParticipants)

    /** virtual call with null ID to represent the "call" between this device and the local hosted conference */
    var hostCall: Call? = null

    val participantsObservable: Observable<List<Call>>
        get() = mParticipantsSubject

    private var mRecording: Boolean = false
    var maximizedParticipant: Contact? = null
    var isModerator = false

    var isAudioMuted = false
        get() = call?.isAudioMuted ?: field
    var isVideoMuted = false
        get() = call?.isVideoMuted ?: field

    val isRinging: Boolean
        get() = mParticipants.isNotEmpty() && mParticipants[0].isRinging
    val isConference: Boolean
        get() = mParticipants.size > 1 || conversationId != null || (mParticipants.size == 1 && mParticipants[0].id != id)
    val call: Call?
        get() = if (!isConference) {
            firstCall
        } else null
    val firstCall: Call?
        get() = if (mParticipants.isNotEmpty()) {
            mParticipants[0]
        } else null

    val extensionId: String
        get() = "local"

    val state: CallStatus?
        get() = if (isSimpleCall) {
            mParticipants[0].callStatus
        } else mConfState

    val confState: CallStatus?
        get() = if (mParticipants.size == 1) {
            mParticipants[0].callStatus
        } else mConfState

    val hostConfState: CallStatus
        get() = mConfState!!

    val isSimpleCall: Boolean
        get() = mParticipants.size == 1 && id == mParticipants[0].id

    /** If not null, this conference is a swarm call */
    var conversationId: String? = null

    fun setState(state: String) {
        mConfState = CallStatus.fromConferenceString(state)
    }

    val participants: MutableList<Call>
        get() = mParticipants

    fun addParticipant(part: Call) {
        mParticipants.add(part)
    }

    fun removeParticipant(toRemove: Call): Boolean {
        return mParticipants.remove(toRemove)
    }

    fun removeParticipants() {
        mParticipants.clear()
    }

    fun addPending(participantInfo: ParticipantInfo) {
        mPendingCalls.add(participantInfo)
        mPendingSubject.onNext(mPendingCalls)
    }

    fun removePending(participantInfo: ParticipantInfo) {
        mPendingCalls.remove(participantInfo)
        mPendingSubject.onNext(mPendingCalls)
    }

    operator fun contains(callID: String): Boolean {
        for (participant in mParticipants)
            if (participant.id == callID) return true
        return false
    }

    fun getCallById(callID: String): Call? {
        for (participant in mParticipants)
            if (participant.id == callID) return participant
        return null
    }

    fun findCallByContact(uri: Uri): Call? {
        val mUri = if (uri.host == "ring.dht") Uri.fromId(uri.rawRingId) else uri
        for (call in mParticipants) {
            if (call.contact?.uri == mUri)
                return call
        }
        return null
    }

    val isIncoming: Boolean
        get() = mParticipants.size == 1 && mParticipants[0].isIncoming
    val isOnGoing: Boolean
        get() = mParticipants.size == 1 && mParticipants[0].isOnGoing || mParticipants.size > 1

    @Deprecated("not working with groups/conferences")
    fun getMediaList(): List<Media> {
        return if (mParticipants.size == 1) mParticipants[0].mediaList else ArrayList()
    }

    val hasVideo: Observable<Boolean> =
        mParticipantsSubject.switchMap { participants -> Observable.combineLatest(participants.map { it.mediaListObservable })
            { mediaLists ->
                for (mediaList in mediaLists) {
                    for (media in mediaList as List<Media>) {
                        if (media.mediaType == Media.MediaType.MEDIA_TYPE_VIDEO)
                            return@combineLatest true
                    }
                }
                false
            }
        }

    fun hasAudioMedia(): Boolean {
        return mParticipants.size == 1  && mParticipants[0].hasMedia(Media.MediaType.MEDIA_TYPE_AUDIO)
    }

    fun hasVideo(): Boolean {
        for (call in mParticipants) if (call.hasMedia(Media.MediaType.MEDIA_TYPE_VIDEO)) return true
        return false
    }

    fun hasActiveScreenSharing(): Boolean {
        for (call in mParticipants)
            if(call.hasActiveScreenSharing()) return true
        return false
    }

    fun hasActiveVideo(): Boolean {
        for (call in mParticipants)
            if (call.hasActiveMedia(Media.MediaType.MEDIA_TYPE_VIDEO))
                return true
        return false
    }

    fun hasActiveNonScreenShareVideo(): Boolean {
        return mParticipants.any { call ->
            val mediaList = call.mediaList
            mediaList.any { media ->
                media.isEnabled &&
                !media.isMuted &&
                media.mediaType == Media.MediaType.MEDIA_TYPE_VIDEO &&
                media.source != "camera://desktop"
            }
        }
    }

    val timestampStart: Long
        get() {
            var t = Long.MAX_VALUE
            for (call in mParticipants) t = min(call.timestamp, t)
            return t
        }

    fun setInfo(info: List<ParticipantInfo>) {
        mParticipantInfo.onNext(info)
    }

    val participantInfo: Observable<List<ParticipantInfo>>
        get() = mParticipantInfo

    val pendingCalls: Observable<List<ParticipantInfo>>
        get() = mPendingSubject

    val participantRecording: Observable<Set<Contact>>
        get() = mParticipantRecording

    fun setParticipantRecording(contact: Contact, state: Boolean) {
        if (state) {
            mParticipantRecordingSet.add(contact)
        } else {
            mParticipantRecordingSet.remove(contact)
        }
        mParticipantRecording.onNext(mParticipantRecordingSet)
    }

}