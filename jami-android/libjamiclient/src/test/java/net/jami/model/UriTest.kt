/*
 * Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package net.jami.model

import net.jami.model.Uri.Companion.fromString
import net.jami.model.Uri.Companion.fromStringWithName
import net.jami.model.Uri.Companion.isIpAddress
import org.junit.Assert
import org.junit.Test

class UriTest {
    @Test
    fun testGoodRawString() {
        val uri = "ring:1234567890123456789012345678901234567890"
        val test = fromString(uri)
        Assert.assertTrue(test.rawUriString.contentEquals(uri))
    }

    @Test
    fun testBadIPAddress() {
        Assert.assertFalse(isIpAddress("not an ip"))
    }

    @Test
    fun testGoodIPAddress() {
        Assert.assertTrue(isIpAddress("127.0.0.1"))
        Assert.assertTrue(isIpAddress("2001:db8:0:85a3:0:0:ac1f:8001"))
    }

    @Test
    fun testRingModel() {
        val uri = "ring:1234567890123456789012345678901234567890"
        val test = fromStringWithName(uri)
        Assert.assertNull(test.second)
        Assert.assertTrue(test.first.scheme.contentEquals("ring:"))
        Assert.assertTrue(test.first.uri.contentEquals("ring:1234567890123456789012345678901234567890"))
    }

    @Test
    fun testSIPModel() {
        val uri = "100@sipuri"
        val test = fromString(uri)
        Assert.assertTrue(test.username.contentEquals("100"))
        Assert.assertTrue(test.host.contentEquals("sipuri"))
    }
}