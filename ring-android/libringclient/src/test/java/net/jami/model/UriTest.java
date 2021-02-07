/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.jami.model;

import net.jami.utils.Tuple;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UriTest {

    @Test
    public void testGoodRawString() {
        String uri = "ring:1234567890123456789012345678901234567890";
        Uri test = Uri.fromString(uri);
        assertTrue(test.getRawUriString().contentEquals(uri));
    }

    @Test
    public void testBadIPAddress() {
        assertFalse(Uri.isIpAddress("not an ip"));
    }

    @Test
    public void testGoodIPAddress() {
        assertTrue(Uri.isIpAddress("127.0.0.1"));
        assertTrue(Uri.isIpAddress("2001:db8:0:85a3:0:0:ac1f:8001"));
    }

    @Test
    public void testRingModel() {
        String uri = "ring:1234567890123456789012345678901234567890";
        Tuple<Uri, String> test = Uri.fromStringWithName(uri);

        assertNull(test.second);
        assertTrue(test.first.getScheme().contentEquals("ring:"));
        assertTrue(test.first.getUri().contentEquals("ring:1234567890123456789012345678901234567890"));
    }

    @Test
    public void testSIPModel() {
        String uri = "100@sipuri";
        Uri test = Uri.fromString(uri);

        assertTrue(test.getUsername().contentEquals("100"));
        assertTrue(test.getHost().contentEquals("sipuri"));
    }
}
