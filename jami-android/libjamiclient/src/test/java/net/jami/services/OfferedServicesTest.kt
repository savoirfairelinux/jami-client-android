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
package net.jami.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferedServicesTest {
    private val alice = "a".repeat(40)
    private val bob = "b".repeat(40)
    private val carol = "c".repeat(40)

    private val files = service("files")
    private val music = service("music")
    private val wiki = service("wiki")

    @Test
    fun nothingIsOfferedBeforeAnyContactAnswers() {
        val offered = OfferedServices.asking(listOf(alice, bob))

        assertTrue(offered.isLoading)
        assertEquals(2, offered.pending)
        assertTrue(offered.services.isEmpty())
    }

    @Test
    fun withoutContactsThereIsNothingToWaitFor() {
        val offered = OfferedServices.asking(emptyList())

        assertFalse(offered.isLoading)
        assertEquals(0, offered.pending)
        assertTrue(offered.services.isEmpty())
    }

    @Test
    fun eachAnswerAddsTheServicesOfItsContact() {
        val bobAnswered = OfferedServices.asking(listOf(alice, bob))
            .with(answer(bob, wiki))

        assertEquals(listOf(OfferedService(bob, wiki)), bobAnswered.services)
        assertEquals(1, bobAnswered.pending)
        assertTrue(bobAnswered.isLoading)

        val allAnswered = bobAnswered.with(answer(alice, files, music))

        assertEquals(
            listOf(OfferedService(alice, files), OfferedService(alice, music), OfferedService(bob, wiki)),
            allAnswered.services
        )
        assertEquals(0, allAnswered.pending)
        assertFalse(allAnswered.isLoading)
    }

    /** Contacts keep the order they were asked in, each one its services in the order it gave them. */
    @Test
    fun theOrderDoesNotDependOnWhoAnswersFirst() {
        val contacts = listOf(alice, bob, carol)
        val answers = listOf(answer(alice, files, music), answer(bob, wiki), answer(carol, service("chat")))

        val inOrder = answers.fold(OfferedServices.asking(contacts)) { offered, answer -> offered.with(answer) }
        val reversed = answers.reversed().fold(OfferedServices.asking(contacts)) { offered, answer -> offered.with(answer) }

        assertEquals(listOf("files", "music", "wiki", "chat"), inOrder.services.map { it.service.id })
        assertEquals(inOrder.services, reversed.services)
    }

    @Test
    fun aContactThatCouldNotAnswerOffersNothingButIsNoLongerAwaited() {
        for (status in PeerServicesStatus.entries - PeerServicesStatus.OK) {
            val offered = OfferedServices.asking(listOf(alice, bob))
                .with(answer(alice, files, status = status))

            assertTrue("$status", offered.services.isEmpty())
            assertEquals("$status", 1, offered.pending)
        }
    }

    @Test
    fun anAnswerFromAContactNotAskedIsIgnored() {
        val offered = OfferedServices.asking(listOf(alice))
            .with(answer(bob, wiki))

        assertTrue(offered.services.isEmpty())
        assertEquals(1, offered.pending)
    }

    @Test
    fun aContactListedTwiceIsAwaitedOnce() {
        val offered = OfferedServices.asking(listOf(alice, alice))

        assertEquals(1, offered.pending)
        assertFalse(offered.with(answer(alice, files)).isLoading)
        assertEquals(1, offered.with(answer(alice, files)).services.size)
    }

    @Test
    fun aContactAnsweringAgainCountsOnceWithItsLatestServices() {
        val offered = OfferedServices.asking(listOf(alice, bob))
            .with(answer(alice, files))
            .with(answer(alice, music))

        assertEquals(listOf(OfferedService(alice, music)), offered.services)
        assertEquals(1, offered.pending)
    }

    /** A tunnel is known by its contact and the id of its service: each pair is listed once. */
    @Test
    fun aServiceIsListedOncePerContact() {
        val offered = OfferedServices.asking(listOf(alice, bob))
            .with(answer(alice, files, files.copy(device = "other device")))
            .with(answer(bob, files))

        assertEquals(listOf(OfferedService(alice, files), OfferedService(bob, files)), offered.services)
    }

    private fun service(id: String) = PeerServiceInfo(id, id.replaceFirstChar { it.uppercase() }, "", "http", "device")

    private fun answer(peer: String, vararg services: PeerServiceInfo, status: PeerServicesStatus = PeerServicesStatus.OK) =
        PeerServicesResult(requestId = 1, accountId = "account", peerId = peer, status = status, services = services.toList())
}
