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

import net.jami.smartlist.ConversationItemViewModel

enum class TargetCapability {
    CAN_CALL,
    CAN_MESSAGE,
    CAN_SHARE_DOC,
    CAN_POST,
    CAN_SHARE_LOCATION
}

sealed interface Target {
    val id: String
    val displayName: String
    val secondaryText: String?
    val avatarUri: Uri?
    val viewModel: ConversationItemViewModel?
    val capabilities: Set<TargetCapability>

    fun hasCapability(capability: TargetCapability): Boolean = capability in capabilities

    data class ContactTarget(
        val uri: Uri,
        val name: String,
        val username: String? = null,
        val photoUri: Uri? = null,
        val isOnline: Boolean = false,
        override val viewModel: ConversationItemViewModel? = null
    ) : Target {
        override val id: String get() = uri.rawUriString
        override val displayName: String get() = name
        override val secondaryText: String? get() = username ?: uri.rawUriString
        override val avatarUri: Uri? get() = photoUri
        override val capabilities: Set<TargetCapability> get() = setOf(
            TargetCapability.CAN_CALL,
            TargetCapability.CAN_MESSAGE,
            TargetCapability.CAN_SHARE_DOC,
            TargetCapability.CAN_SHARE_LOCATION
        )
    }

    data class GroupTarget(
        val uri: Uri,
        val title: String,
        val memberCount: Int = 0,
        val avatar: Uri? = null,
        override val viewModel: ConversationItemViewModel? = null
    ) : Target {
        override val id: String get() = uri.rawUriString
        override val displayName: String get() = title
        override val secondaryText: String? get() = null
        override val avatarUri: Uri? get() = avatar
        override val capabilities: Set<TargetCapability> get() = setOf(
            TargetCapability.CAN_CALL,
            TargetCapability.CAN_MESSAGE,
            TargetCapability.CAN_SHARE_DOC,
            TargetCapability.CAN_SHARE_LOCATION
        )
    }

    data class CircleTarget(
        val circleId: String,
        val name: String,
        val memberCount: Int = 0,
        val uri: Uri? = null,
        override val viewModel: ConversationItemViewModel? = null
    ) : Target {
        override val id: String get() = circleId
        override val displayName: String get() = name
        override val secondaryText: String? get() = null
        override val avatarUri: Uri? get() = uri
        // A document lives in a single conversation: a Circle has none of its own
        override val capabilities: Set<TargetCapability> get() = setOf(
            TargetCapability.CAN_MESSAGE,
            TargetCapability.CAN_POST
        )
    }

    data class BoardTarget(
        val boardId: String,
        val title: String,
        val description: String? = null,
        val uri: Uri? = null,
        override val viewModel: ConversationItemViewModel? = null
    ) : Target {
        override val id: String get() = boardId
        override val displayName: String get() = title
        override val secondaryText: String? get() = description
        override val avatarUri: Uri? get() = uri
        // Only its owner publishes on a Board: no document is written together there
        override val capabilities: Set<TargetCapability> get() = setOf(
            TargetCapability.CAN_MESSAGE,
            TargetCapability.CAN_POST
        )
    }
}
