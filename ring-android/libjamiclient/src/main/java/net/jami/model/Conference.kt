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
import net.jami.model.Call.CallStatus
import net.jami.model.Call.CallStatus.Companion.fromConferenceString
import java.util.*
import kotlin.math.min

class Conference {
    class ParticipantInfo(val call: Call?, val contact: Contact, i: Map<String, String>) {
        var x: Int = i["x"]?.toInt() ?: 0
        var y: Int = i["y"]?.toInt() ?: 0
        var w: Int = i["w"]?.toInt() ?: 0
        var h: Int = i["h"]?.toInt() ?: 0
        var videoMuted: Boolean = java.lang.Boolean.parseBoolean(i["videoMuted"])
        var audioMuted: Boolean = java.lang.Boolean.parseBoolean(i["audioMuted"])
        var isModerator: Boolean = java.lang.Boolean.parseBoolean(i["isModerator"])
        val isEmpty: Boolean
            get() = x == 0 && y == 0 && w == 0 && h == 0
    }

    private val mParticipantInfo: Subject<List<ParticipantInfo>> = BehaviorSubject.createDefault(emptyList())
    private val mParticipantRecordingSet: MutableSet<Contact> = HashSet()
    private val mParticipantRecording: Subject<Set<Contact>> = BehaviorSubject.createDefault(emptySet())
    val id: String
    private var mConfState: CallStatus? = null
    private val mParticipants: ArrayList<Call>
    private var mRecording: Boolean
    var maximizedParticipant: Contact? = null
    var isModerator = false

    constructor(call: Call) : this(call.daemonIdString!!) {
        mParticipants.add(call)
    }

    constructor(cID: String) {
        id = cID
        mParticipants = ArrayList()
        mRecording = false
    }

    constructor(c: Conference) {
        id = c.id
        mConfState = c.mConfState
        mParticipants = ArrayList(c.mParticipants)
        mRecording = c.mRecording
    }

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

    fun setState(state: String?) {
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

    operator fun contains(callID: String?): Boolean {
        for (participant in mParticipants) {
            if (participant.daemonIdString.contentEquals(callID)) return true
        }
        return false
    }

    fun getCallById(callID: String?): Call? {
        for (participant in mParticipants) {
            if (participant.daemonIdString.contentEquals(callID)) return participant
        }
        return null
    }

    fun findCallByContact(uri: Uri): Call? {
        for (call in mParticipants) {
            if (call.contact!!.uri.toString() == uri.toString()) return call
        }
        return null
    }

    val isIncoming: Boolean
        get() = mParticipants.size == 1 && mParticipants[0].isIncoming
    val isOnGoing: Boolean
        get() = mParticipants.size == 1 && mParticipants[0].isOnGoing || mParticipants.size > 1

    fun hasVideo(): Boolean {
        for (call in mParticipants) if (!call.isAudioOnly) return true
        return false
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