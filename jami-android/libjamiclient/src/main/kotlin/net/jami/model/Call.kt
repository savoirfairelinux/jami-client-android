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
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Contact
import net.jami.model.ConversationHistory
import net.jami.model.Media
import net.jami.model.interaction.Interaction
import net.jami.services.CallService
import net.jami.utils.Log
import java.util.*

class Call(
    val account: String,
    val id: String?,
    val peerUri: Uri,
    val isIncoming: Boolean,
    val conversationUri: Uri? = null
) {
    private var isPeerHolding = false
    var isAudioMuted = false
    var isVideoMuted = false
    private val isRecording = false
    var callStatus = CallStatus.NONE
        private set

    var timestamp: Long = 0
    var timestampEnd: Long = 0

    var contact: Contact? = null

    var isMissed = true
        private set

    var audioCodec: String? = null
        private set

    var videoCodec: String? = null
        private set

    var confId: String? = null

    val mediaList = ArrayList<Media>()
    private val mediaListSubject: Subject<List<Media>> = BehaviorSubject.createDefault(mediaList)
    val mediaListObservable: Observable<List<Media>>
        get() = mediaListSubject

    fun setMediaList(media: List<Media>) {
        Log.w("devdebug", "setMediaList: $media")
        mediaList.clear()
        mediaList.addAll(media)
        mediaListSubject.onNext(mediaList)
    }

    private val systemConnectionSubject: SingleSubject<CallService.SystemCall> =
        SingleSubject.create()
    val systemConnection: Single<CallService.SystemCall> get() = systemConnectionSubject

    fun setSystemConnection(value: CallService.SystemCall?) {

//        (value as CallServiceImpl.AndroidCall).connection!!.audioState
        Log.i("devdebug", "Telecom API: setSystemConnection $value")
        if (value != null) systemConnectionSubject.onSuccess(value);
        else systemConnectionSubject.onError(UnsupportedOperationException())
    }

    constructor(
        daemonId: String?,
        author: String?,
        account: String,
        conversation: Conversation?,
        contact: Contact,
        direction: Direction,
    ) : this(account, daemonId, contact.uri, direction == Direction.INCOMING, conversation?.uri) {
        timestamp = System.currentTimeMillis()
        this.contact = contact
    }

    constructor(daemonId: String?, account: String, contactNumber: String, direction: Direction, timestamp: Long)
            : this(account, daemonId, Uri.fromString(contactNumber), direction == Direction.INCOMING) {
        this.timestamp = timestamp
    }

    constructor(daemonId: String?, callDetails: Map<String, String>) : this(
        daemonId,
        callDetails[KEY_ACCOUNT_ID]!!,
        callDetails[KEY_PEER_NUMBER]!!,
        Direction.fromInt(callDetails[KEY_CALL_TYPE]!!.toInt()),
        System.currentTimeMillis())
    {
        setCallState(CallStatus.fromString(callDetails[KEY_CALL_STATE]!!))
        setDetails(callDetails)
    }

    fun setDetails(details: Map<String, String>) {
        isPeerHolding = details[KEY_PEER_HOLDING].toBoolean()
        isAudioMuted = details[KEY_AUDIO_MUTED].toBoolean()
        isVideoMuted = details[KEY_VIDEO_MUTED].toBoolean()
        audioCodec = details[KEY_AUDIO_CODEC]
        videoCodec = details[KEY_VIDEO_CODEC]
        confId = details[KEY_CONF_ID]?.ifEmpty { null }
    }
    val isConferenceParticipant: Boolean
        get() = confId != null

    val isGroupCall: Boolean
        get() = isConferenceParticipant// && duration == 0L

    fun setCallState(callStatus: CallStatus) {
        this.callStatus = callStatus
        if (callStatus == CallStatus.CURRENT) {
            isMissed = false
        }
    }

    val isRinging: Boolean
        get() = callStatus.isRinging
    val isOnGoing: Boolean
        get() = callStatus.isOnGoing

    fun hasMedia(label: Media.MediaType): Boolean {
        for (media in mediaList) {
            // todo check -> isEnabled est il utile ? Si le media n'est pas activé alors le daemon ne nous le transmets pas ?
            if (media.isEnabled && media.mediaType == label) {
                return true
            }
        }
        return false
    }
    fun hasActiveMedia(label: Media.MediaType): Boolean {
        for (media in mediaList) {
            if (media.isEnabled && !media.isMuted && media.mediaType == label)
                return true
        }
        return false
    }

    fun hasActiveScreenSharing(): Boolean {
        for (media in mediaList) {
            if (media.source == "camera://desktop" && media.isEnabled && !media.isMuted)
                return true
        }
        return false
    }

    enum class CallStatus {
        NONE, SEARCHING, CONNECTING, RINGING, CURRENT, HUNGUP, BUSY, FAILURE, HOLD, UNHOLD, INACTIVE, OVER;

        val isRinging: Boolean
            get() = this == CONNECTING || this == RINGING || this == NONE || this == SEARCHING
        val isOnGoing: Boolean
            get() = this == CURRENT || this == HOLD || this == UNHOLD
        val isOver: Boolean
            get() = this == HUNGUP || this == BUSY || this == FAILURE || this == OVER

        companion object {
            fun fromString(state: String): CallStatus {
                return when (state) {
                    "SEARCHING" -> SEARCHING
                    "CONNECTING" -> CONNECTING
                    "INCOMING", "RINGING" -> RINGING
                    "CURRENT" -> CURRENT
                    "HUNGUP" -> HUNGUP
                    "BUSY" -> BUSY
                    "FAILURE" -> FAILURE
                    "HOLD" -> HOLD
                    "UNHOLD" -> UNHOLD
                    "INACTIVE" -> INACTIVE
                    "OVER" -> OVER
                    "NONE" -> NONE
                    else -> NONE
                }
            }

            fun fromConferenceString(state: String): CallStatus {
                return when (state) {
                    "ACTIVE_ATTACHED" -> CURRENT
                    "ACTIVE_DETACHED", "HOLD" -> HOLD
                    else -> NONE
                }
            }
        }
    }

    enum class Direction(val value: Int) {
        INCOMING(0), OUTGOING(1);

        companion object {
            fun fromInt(value: Int): Direction {
                return if (value == INCOMING.value) INCOMING else OUTGOING
            }
        }
    }

    companion object {
        val TAG = Call::class.simpleName!!
        const val KEY_ACCOUNT_ID = "ACCOUNTID"
        const val KEY_HAS_VIDEO = "HAS_VIDEO"
        const val KEY_CALL_TYPE = "CALL_TYPE"
        const val KEY_CALL_STATE = "CALL_STATE"
        const val KEY_PEER_NUMBER = "PEER_NUMBER"
        const val KEY_PEER_HOLDING = "PEER_HOLDING"
        const val KEY_AUDIO_MUTED = "AUDIO_MUTED"
        const val KEY_VIDEO_MUTED = "VIDEO_MUTED"
        const val KEY_AUDIO_CODEC = "AUDIO_CODEC"
        const val KEY_VIDEO_CODEC = "VIDEO_CODEC"
        const val KEY_REGISTERED_NAME = "REGISTERED_NAME"
        const val KEY_DURATION = "duration"
        const val KEY_CONF_ID = "CONF_ID"
    }
}