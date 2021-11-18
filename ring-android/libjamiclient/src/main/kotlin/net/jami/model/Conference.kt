/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.call.CallPresenter
import net.jami.model.Call.CallStatus
import net.jami.model.Call.CallStatus.Companion.fromConferenceString
import net.jami.utils.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

class Conference(val accountId: String, val id: String) {
    class ParticipantInfo(val call: Call?, val contact: Contact, i: Map<String, String>, val pending: Boolean = false) {
        val x: Int = i["x"]?.toInt() ?: 0
        val y: Int = i["y"]?.toInt() ?: 0
        val w: Int = i["w"]?.toInt() ?: 0
        val h: Int = i["h"]?.toInt() ?: 0
        val videoMuted: Boolean = i["videoMuted"].toBoolean()
        val audioModeratorMuted: Boolean = i["audioModeratorMuted"].toBoolean()
        val audioLocalMuted: Boolean = i["audioLocalMuted"].toBoolean()
        val isModerator: Boolean = i["isModerator"].toBoolean()
        val isEmpty: Boolean
            get() = x == 0 && y == 0 && w == 0 && h == 0
    }

    private val mParticipantInfo: Subject<List<ParticipantInfo>> = BehaviorSubject.createDefault(emptyList())
    private val mParticipantRecordingSet: MutableSet<Contact> = HashSet()
    private val mParticipantRecording: Subject<Set<Contact>> = BehaviorSubject.createDefault(emptySet())
    private var mConfState: CallStatus? = null
    private val mParticipants: ArrayList<Call> = ArrayList()
    private var mRecording: Boolean = false
    var maximizedParticipant: Contact? = null
    var isModerator = false

    var isAudioMuted = false
        get() = call?.isAudioMuted ?: field
    var isVideoMuted = false
        get() = call?.isVideoMuted ?: field

    constructor(call: Call) : this(call.account!!, call.daemonIdString!!) {
        mParticipants.add(call)
    }

    /*constructor(c: Conference) {
        id = c.id
        mConfState = c.mConfState
        mParticipants = ArrayList(c.mParticipants)
        mRecording = c.mRecording
    }*/

    val isRinging: Boolean
        get() = mParticipants.isNotEmpty() && mParticipants[0].isRinging
    val isConference: Boolean
        get() = mParticipants.size > 1
    val call: Call?
        get() = if (!isConference) {
            firstCall
        } else null
    val firstCall: Call?
        get() = if (mParticipants.isNotEmpty()) {
            mParticipants[0]
        } else null
    val pluginId: String
        get() = "local"
    val state: CallStatus?
        get() = if (isSimpleCall) {
            mParticipants[0].callStatus
        } else mConfState
    val confState: CallStatus?
        get() = if (mParticipants.size == 1) {
            mParticipants[0].callStatus
        } else mConfState

    val isSimpleCall: Boolean
        get() = mParticipants.size == 1 && id == mParticipants[0].daemonIdString

    fun setState(state: String) {
        mConfState = fromConferenceString(state)
    }

    val participants: MutableList<Call>
        get() = mParticipants

    fun addParticipant(part: Call) {
        mParticipants.add(part)
    }

    fun removeParticipant(toRemove: Call): Boolean {
        return mParticipants.remove(toRemove)
    }

    operator fun contains(callID: String): Boolean {
        for (participant in mParticipants)
            if (participant.daemonIdString == callID) return true
        return false
    }

    fun getCallById(callID: String): Call? {
        for (participant in mParticipants)
            if (participant.daemonIdString == callID) return participant
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


    fun getMediaList(): List<Media>? {
        return if (mParticipants.size == 1) mParticipants[0].mediaList else ArrayList()
    }

    fun hasAudioMedia(): Boolean {
        return mParticipants.size == 1  && mParticipants[0].hasMedia(Media.MediaType.MEDIA_TYPE_AUDIO)
    }

    fun hasVideo(): Boolean {
        for (call in mParticipants) if (call.hasMedia(Media.MediaType.MEDIA_TYPE_VIDEO)) return true
        return false
    }

 /*   fun hasVideoMedia(): Boolean {
        return mParticipants.size == 1 && mParticipants[0].hasMedia(Media.MediaType.MEDIA_TYPE_VIDEO)
    }*/

    fun hasActiveVideo(): Boolean {
        for (call in mParticipants)
            if (call.hasActiveMedia(Media.MediaType.MEDIA_TYPE_VIDEO))
                return true
        return false
    }


    fun hasActiveAudio(user: String?) {
        Log.w("Conference.kt", "DEBUG hasActiveAudio -------> init")
        mParticipantInfo.subscribe {
            Log.w("Conference.kt", "DEBUG hasActiveAudio -------> List<ParticipantInfo>: $it")
            for (participants in it)
                //if (user == participants.contact.username)
                Log.w("Conference.kt", "DEBUG hasActiveAudio -------> ${participants.contact.displayName}, local audio muted : ${participants.audioLocalMuted}, moderator audio muted: ${participants.audioModeratorMuted}")
        }
    }

    val timestampStart: Long
        get() {
            var t = Long.MAX_VALUE
            for (call in mParticipants) t = min(call.timestamp, t)
            return t
        }

    fun removeParticipants() {
        mParticipants.clear()
    }

    fun setInfo(info: List<ParticipantInfo>) {
        mParticipantInfo.onNext(info)
    }

    val participantInfo: Observable<List<ParticipantInfo>>
        get() = mParticipantInfo
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