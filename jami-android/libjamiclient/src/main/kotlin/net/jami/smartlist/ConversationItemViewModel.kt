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
package net.jami.smartlist

import io.reactivex.rxjava3.core.Observable
import net.jami.model.*
import kotlin.math.min

class ConversationItemViewModel(
    conversation: Conversation,
    val conversationProfile: Profile,
    val contacts: List<ContactViewModel>,
    val showPresence: Boolean
) {
    val accountId: String = conversation.accountId
    val uri: Uri = conversation.uri
    val mode: Conversation.Mode = conversation.mode.blockingFirst()
    val uuid: String = uri.rawUriString
    val title: String = getTitle(conversation, conversationProfile, contacts)
    val isOnline: Boolean = showPresence && contacts.firstOrNull { it.presence } != null
    var isChecked = false
    var selected: Observable<Boolean>? = conversation.getVisible()
        private set
    val lastEvent: Interaction? = conversation.lastEvent
    val request: TrustRequest? = conversation.request

    enum class Title {
        None, Conversations, PublicDirectory
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

    fun isGroup(): Boolean =
        // SwarmGroup is a conversation were there are multiple participants (not a 1:1
        // conversation). To know this, we need to check the conversation mode.
        // However, conversation mode can also be a request. In this case, we need to check the
        // request mode to know if it is a 1:1 conversation or not (request attribute).
        if (mode == Conversation.Mode.Request)
            request?.mode != Conversation.Mode.OneToOne
        else mode != Conversation.Mode.OneToOne

    override fun equals(other: Any?): Boolean {
        if (other !is ConversationItemViewModel) return false
        return contacts === other.contacts
                && title == other.title
                && isOnline == other.isOnline
                && lastEvent === other.lastEvent
    }

    companion object {
        private const val MAX_NAMES = 3

        fun getContact(contacts: List<ContactViewModel>) : ContactViewModel? =
            // Get user contact only if self-conversation.
            contacts.firstOrNull { !it.contact.isUser } ?: contacts.firstOrNull()

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
