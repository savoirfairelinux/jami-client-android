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
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Interaction
import net.jami.model.Uri

class SmartListViewModel {
    val accountId: String
    val uri: Uri
    val contacts: List<Contact>
    val uuid: String?
    val contactName: String?
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

    constructor(accountId: String, contact: Contact, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        uri = contact.uri
        uuid = uri.rawUriString
        contactName = contact.displayName
        hasUnreadTextMessage = lastEvent != null && !lastEvent.isRead
        hasOngoingCall = false
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.isOnline
        headerTitle = Title.None
    }

    constructor(accountId: String, contact: Contact, id: String?, lastEvent: Interaction?) {
        this.accountId = accountId
        contacts = listOf(contact)
        uri = contact.uri
        uuid = id
        contactName = contact.displayName
        hasUnreadTextMessage = lastEvent != null && !lastEvent.isRead
        hasOngoingCall = false
        this.lastEvent = lastEvent
        showPresence = true
        isOnline = contact.isOnline
        headerTitle = Title.None
    }

    constructor(conversation: Conversation, contacts: List<Contact>, presence: Boolean) {
        accountId = conversation.accountId
        this.contacts = contacts
        uri = conversation.uri
        uuid = uri.rawUriString
        contactName = conversation.title
        val lastEvent = conversation.lastEvent
        hasUnreadTextMessage = conversation.hasUnreadTextMessage
        hasOngoingCall = false
        this.lastEvent = lastEvent
        selected = conversation.getVisible()
        for (contact in contacts) {
            if (contact.isUser) continue
            if (contact.isOnline) {
                isOnline = true
                break
            }
        }
        showPresence = presence
        headerTitle = Title.None
    }

    constructor(conversation: Conversation, presence: Boolean) : this(conversation, conversation.contacts, presence) {}

    private constructor(title: Title) {
        contactName = null
        accountId = ""
        contacts = emptyList()
        uuid = null
        uri = Uri()
        hasUnreadTextMessage = false
        lastEvent = null
        showPresence = false
        headerTitle = title
    }

    val isSwarm: Boolean
        get() = uri.isSwarm

    /**
     * Used to get contact for one to one or legacy conversations
     */
    fun getContact(): Contact? {
        if (contacts.size == 1) return contacts[0]
        for (c in contacts) {
            if (!c.isUser) return c
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
        if (other !is SmartListViewModel) return false
        return (other.headerTitle == headerTitle
                && (headerTitle != Title.None
                || (contacts === other.contacts && contactName == other.contactName
                && isOnline == other.isOnline && lastEvent === other.lastEvent && hasOngoingCall == other.hasOngoingCall && hasUnreadTextMessage == other.hasUnreadTextMessage)))
    }

    companion object {
        val TITLE_CONVERSATIONS: Observable<SmartListViewModel> = Observable.just(SmartListViewModel(Title.Conversations))
        val TITLE_PUBLIC_DIR: Observable<SmartListViewModel> = Observable.just(SmartListViewModel(Title.PublicDirectory))
        val EMPTY_LIST: Single<List<Observable<SmartListViewModel>>> = Single.just(emptyList())
        @JvmStatic
        val EMPTY_RESULTS: Observable<List<SmartListViewModel>> = Observable.just(emptyList())
    }
}