/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import io.reactivex.rxjava3.core.Single
import net.jami.model.*

class ConversationItemViewModel {
    val accountId: String
    val uri: Uri
    val mode: Conversation.Mode
    val contacts: List<ContactViewModel>
    val uuid: String?
    val contactName: String
    private val hasUnreadTextMessage: Boolean
    private var hasOngoingCall = false
    private val showPresence: Boolean
    var isOnline = false
        private set
    var isChecked = false
    var selected: Observable<Boolean>? = null
        private set
    val lastEvent: Interaction?

    enum class Title {
        None, Conversations, PublicDirectory
    }

    val headerTitle: Title

    constructor(accountId: String, contact: ContactViewModel, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        uri = contact.contact.uri
        mode = Conversation.Mode.Legacy
        uuid = uri.rawUriString
        contactName = contact.displayName
        hasUnreadTextMessage = lastEvent != null && !lastEvent.isRead
        hasOngoingCall = false
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.contact.isOnline
        headerTitle = Title.None
    }

    constructor(accountId: String, contact: ContactViewModel, id: String?, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        uri = contact.contact.uri
        mode = Conversation.Mode.Legacy
        uuid = id
        contactName = contact.displayName
        hasUnreadTextMessage = lastEvent != null && !lastEvent.isRead
        hasOngoingCall = false
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.contact.isOnline
        headerTitle = Title.None
    }

    constructor(conversation: Conversation, contacts: List<ContactViewModel>, presence: Boolean) {
        accountId = conversation.accountId
        this.contacts = contacts
        uri = conversation.uri
        mode = conversation.mode.blockingFirst()
        uuid = uri.rawUriString
        contactName = getTitle(conversation, contacts)
        val lastEvent = conversation.lastEvent
        hasUnreadTextMessage = lastEvent != null && !lastEvent.isRead
        hasOngoingCall = false
        this.lastEvent = lastEvent
        selected = conversation.getVisible()
        for (c in contacts) {
            if (c.contact.isUser) continue
            if (c.contact.isOnline) {
                isOnline = true
                break
            }
        }
        showPresence = presence
        headerTitle = Title.None
    }

    //constructor(conversation: Conversation, presence: Boolean) : this(conversation, conversation.contacts, presence)

    private constructor(title: Title) {
        contactName = ""
        accountId = ""
        contacts = emptyList()
        uuid = null
        uri = Uri()
        mode = Conversation.Mode.Legacy
        hasUnreadTextMessage = false
        lastEvent = null
        showPresence = false
        headerTitle = title
    }

    val uriTitle: String?
        get() = getUriTitle(uri, contacts)

    val isSwarm: Boolean
        get() = uri.isSwarm

    /**
     * Used to get contact for one to one or legacy conversations
     */
    fun getContact(): ContactViewModel? {
        if (contacts.size == 1) return contacts[0]
        for (c in contacts) {
            if (!c.contact.isUser) return c
        }
        return null
    }

    val lastInteractionTime: Long
        get() = lastEvent?.timestamp ?: 0

    fun hasUnreadTextMessage(): Boolean {
        return hasUnreadTextMessage
    }

    fun hasOngoingCall(): Boolean {
        return hasOngoingCall
    }

    /*public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        if (showPresence)
            isOnline = online;
    }*/
    fun showPresence(): Boolean {
        return showPresence
    }

    fun setHasOngoingCall(hasOngoingCall: Boolean) {
        this.hasOngoingCall = hasOngoingCall
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ConversationItemViewModel) return false
        return (other.headerTitle == headerTitle
                && (headerTitle != Title.None
                || (contacts === other.contacts && contactName == other.contactName
                && isOnline == other.isOnline && lastEvent === other.lastEvent && hasOngoingCall == other.hasOngoingCall && hasUnreadTextMessage == other.hasUnreadTextMessage)))
    }

    companion object {
        val TITLE_CONVERSATIONS: Observable<ConversationItemViewModel> = Observable.just(ConversationItemViewModel(Title.Conversations))
        val TITLE_PUBLIC_DIR: Observable<ConversationItemViewModel> = Observable.just(ConversationItemViewModel(Title.PublicDirectory))
        val EMPTY_LIST: Single<List<Observable<ConversationItemViewModel>>> = Single.just(emptyList())
        val EMPTY_RESULTS: Observable<MutableList<ConversationItemViewModel>> = Observable.just(ArrayList())

        fun getContact(contacts: List<ContactViewModel>) : ContactViewModel? {
            for (contact in contacts)
                if (!contact.contact.isUser)
                    return contact
            return null
        }

        fun getTitle(conversation: Conversation, contacts: List<ContactViewModel>): String  {
            if (contacts.isEmpty()) {
                return if (conversation.mode.blockingFirst() == Conversation.Mode.Syncing) { "(Syncing)" } else ""
            } else if (contacts.size == 1) {
                return contacts[0].displayName
            }
            val names = ArrayList<String>(contacts.size)
            var target = contacts.size
            for (c in contacts) {
                if (c.contact.isUser) {
                    target--
                    continue
                }
                val displayName = c.displayName
                if (displayName.isNotEmpty()) {
                    names.add(displayName)
                    if (names.size == 3) break
                }
            }
            val ret = StringBuilder()
            names.joinTo(ret)
            if (names.isNotEmpty() && names.size < target) {
                ret.append(" + ").append(contacts.size - names.size)
            }
            return ret.toString().ifEmpty { conversation.uri.rawUriString }
        }

        fun getUriTitle(conversationUri: Uri, contacts: List<ContactViewModel>): String? {
            if (contacts.isEmpty()) {
                return null
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
