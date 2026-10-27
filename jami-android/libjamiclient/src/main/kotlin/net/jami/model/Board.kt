/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package net.jami.model

/**
 * A Board is a daemon Feed: a private swarm published by its owner to the contacts
 * it authorizes, who discover it in the owner's catalogue and subscribe explicitly.
 */
data class Board(
    val id: String,
    val accountId: String,
    val title: String,
    val avatar: String = "",
    val ownerUri: String = "",
    val ownerName: String = "",
    val owned: Boolean = false,
    val allowReplies: Boolean = false,
    val closed: Boolean = false,
    /** Contacts allowed to subscribe. Only known by the owner. */
    val authorizedUris: Set<String> = emptySet(),
    val subscribed: Boolean = false,
    val requested: Boolean = false,
    val available: Boolean = false,
    val unreadCount: Int = 0
) {
    val uri: Uri get() = Uri(Uri.SWARM_SCHEME, id)

    /** Owned or subscribed: its conversation exists locally and can be opened. */
    val isFollowed: Boolean get() = !closed && available && (owned || subscribed)

    /** Offered by a contact, not subscribed yet. */
    val isOffered: Boolean get() = !closed && available && !owned && !subscribed

    companion object {
        /** [defaultTitle] is the (localized) title shown when the Feed has none. */
        fun fromDaemon(
            accountId: String,
            info: Map<String, String>,
            defaultTitle: String,
            ownerName: String = "",
            unreadCount: Int = 0
        ): Board? {
            val id = info["id"]?.takeIf { it.isNotBlank() } ?: return null
            return Board(
                id = id,
                accountId = accountId,
                title = info["title"]?.takeIf { it.isNotBlank() } ?: defaultTitle,
                avatar = info["avatar"].orEmpty(),
                ownerUri = info["feedOwner"].orEmpty(),
                ownerName = ownerName,
                owned = info["owned"] == "true",
                allowReplies = info["feedReplies"] == "true",
                closed = info["feedClosed"] == "true",
                authorizedUris = info["feedAccess"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                subscribed = info["subscribed"] == "true",
                requested = info["requested"] == "true",
                available = info["available"] == "true",
                unreadCount = unreadCount
            )
        }
    }
}
