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

/**
 * A collaborative (real-time editable) document announced in a swarm conversation.
 * The CRDT state lives in the daemon; this interaction is only the chat-bubble entry
 * that lets a member open the live editor.
 */
class CollaborativeDocument : Interaction {
    var documentId: String = ""
        private set
    var name: String = ""
    var kind: String = KIND_TEXT
        private set

    constructor(
        accountId: String,
        author: String?,
        documentId: String,
        name: String,
        kind: String,
        isIncoming: Boolean,
        timestamp: Long
    ) {
        account = accountId
        this.author = author
        this.documentId = documentId
        this.name = name
        this.kind = if (kind.isEmpty()) KIND_TEXT else kind
        body = name
        this.isIncoming = isIncoming
        this.timestamp = timestamp
        type = InteractionType.COLLAB_DOC
        mIsRead = 1
    }

    val displayName: String
        get() = name.ifEmpty { body ?: "" }

    val isRich: Boolean
        get() = kind == KIND_RICH

    companion object {
        const val KIND_TEXT = "text"
        const val KIND_RICH = "rich"
    }
}
