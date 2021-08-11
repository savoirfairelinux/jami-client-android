/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils

import android.os.Bundle
import net.jami.model.Interaction
import android.text.TextUtils
import androidx.core.content.pm.ShortcutManagerCompat
import android.content.Intent
import cx.ring.BuildConfig
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.utils.StringUtils
import net.jami.utils.Tuple
import java.util.*

class ConversationPath {
    val accountId: String
    val conversationId: String
    val conversationUri: Uri
        get() = Uri.fromString(conversationId)

    constructor(account: String, contact: String) {
        accountId = account
        conversationId = contact
    }

    constructor(account: String, conversationUri: Uri) {
        accountId = account
        conversationId = conversationUri.uri
    }

    constructor(path: Tuple<String, String>) {
        accountId = path.first
        conversationId = path.second
    }

    constructor(conversation: Conversation) {
        accountId = conversation.accountId
        conversationId = conversation.uri.uri
    }

    fun toBundle(bundle: Bundle) {
        bundle.putString(KEY_CONVERSATION_URI, conversationId)
        bundle.putString(KEY_ACCOUNT_ID, accountId)
    }
    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(KEY_CONVERSATION_URI, conversationId)
        bundle.putString(KEY_ACCOUNT_ID, accountId)
        return bundle
    }
    fun toUri(): android.net.Uri {
        return ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
            .appendEncodedPath(accountId)
            .appendEncodedPath(conversationId)
            .build()
    }
    fun toKey(): String {
        return TextUtils.join(",", listOf(accountId, conversationId))
    }

    override fun toString(): String {
        return "ConversationPath{accountId='$accountId' conversationId='$conversationId'}"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ConversationPath) return false
        return (other.accountId == accountId
                && other.conversationId == conversationId)
    }

    override fun hashCode(): Int {
        return Objects.hash(accountId, conversationId)
    }

    companion object {
        const val KEY_CONVERSATION_URI = BuildConfig.APPLICATION_ID + ".conversationUri"
        const val KEY_ACCOUNT_ID = BuildConfig.APPLICATION_ID + ".accountId"

        fun toUri(accountId: String, contactId: String): android.net.Uri {
            return ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(contactId)
                .build()
        }

        fun toUri(accountId: String?, conversationUri: Uri): android.net.Uri {
            return ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(conversationUri.uri)
                .build()
        }

        fun toUri(conversation: Conversation): android.net.Uri {
            return toUri(conversation.accountId, conversation.uri)
        }

        fun toUri(interaction: Interaction): android.net.Uri {
            return if (interaction.conversation is Conversation)
                toUri(interaction.account, (interaction.conversation as Conversation).uri)
            else
                toUri(interaction.account, Uri.fromString(interaction.conversation!!.participant))
        }

        fun toBundle(accountId: String, uri: String): Bundle {
            val bundle = Bundle()
            bundle.putString(KEY_CONVERSATION_URI, uri)
            bundle.putString(KEY_ACCOUNT_ID, accountId)
            return bundle
        }

        fun toBundle(accountId: String, uri: Uri): Bundle {
            return toBundle(accountId, uri.uri)
        }

        fun toBundle(conversation: Conversation): Bundle {
            return toBundle(conversation.accountId, conversation.uri)
        }

        fun toKey(accountId: String, uri: String): String {
            return TextUtils.join(",", listOf(accountId, uri))
        }

        fun fromKey(key: String?): ConversationPath? {
            if (key != null) {
                val keys = TextUtils.split(key, ",")
                if (keys.size > 1) {
                    val accountId = keys[0]
                    val contactId = keys[1]
                    if (!StringUtils.isEmpty(accountId) && !StringUtils.isEmpty(contactId)) {
                        return ConversationPath(accountId, contactId)
                    }
                }
            }
            return null
        }

        fun fromUri(uri: android.net.Uri?): ConversationPath? {
            if (uri == null) return null
            if (ContentUriHandler.SCHEME_TV == uri.scheme || uri.toString().startsWith(ContentUriHandler.CONVERSATION_CONTENT_URI.toString())) {
                val pathSegments = uri.pathSegments
                if (pathSegments.size > 2) {
                    return ConversationPath(pathSegments[pathSegments.size - 2], pathSegments[pathSegments.size - 1])
                }
            }
            return null
        }

        fun fromBundle(bundle: Bundle?): ConversationPath? {
            if (bundle != null) {
                val accountId = bundle.getString(KEY_ACCOUNT_ID)
                val contactId = bundle.getString(KEY_CONVERSATION_URI)
                if (accountId != null && contactId != null) {
                    return ConversationPath(accountId, contactId)
                } else {
                    val shortcutId = bundle.getString(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
                    if (shortcutId != null) return fromKey(shortcutId)
                }
            }
            return null
        }

        fun fromIntent(intent: Intent?): ConversationPath? {
            if (intent != null) {
                val uri = intent.data
                val conversationPath = fromUri(uri)
                return conversationPath ?: fromBundle(intent.extras)
            }
            return null
        }
    }
}