/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatusTest {

    @Test
    public void fromString_test() throws Exception {
        TextMessage.Status[] values = TextMessage.Status.values();
        for (TextMessage.Status s : values) {

            assertEquals(TextMessage.Status.fromString(s.name()), s);
        }
    }

    @Test
    public void fromString_invalid_test() throws Exception {
        TextMessage.Status status = TextMessage.Status.fromString("abc");

        assertEquals(TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void fromString_null_test() throws Exception {
        TextMessage.Status status = TextMessage.Status.fromString(null);

        assertEquals(TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void fromInt_test() throws Exception {
        for (int i = 0; i < 5; i++) {
            TextMessage.Status status = TextMessage.Status.fromInt(i);
            TextMessage.Status[] values = TextMessage.Status.values();

            assertEquals(status, values[i]);
        }
    }

    @Test
    public void fromInt_invalid_test() throws Exception {
        TextMessage.Status status = TextMessage.Status.fromInt(-1);

        assertEquals(TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void toString_test() throws Exception {
        TextMessage.Status[] values = TextMessage.Status.values();
        for (TextMessage.Status s : values) {
            assertEquals(s.toString(), s.name());
        }
    }
}