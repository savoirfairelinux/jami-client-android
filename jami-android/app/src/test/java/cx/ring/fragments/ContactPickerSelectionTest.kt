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
package cx.ring.fragments

import cx.ring.model.ContactPickerItem
import cx.ring.model.createContactPickerItem
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.smartlist.ConversationItemViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactPickerSelectionTest {
    @Test
    fun rejectsMissingAccountOrStaleSelection() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        for (account in listOf(null, "", " ")) {
            assertNull(resolveContactPickerSelection(account, uris(alice), listOf(alice)))
        }
        assertNull(resolveContactPickerSelection("account-a", emptySet(), listOf(alice)))
        assertNull(resolveContactPickerSelection("account-a", uris(alice), emptyList()))
        assertNull(resolveContactPickerSelection("account-a", uris(alice, bob), listOf(alice)))
    }

    @Test
    fun selectionIsRestrictedToThePickerAccount() {
        val aliceA = contact("account-a", "alice")
        val aliceB = contact("account-b", "alice")
        val bob = contact("account-b", "bob")
        assertNull(resolveContactPickerSelection("account-a", uris(aliceB), listOf(aliceB)))
        assertNull(resolveContactPickerSelection("account-a", uris(aliceA, bob), listOf(aliceA, bob)))
        assertEquals(
            setOf(aliceA.members.single().contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(aliceA, aliceB))
        )
        assertEquals(
            setOf(aliceA.members.single().contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(aliceB, aliceA))
        )
        assertEquals(
            setOf(aliceB.members.single().contact),
            resolveContactPickerSelection("account-b", uris(aliceB), listOf(aliceA, aliceB))
        )
        assertEquals(
            setOf(aliceA.members.single().contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(bob, aliceA))
        )
    }

    @Test
    fun returnsCompleteSelectionForPickerAccount() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        val selection = resolveContactPickerSelection("account-a", uris(alice, bob), listOf(bob, alice))

        assertEquals(setOf(alice.members.single().contact, bob.members.single().contact), selection)
    }

    @Test
    fun groupSelectionResolvesEveryMemberIncludingContactsWithoutOneToOneChats() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        val group = group("group", alice.members + bob.members)

        assertEquals(
            setOf(alice.members.single().contact, bob.members.single().contact),
            resolveContactPickerSelection("account-a", uris(alice, bob), listOf(group))
        )
    }

    @Test
    fun groupAndIndividualCheckStatesShareTheSameSelection() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        val group = group("group", alice.members + bob.members)
        val selection = linkedSetOf<String>()
        alice.toggleSelection(selection)
        assertFalse(group.isSelected(selection))

        group.toggleSelection(selection)
        assertTrue(group.isSelected(selection))
        assertTrue(alice.isSelected(selection))
        assertTrue(bob.isSelected(selection))
        assertEquals(uris(alice, bob), selection)

        alice.toggleSelection(selection)
        assertFalse(group.isSelected(selection))
        assertTrue(bob.isSelected(selection))
        group.toggleSelection(selection)
        group.toggleSelection(selection)
        assertTrue(selection.isEmpty())
    }

    @Test
    fun overlappingGroupsProduceDeduplicatedMembers() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        val charlie = contact("account-a", "charlie")
        val first = group("first", alice.members + bob.members)
        val second = group("second", bob.members + charlie.members)
        val selection = linkedSetOf<String>()
        first.toggleSelection(selection)
        second.toggleSelection(selection)

        assertEquals(uris(alice, bob, charlie), selection)
        assertEquals(
            setOf(alice.members.single().contact, bob.members.single().contact, charlie.members.single().contact),
            resolveContactPickerSelection("account-a", selection, listOf(alice, first, second))
        )
    }

    @Test
    fun groupSourcesExcludeSelfRemovedBlockedAndAlreadyPresentContacts() {
        val names = listOf("self", "admin", "member", "invited", "blocked", "left", "unknown", "existing")
        val members = names.map { name ->
            val contact = Contact(Uri.fromString("sip:$name@example.test"), name == "self")
            ContactViewModel(contact, Profile(name, null))
        }
        val roles = listOf(
            MemberRole.ADMIN, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.INVITED,
            MemberRole.BLOCKED, MemberRole.LEFT, MemberRole.UNKNOWN, MemberRole.MEMBER
        )
        val source = group("source", members).conversation
        val item = createContactPickerItem(
            source, setOf(members.last().contact.uri.rawUriString),
            members.zip(roles).associate { (member, role) -> member.contact.uri.uri to role }
        )!!
        assertEquals(members.slice(1..3), item.members)
        assertNull(createContactPickerItem(source, emptySet(), emptyMap()))
    }

    @Test
    fun pendingAndEmptyGroupsAreNotSelectionSources() {
        val alice = contact("account-a", "alice")
        val pending = ConversationItemViewModel(
            Conversation("account-a", Uri(Uri.SWARM_SCHEME, "pending"), Conversation.Mode.Request),
            Profile("Pending group", null), alice.members, false
        )
        assertNull(createContactPickerItem(pending, emptySet(), emptyMap()))
        assertNull(createContactPickerItem(group("empty", emptyList()).conversation, emptySet(), emptyMap()))
    }

    private fun group(name: String, members: List<ContactViewModel>): ContactPickerItem {
        val conversation = Conversation("account-a", Uri(Uri.SWARM_SCHEME, name), Conversation.Mode.InvitesOnly)
        members.forEach {
            conversation.addContact(it.contact, if (it.contact.isUser) MemberRole.ADMIN else MemberRole.MEMBER)
        }
        return ContactPickerItem(
            ConversationItemViewModel(
                conversation,
                Profile(name, null), members, false
            ), members
        )
    }

    private fun contact(accountId: String, name: String): ContactPickerItem {
        val contact = Contact(Uri.fromString("sip:$name@example.test"))
        val profile = Profile(name, null)
        val members = listOf(ContactViewModel(contact, profile))
        return ContactPickerItem(
            ConversationItemViewModel(Conversation(accountId, contact), profile, members, false),
            members
        )
    }

    private fun uris(vararg contacts: ContactPickerItem): Set<String> =
        contacts.flatMap { it.memberUris }.toSet()
}
