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
package net.jami.model.interaction

import net.jami.model.Call
import net.jami.model.Call.Direction
import net.jami.model.Contact
import net.jami.model.ConversationHistory
import net.jami.utils.Log
import java.util.*

class CallHistory : Interaction {
    override val daemonIdString: String?
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

    var contactNumber: String? = null
        private set

    var confId: String? = null
    var hostDevice: String? = null
    var hostUri: String? = null

    constructor(call: Call) {
        account = call.account
        daemonIdString = call.id
        try {
            daemonId = call.id?.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Can't parse CallId $daemonId")
        }
        isIncoming = call.isIncoming
        timestamp = call.timestamp
        type = InteractionType.CALL
        conversation = call.conversation
        contact = call.contact
        //duration = call.duration
        isMissed = duration == 0L
    }

    constructor(
        daemonId: String?,
        author: String?,
        account: String?,
        conversation: ConversationHistory?,
        contact: Contact?,
        direction: Direction,
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
        type = InteractionType.CALL
        this.contact = contact
        mIsRead = 1
    }

    constructor(interaction: Interaction) {
        id = interaction.id
        author = interaction.author
        conversation = interaction.conversation
        isIncoming = author != null
        timestamp = interaction.timestamp
        type = InteractionType.CALL
        status = interaction.status
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
        type = InteractionType.CALL
        mIsRead = 1
    }

    val isConferenceParticipant: Boolean
        get() = confId != null

    val isGroupCall: Boolean
        get() = isConferenceParticipant && duration == 0L

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

    fun setEnded(end: CallHistory) {
        duration = end.duration
    }

    companion object {
        val TAG = CallHistory::class.simpleName!!
        const val KEY_HAS_VIDEO = "HAS_VIDEO"
        const val KEY_DURATION = "duration"
    }
}