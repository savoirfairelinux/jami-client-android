/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package net.jami.smartlist

import io.reactivex.rxjava3.core.Observable
import net.jami.model.*
import kotlin.math.min

class ConversationItemViewModel {
    val accountId: String
    val uri: Uri
    val mode: Conversation.Mode
    val contacts: List<ContactViewModel>
    val conversationProfile: Profile
    val uuid: String?
    val title: String
    private val showPresence: Boolean
    val isOnline: Boolean
    var isChecked = false
    var selected: Observable<Boolean>? = null
        private set
    val lastEvent: Interaction?

    enum class Title {
        None, Conversations, PublicDirectory
    }

    constructor(accountId: String, contact: ContactViewModel, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        this.conversationProfile = contact.profile
        uri = contact.contact.uri
        mode = Conversation.Mode.Legacy
        uuid = uri.rawUriString
        title = contact.displayName
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.contact.isOnline
    }

    constructor(accountId: String, contact: ContactViewModel, id: String?, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        this.conversationProfile = contact.profile
        uri = contact.contact.uri
        mode = Conversation.Mode.Legacy
        uuid = id
        title = contact.displayName
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.contact.isOnline
    }

    constructor(conversation: Conversation, conversationProfile: Profile, contacts: List<ContactViewModel>, presence: Boolean) {
        accountId = conversation.accountId
        this.contacts = contacts
        this.conversationProfile = conversationProfile
        uri = conversation.uri
        mode = conversation.mode.blockingFirst()
        uuid = uri.rawUriString
        title = getTitle(conversation, conversationProfile, contacts)
        val lastEvent = conversation.lastEvent
        this.lastEvent = lastEvent
        selected = conversation.getVisible()
        var online = false
        for (c in contacts) {
            if (c.contact.isUser) continue
            if (c.contact.isOnline) {
                online = true
                break
            }
        }
        isOnline = online
        showPresence = presence
    }

    val uriTitle: String
        get() = getUriTitle(uri, contacts)

    val isSwarm: Boolean
        get() = uri.isSwarm


    fun matches(query: String): Boolean {
        for (contact in contacts)
            if (contact.matches(query)) return true
        return false
    }


    /**
     * Used to get contact for one to one or legacy conversations
     */
    fun getContact(): ContactViewModel? {
        if (contacts.size == 1) return contacts[0]
        for (c in contacts) {
            if (!c.contact.isUser) return c
        }
        return if (contacts.isNotEmpty()) contacts[0] else null
    }

    fun showPresence(): Boolean {
        return showPresence
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ConversationItemViewModel) return false
        return contacts === other.contacts
                && title == other.title
                && isOnline == other.isOnline
                && lastEvent === other.lastEvent
    }

    companion object {
        private const val MAX_NAMES = 3

        fun getContact(contacts: List<ContactViewModel>) : ContactViewModel? {
            for (contact in contacts)
                if (!contact.contact.isUser)
                    return contact
            return null
        }

        fun getTitle(conversation: Conversation, profile: Profile, contacts: List<ContactViewModel>): String {
            if (!profile.displayName.isNullOrBlank())
                return profile.displayName
            if (contacts.isEmpty()) {
                return if (conversation.mode.blockingFirst() == Conversation.Mode.Syncing) { "(Syncing)" } else ""
            } else if (contacts.size == 1) {
                return contacts[0].displayName
            }
            val names = ArrayList<String>(min(contacts.size, MAX_NAMES))
            var target = contacts.size
            for (c in contacts) {
                if (c.contact.isUser) {
                    target--
                    continue
                }
                val displayName = c.displayName
                if (displayName.isNotEmpty()) {
                    names.add(displayName)
                    if (names.size == MAX_NAMES) break
                }
            }
            val ret = StringBuilder()
            names.joinTo(ret)
            if (names.isNotEmpty() && names.size < target) {
                ret.append(" + ").append(contacts.size - names.size)
            }
            return ret.toString().ifEmpty { conversation.uri.rawUriString }
        }

        fun getUriTitle(conversationUri: Uri, contacts: List<ContactViewModel>): String {
            if (contacts.isEmpty()) {
                return conversationUri.rawUriString
            } else if (contacts.size == 1) {
                return contacts[0].displayUri
            }
            val names = ArrayList<String>(contacts.size)
            for (c in contacts) {
                if (c.contact.isUser) continue
                names.add(c.displayUri)
            }
            return names.joinToString().ifEmpty { conversationUri.rawUriString }
        }
    }
}
