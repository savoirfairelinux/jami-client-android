/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jami.model

import net.jami.utils.StringUtils.isEmpty
import net.jami.utils.ProfileChunk
import ezvcard.VCard
import net.jami.utils.VCardUtils
import ezvcard.Ezvcard
import net.jami.utils.Log
import java.lang.Exception
import java.util.*

class Call : Interaction {
    override val daemonIdString: String?
    private var isPeerHolding = false
    var isAudioMuted = false
        private set
    private var isVideoMuted = false
    private val isRecording = false
    var isAudioOnly = false
        private set
    var callStatus = CallStatus.NONE
        private set
    var timestampEnd: Long = 0
        set(timestampEnd) {
            field = timestampEnd
            if (timestampEnd != 0L && !isMissed) duration = timestampEnd - timestamp
        }
    var duration: Long? = null
        get() {
            if (field == null) {
                val element = toJson(mExtraFlag)[KEY_DURATION]
                if (element != null) {
                    field = element.asLong
                }
            }
            return if (field == null) 0 else field
        }
        set(value) {
            if (value == duration) return
            field = value
            if (duration != null && duration != 0L) {
                val jsonObject = extraFlag
                jsonObject.addProperty(KEY_DURATION, value)
                mExtraFlag = fromJson(jsonObject)
                isMissed = false
            }
        }
    var isMissed = true
        private set
    var audioCodec: String? = null
        private set
    var videoCodec: String? = null
        private set
    var contactNumber: String? = null
        private set
    var confId: String? = null
    private var mProfileChunk: ProfileChunk? = null

    constructor(
        daemonId: String?,
        author: String?,
        account: String?,
        conversation: ConversationHistory?,
        contact: Contact?,
        direction: Direction
    ) {
        daemonIdString = daemonId
        try {
            this.daemonId = daemonId?.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Can't parse CallId $daemonId")
        }
        this.author = if (direction == Direction.INCOMING) author else null
        this.account = account
        this.conversation = conversation
        isIncoming = direction == Direction.INCOMING
        timestamp = System.currentTimeMillis()
        mType = InteractionType.CALL.toString()
        this.contact = contact
        mIsRead = 1
    }

    constructor(interaction: Interaction) {
        id = interaction.id
        author = interaction.author
        conversation = interaction.conversation
        isIncoming = author != null
        timestamp = interaction.timestamp
        mType = InteractionType.CALL.toString()
        mStatus = interaction.status.toString()
        daemonId = interaction.daemonId
        daemonIdString = super.daemonIdString
        mIsRead = if (interaction.isRead) 1 else 0
        account = interaction.account
        mExtraFlag = fromJson(interaction.extraFlag)
        isMissed = duration == 0L
        mIsRead = 1
        contact = interaction.contact
    }

    constructor(daemonId: String?, account: String?, contactNumber: String?, direction: Direction, timestamp: Long) {
        daemonIdString = daemonId
        try {
            this.daemonId = daemonId?.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Can't parse CallId $daemonId")
        }
        isIncoming = direction == Direction.INCOMING
        this.account = account
        author = if (direction == Direction.INCOMING) contactNumber else null
        this.contactNumber = contactNumber
        this.timestamp = timestamp
        mType = InteractionType.CALL.toString()
        mIsRead = 1
    }

    constructor(daemonId: String?, call_details: Map<String, String>) : this(
        daemonId,
        call_details[KEY_ACCOUNT_ID],
        call_details[KEY_PEER_NUMBER],
        Direction.fromInt(call_details[KEY_CALL_TYPE]!!.toInt()),
        System.currentTimeMillis())
    {
        setCallState(CallStatus.fromString(call_details[KEY_CALL_STATE]!!))
        setDetails(call_details)
    }

    fun setDetails(details: Map<String, String>) {
        isPeerHolding = "true" == details[KEY_PEER_HOLDING]
        isAudioMuted = "true" == details[KEY_AUDIO_MUTED]
        isVideoMuted = "true" == details[KEY_VIDEO_MUTED]
        isAudioOnly = "true" == details[KEY_AUDIO_ONLY]
        audioCodec = details[KEY_AUDIO_CODEC]
        videoCodec = details[KEY_VIDEO_CODEC]
        val confId = details[KEY_CONF_ID]
        this.confId = if (isEmpty(confId)) null else confId
    }

    val isConferenceParticipant: Boolean
        get() = confId != null

    val durationString: String
        get() {
            val mDuration = duration!! / 1000
            if (mDuration < 60) {
                return String.format(Locale.getDefault(), "%02d secs", mDuration)
            }
            return if (mDuration < 3600)
                String.format(Locale.getDefault(), "%02d mins %02d secs", mDuration % 3600 / 60, mDuration % 60)
            else
                String.format(Locale.getDefault(), "%d h %02d mins %02d secs", mDuration / 3600, mDuration % 3600 / 60, mDuration % 60)
        }

    fun muteVideo(mute: Boolean) {
        isVideoMuted = mute
    }

    fun muteAudio(mute: Boolean) {
        isAudioMuted = mute
    }

    fun setCallState(callStatus: CallStatus) {
        this.callStatus = callStatus
        if (callStatus == CallStatus.CURRENT) {
            isMissed = false
            mStatus = InteractionStatus.SUCCESS.toString()
        } else if (isRinging || isOnGoing) {
            mStatus = InteractionStatus.SUCCESS.toString()
        } else if (this.callStatus == CallStatus.FAILURE) {
            mStatus = InteractionStatus.FAILURE.toString()
        }
    }

    /*override var timestamp: Long
        get() = super.timestamp
        set(timestamp) {
            var timestamp = timestamp
            timestamp = timestamp
        }*/
    val isRinging: Boolean
        get() = callStatus == CallStatus.CONNECTING || callStatus == CallStatus.RINGING || callStatus == CallStatus.NONE || callStatus == CallStatus.SEARCHING
    val isOnGoing: Boolean
        get() = callStatus == CallStatus.CURRENT || callStatus == CallStatus.HOLD || callStatus == CallStatus.UNHOLD
    /*override var isIncoming: Direction
        get() = super.isIncoming
        set(direction) {
            field = direction == Direction.INCOMING
        }*/

    fun appendToVCard(messages: Map<String, String>): VCard? {
        for ((key, value) in messages) {
            val messageKeyValue = VCardUtils.parseMimeAttributes(key)
            val mimeType = messageKeyValue[VCardUtils.VCARD_KEY_MIME_TYPE]
            if (VCardUtils.MIME_PROFILE_VCARD != mimeType) {
                continue
            }
            val part = messageKeyValue[VCardUtils.VCARD_KEY_PART]!!.toInt()
            val nbPart = messageKeyValue[VCardUtils.VCARD_KEY_OF]!!.toInt()
            if (null == mProfileChunk) {
                mProfileChunk = ProfileChunk(nbPart)
            }
            mProfileChunk?.let { profile ->
                profile.addPartAtIndex(value, part)
                if (profile.isProfileComplete) {
                    val ret = Ezvcard.parse(profile.completeProfile).first()
                    mProfileChunk = null
                    return@appendToVCard ret
                }
            }
        }
        return null
    }

    enum class CallStatus {
        NONE, SEARCHING, CONNECTING, RINGING, CURRENT, HUNGUP, BUSY, FAILURE, HOLD, UNHOLD, INACTIVE, OVER;

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
        const val KEY_AUDIO_ONLY = "AUDIO_ONLY"
        const val KEY_CALL_TYPE = "CALL_TYPE"
        const val KEY_CALL_STATE = "CALL_STATE"
        const val KEY_PEER_NUMBER = "PEER_NUMBER"
        const val KEY_PEER_HOLDING = "PEER_HOLDING"
        const val KEY_AUDIO_MUTED = "PEER_NUMBER"
        const val KEY_VIDEO_MUTED = "VIDEO_MUTED"
        const val KEY_AUDIO_CODEC = "AUDIO_CODEC"
        const val KEY_VIDEO_CODEC = "VIDEO_CODEC"
        const val KEY_REGISTERED_NAME = "REGISTERED_NAME"
        const val KEY_DURATION = "duration"
        const val KEY_CONF_ID = "CONF_ID"
    }
}