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
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class LocationSharersTest {
    private val peer = Uri.fromId("9".repeat(40))

    @Before
    fun silenceLogs() {
        // The account logs when a contact stops sending its position
        Log.injectLogService(Proxy.newProxyInstance(LogService::class.java.classLoader,
            arrayOf(LogService::class.java)) { _, _, _ -> null } as LogService)
    }

    @Test
    fun theContactsSendingTheirPositionAreKnown() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val sharers = account.locationSharers.test()

        account.onLocationUpdate(Location("account", null, peer, 1000L).apply {
            type = Location.Type.Position
            latitude = 45.5
            longitude = -73.6
        })
        account.onLocationUpdate(Location("account", null, peer, 2000L).apply { type = Location.Type.Stop })

        sharers.assertValues(emptySet(), setOf(peer), emptySet())
    }
}
