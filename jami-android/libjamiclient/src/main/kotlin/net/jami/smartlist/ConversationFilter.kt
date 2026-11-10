/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.smartlist

import net.jami.model.Conversation

enum class ConversationFilter {
    All, Contacts, Groups, Feeds;

    fun matches(conversation: Conversation): Boolean {
        val mode = conversation.effectiveMode
        return when (this) {
            All -> true
            Contacts -> mode == Conversation.Mode.Legacy || mode == Conversation.Mode.OneToOne
            Groups -> mode.isGroup && mode != Conversation.Mode.Feed
            Feeds -> mode == Conversation.Mode.Feed
        }
    }
}
