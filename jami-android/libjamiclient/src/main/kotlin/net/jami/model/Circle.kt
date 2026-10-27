/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package net.jami.model

data class Circle(
    val id: String,
    val name: String,
    val members: Set<String> = emptySet(),
    val builtIn: Boolean = false,
    val groups: Set<String> = emptySet(),
) {
    val isAllContacts: Boolean get() = id == CircleMetadata.ALL_ID

    val totalCount: Int get() = members.size + groups.size

    fun containsContact(uri: String): Boolean = isAllContacts || uri in members

    fun contains(conversation: Conversation): Boolean {
        if (isAllContacts) return true
        if (conversation.isSwarmGroup()) return conversation.uri.rawUriString in groups
        val others = conversation.contacts.filter { !it.isUser }
        return others.isNotEmpty() && others.all {
            it.uri.rawUriString in members || it.uri.rawRingId in members
        }
    }
}
