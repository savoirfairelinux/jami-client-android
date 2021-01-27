/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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

package cx.ring.model;

import net.jami.model.Uri;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UriTest {

    @Test
    public void testGoodRawString() {
        String uri = "ring:1234567890123456789012345678901234567890";
        net.jami.model.Uri test = new net.jami.model.Uri(uri);
        assertTrue(test.getRawUriString().contentEquals(uri));
    }

    @Test
    public void testBadIPAddress() {
        assertFalse(net.jami.model.Uri.isIpAddress("not an ip"));
    }

    @Test
    public void testGoodIPAddress() {
        assertTrue(net.jami.model.Uri.isIpAddress("127.0.0.1"));
        assertTrue(net.jami.model.Uri.isIpAddress("2001:db8:0:85a3:0:0:ac1f:8001"));
    }

    @Test
    public void testRingModel() {
        String uri = "ring:1234567890123456789012345678901234567890";
        net.jami.model.Uri test = new net.jami.model.Uri(uri);

        assertTrue(test.getDisplayName() == null);
        assertTrue(test.getScheme().contentEquals("ring:"));
        assertTrue(test.getUriString().contentEquals("ring:1234567890123456789012345678901234567890"));
    }

    @Test
    public void testSIPModel() {
        String uri = "100@sipuri";
        net.jami.model.Uri test = new Uri(uri);

        assertTrue(test.getUsername().contentEquals("100"));
        assertTrue(test.getHost().contentEquals("sipuri"));
    }
}
