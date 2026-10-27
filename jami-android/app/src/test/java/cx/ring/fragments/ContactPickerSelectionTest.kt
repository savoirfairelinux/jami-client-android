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

import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.smartlist.ConversationItemViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            setOf(aliceA.getContact()!!.contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(aliceA, aliceB))
        )
        assertEquals(
            setOf(aliceA.getContact()!!.contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(aliceB, aliceA))
        )
        assertEquals(
            setOf(aliceB.getContact()!!.contact),
            resolveContactPickerSelection("account-b", uris(aliceB), listOf(aliceA, aliceB))
        )
        assertEquals(
            setOf(aliceA.getContact()!!.contact),
            resolveContactPickerSelection("account-a", uris(aliceA), listOf(bob, aliceA))
        )
    }

    @Test
    fun returnsCompleteSelectionForPickerAccount() {
        val alice = contact("account-a", "alice")
        val bob = contact("account-a", "bob")
        val selection = resolveContactPickerSelection("account-a", uris(alice, bob), listOf(bob, alice))

        assertEquals(setOf(alice.getContact()!!.contact, bob.getContact()!!.contact), selection)
    }

    private fun contact(accountId: String, name: String): ConversationItemViewModel {
        val contact = Contact(Uri.fromString("sip:$name@example.test"))
        val profile = Profile(name, null)
        return ConversationItemViewModel(
            Conversation(accountId, contact),
            profile,
            listOf(ContactViewModel(contact, profile)),
            false
        )
    }

    private fun uris(vararg contacts: ConversationItemViewModel): Set<String> =
        contacts.map { it.getContact()!!.contact.uri.rawUriString }.toSet()
}
