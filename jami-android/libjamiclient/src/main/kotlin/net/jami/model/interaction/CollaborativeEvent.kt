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
 * Real-time events emitted by the daemon for a collaborative document, forwarded to
 * the open editor. All variants carry the (account, conversation, document) identity
 * so the editor can filter the ones it is displaying.
 */
sealed class CollaborativeEvent {
    abstract val accountId: String
    abstract val conversationId: String
    abstract val documentId: String

    /** A remote plain-text edit: replace [deleteLen] UTF-16 units at [index] with [insert]. */
    data class TextChange(
        override val accountId: String,
        override val conversationId: String,
        override val documentId: String,
        val index: Int,
        val deleteLen: Int,
        val insert: String,
    ) : CollaborativeEvent()

    /** A remote rich-text delta (Quill-delta JSON). */
    data class Delta(
        override val accountId: String,
        override val conversationId: String,
        override val documentId: String,
        val deltaJson: String,
    ) : CollaborativeEvent()

    /** A remote peer moved its cursor/selection. */
    data class Cursor(
        override val accountId: String,
        override val conversationId: String,
        override val documentId: String,
        val peerId: String,
        val position: Int,
        val anchor: Int,
    ) : CollaborativeEvent()

    /** A remote peer closed the document. */
    data class ParticipantLeft(
        override val accountId: String,
        override val conversationId: String,
        override val documentId: String,
        val peerId: String,
    ) : CollaborativeEvent()

    /** The document was renamed. */
    data class Renamed(
        override val accountId: String,
        override val conversationId: String,
        override val documentId: String,
        val name: String,
    ) : CollaborativeEvent()
}
