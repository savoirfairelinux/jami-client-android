/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.model

import net.jami.services.AccountService.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSharingTest {
    private val peer = Uri.fromId("9".repeat(40))

    @Test
    fun aPositionIsRead() {
        val location = Location.parse("account", null, peer,
            """{"type":"Position","lat":45.5,"long":-73.6,"time":1000}""")

        assertNotNull(location)
        assertEquals(Location.Type.Position, location!!.type)
        assertEquals(45.5, location.latitude, 0.0)
        assertEquals(-73.6, location.longitude, 0.0)
        assertEquals(1000L, location.date)
    }

    @Test
    fun aStopWithoutTimeIsReadAndDatedOnReception() {
        // As Jami for desktop sends it
        val location = Location.parse("account", null, peer, """{"type":"Stop"}""", now = 5000L)

        assertNotNull(location)
        assertEquals(Location.Type.Stop, location!!.type)
        assertEquals(5000L, location.date)
    }

    @Test
    fun aMessageThatIsNotALocationIsIgnored() {
        assertNull(Location.parse("account", null, peer, """{"type":"Unknown","time":1000}"""))
        assertNull(Location.parse("account", null, peer, """{"type":"Position","time":1000}"""))
    }

    @Test
    fun anUnchangedPositionKeepsThePeerLocationAlive() {
        // A peer that does not move may keep sending the very same position,
        // dated from when it was found: it must not be forgotten while it does.
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val contact = account.getContactFromCache(peer)
        account.onLocationUpdate(position(time = 1000L))
        val first = receivedDate(account, contact)

        Thread.sleep(5)
        account.onLocationUpdate(position(time = 1000L))

        assertTrue(receivedDate(account, contact).after(first))
    }

    private fun position(time: Long) = Location("account", null, peer, time).apply {
        type = Location.Type.Position
        latitude = 45.5
        longitude = -73.6
    }

    private fun receivedDate(account: Account, contact: Contact) =
        account.locationsUpdates.blockingFirst()[contact]!!.blockingFirst().receivedDate
}
