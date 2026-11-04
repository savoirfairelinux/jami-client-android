/*
 * Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.model

import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.smartlist.ConversationItemViewModel

data class ContactPickerItem(
    val conversation: ConversationItemViewModel,
    val members: List<ContactViewModel>
) {
    val memberUris: Set<String> = members.map { it.contact.uri.rawUriString }.toSet()

    fun isSelected(selection: Set<String>): Boolean =
        memberUris.isNotEmpty() && selection.containsAll(memberUris)

    fun toggleSelection(selection: MutableSet<String>) {
        if (isSelected(selection)) selection.removeAll(memberUris)
        else selection.addAll(memberUris)
    }
}

internal fun createContactPickerItem(
    conversation: ConversationItemViewModel,
    excluded: Set<String>,
    roles: Map<String, MemberRole>
): ContactPickerItem? {
    val members = when {
        conversation.mode == Conversation.Mode.Legacy || conversation.mode == Conversation.Mode.OneToOne ->
            listOfNotNull(conversation.getContact())
        conversation.mode.isGroup && !conversation.isEnded ->
            conversation.contacts.filter {
                when (roles[it.contact.uri.uri]) {
                    MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.INVITED -> true
                    else -> false
                }
            }
        else -> return null
    }.filter { !it.contact.isUser && it.contact.uri.rawUriString !in excluded }
        .distinctBy { it.contact.uri }

    return members.takeIf { it.isNotEmpty() }?.let { ContactPickerItem(conversation, it) }
}
