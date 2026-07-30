/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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

import net.jami.model.ConversationHistory

/**
 * The announcement of a document the conversation writes together.
 *
 * The commit carries no content: the document lives in its own repository, so
 * what is in the conversation is the fact that it exists. The name here is the
 * one it was created with; it can be renamed afterwards, and the current name
 * is asked of the daemon when the announcement is shown.
 */
class CollabDocument(
    author: String?,
    account: String,
    timestamp: Long,
    conversation: ConversationHistory?,
    val documentId: String,
    name: String,
    val mimeType: String,
    isIncoming: Boolean,
) : Interaction() {
    init {
        this.author = author
        this.account = account
        this.timestamp = timestamp
        this.conversation = conversation
        this.isIncoming = isIncoming
        type = InteractionType.COLLAB_DOC
        body = name
    }

    /** The name it was created with. */
    val name: String
        get() = body ?: ""
}
