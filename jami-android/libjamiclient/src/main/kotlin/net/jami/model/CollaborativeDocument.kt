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
package net.jami.model

/**
 * A collaborative document announced in a conversation. The daemon only ever
 * describes a document; its content lives in a Y-CRDT replica the client owns.
 */
data class CollaborativeDocument(
    val id: String,
    val name: String,
    val mimeType: String,
    val author: String?,
    val timestamp: Long,
) {
    val isRichText: Boolean
        get() = mimeType == MIME_RICH_TEXT

    companion object {
        /** The media type every editor-backed document uses. */
        const val MIME_RICH_TEXT = "text/html"

        /** The media type the daemon falls back to when none is given. */
        const val MIME_PLAIN_TEXT = "text/plain"

        /**
         * Build a document from a COLLAB_DOC commit map, as returned by
         * `getCollaborativeDocuments`. The daemon spells the document id "uri",
         * since the map is the announcing commit itself.
         */
        fun fromNative(map: Map<String, String>): CollaborativeDocument? {
            val id = map["uri"]?.takeIf { it.isNotEmpty() } ?: return null
            return CollaborativeDocument(
                id = id,
                name = map["displayName"].orEmpty(),
                mimeType = map["mimeType"]?.takeIf { it.isNotEmpty() } ?: MIME_PLAIN_TEXT,
                author = map["author"]?.takeIf { it.isNotEmpty() },
                timestamp = map["timestamp"]?.toLongOrNull() ?: 0L,
            )
        }
    }
}

/**
 * One checkpoint in a document's history: a commit gathering a batch of updates.
 */
data class CollaborativeVersion(
    val commitId: String,
    val author: String,
    val device: String,
    val timestamp: Long,
    val deltas: Int,
) {
    companion object {
        fun fromNative(map: Map<String, String>): CollaborativeVersion? {
            val commitId = map["id"]?.takeIf { it.isNotEmpty() } ?: return null
            return CollaborativeVersion(
                commitId = commitId,
                author = map["author"].orEmpty(),
                device = map["device"].orEmpty(),
                timestamp = map["timestamp"]?.toLongOrNull() ?: 0L,
                deltas = map["deltas"]?.toIntOrNull() ?: 0,
            )
        }
    }
}
