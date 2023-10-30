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
package cx.ring.utils

import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutManagerCompat
import cx.ring.BuildConfig
import net.jami.model.Conversation
import net.jami.model.Interaction
import net.jami.model.Uri
import net.jami.smartlist.ConversationItemViewModel

data class ConversationPath(
    val accountId: String,
    val conversationId: String
) {
    val conversationUri: Uri by lazy { Uri.fromString(conversationId) }

    constructor(account: String, conversationUri: Uri) : this(account, conversationUri.uri)

    constructor(conversation: Conversation) : this(conversation.accountId, conversation.uri.uri)

    constructor(conversation: ConversationItemViewModel) : this(conversation.accountId, conversation.uri.uri)

    inline fun toBundle(bundle: Bundle) {
        bundle.putString(KEY_CONVERSATION_URI, conversationId)
        bundle.putString(KEY_ACCOUNT_ID, accountId)
    }
    fun toBundle() = toBundle(accountId, conversationId)

    fun toUri(): android.net.Uri = ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
        .appendEncodedPath(accountId)
        .appendEncodedPath(conversationId)
        .build()

    fun toKey() = toKey(accountId, conversationId)

    companion object {
        const val KEY_CONVERSATION_URI = "${BuildConfig.APPLICATION_ID}.conversationUri"
        const val KEY_ACCOUNT_ID = "${BuildConfig.APPLICATION_ID}.accountId"

        fun toUri(accountId: String, contactId: String): android.net.Uri =
            ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(contactId)
                .build()

        fun toUri(accountId: String, conversationUri: Uri): android.net.Uri =
            ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(conversationUri.uri)
                .build()

        fun toUri(conversation: Conversation) = toUri(conversation.accountId, conversation.uri)

        fun toUri(interaction: Interaction)=  if (interaction.conversation is Conversation)
            toUri(interaction.account!!, (interaction.conversation as Conversation).uri)
        else
            toUri(interaction.account!!, Uri.fromString(interaction.conversation!!.participant!!))

        inline fun toBundle(accountId: String, uri: String) = Bundle().apply {
            putString(KEY_CONVERSATION_URI, uri)
            putString(KEY_ACCOUNT_ID, accountId)
        }

        fun toBundle(accountId: String, uri: Uri) = toBundle(accountId, uri.uri)

        fun toBundle(conversation: Conversation) = toBundle(conversation.accountId, conversation.uri)

        inline fun toKey(accountId: String, uri: String) = "$accountId,$uri"

        fun fromKey(key: String): ConversationPath? {
            val keys = key.split(',')
            if (keys.size > 1) {
                val accountId = keys[0]
                val contactId = keys[1]
                if (accountId.isNotEmpty() && contactId.isNotEmpty()) {
                    return ConversationPath(accountId, contactId)
                }
            }
            return null
        }

        fun fromUri(uri: android.net.Uri?): ConversationPath? {
            if (uri == null) return null
            if (ContentUriHandler.SCHEME_TV == uri.scheme || uri.toString().startsWith(ContentUriHandler.CONVERSATION_CONTENT_URI.toString())) {
                val pathSegments = uri.pathSegments
                if (pathSegments.size > 2) {
                    return ConversationPath(pathSegments[1], pathSegments[2])
                }
            }
            return null
        }

        fun fromBundle(bundle: Bundle?): ConversationPath? {
            if (bundle != null) {
                val accountId = bundle.getString(KEY_ACCOUNT_ID)
                val contactId = bundle.getString(KEY_CONVERSATION_URI)
                return if (accountId != null && contactId != null)
                    ConversationPath(accountId, contactId)
                else
                    bundle.getString(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)?.let { shortcutId ->
                        fromKey(shortcutId)
                    }
            }
            return null
        }

        fun fromIntent(intent: Intent?): ConversationPath? = if (intent != null) {
            fromUri(intent.data) ?: fromBundle(intent.extras)
        } else null
    }
}

data class InteractionPath(val conversation: ConversationPath, val messageId: String) {
    fun toUri(): android.net.Uri =
        toUri(conversation.accountId, conversation.conversationUri, messageId)

    companion object {
        fun fromUri(uri: android.net.Uri?): InteractionPath? {
            if (uri == null) return null
            if (ContentUriHandler.SCHEME_TV == uri.scheme || uri.toString().startsWith(ContentUriHandler.CONVERSATION_CONTENT_URI.toString())) {
                val pathSegments = uri.pathSegments
                if (pathSegments.size > 3) {
                    return InteractionPath(ConversationPath(pathSegments[1], pathSegments[2]), pathSegments[3])
                }
            }
            return null
        }

        fun toUri(accountId: String, conversationUri: Uri, messageId: String): android.net.Uri =
            ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(conversationUri.uri)
                .appendEncodedPath(messageId)
                .build()
    }
}