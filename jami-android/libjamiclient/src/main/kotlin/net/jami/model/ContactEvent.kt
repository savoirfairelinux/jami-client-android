/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

class ContactEvent : Interaction {
    var request: TrustRequest? = null
    var event: Event

    constructor(interaction: Interaction) {
        id = interaction.id
        conversation = interaction.conversation
        author = interaction.author
        mType = InteractionType.CONTACT.toString()
        timestamp = interaction.timestamp
        mStatus = interaction.status.toString()
        mIsRead = 1
        contact = interaction.contact
        event = getEventFromStatus(interaction.status)
    }

    constructor() {
        author = null
        event = Event.ADDED
        mType = InteractionType.CONTACT.toString()
        timestamp = System.currentTimeMillis()
        mStatus = InteractionStatus.SUCCESS.toString()
        mIsRead = 1
    }

    constructor(accountId: String, contact: Contact) {
        this.contact = contact
        account = accountId
        author = contact.uri.uri
        mType = InteractionType.CONTACT.toString()
        event = Event.ADDED
        mStatus = InteractionStatus.SUCCESS.toString()
        timestamp = contact.addedDate!!.time
        mIsRead = 1
    }

    constructor(accountId: String, contact: Contact, request: TrustRequest) {
        account = accountId
        this.request = request
        this.contact = contact
        author = contact.uri.uri
        timestamp = request.timestamp
        mType = InteractionType.CONTACT.toString()
        event = Event.INCOMING_REQUEST
        mStatus = InteractionStatus.UNKNOWN.toString()
        mIsRead = 1
    }

    enum class Event {
        UNKNOWN, INCOMING_REQUEST, INVITED, ADDED, REMOVED, BANNED;

        companion object {
            fun fromConversationAction(action: String): Event = when (action) {
                "add" -> INVITED
                "join" -> ADDED
                "remove" -> REMOVED
                "ban" -> BANNED
                else -> UNKNOWN
            }
        }
    }

    fun setEvent(event: Event): ContactEvent {
        this.event = event
        return this
    }

    private fun getEventFromStatus(status: InteractionStatus): Event {
        // success for added contacts
        if (status === InteractionStatus.SUCCESS) return Event.ADDED else if (status === InteractionStatus.UNKNOWN) return Event.INCOMING_REQUEST
        return Event.UNKNOWN
    }
}