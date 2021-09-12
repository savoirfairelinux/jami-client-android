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

package net.jami.utils;

import net.jami.utils.VCardUtils;

import org.junit.Test;

import ezvcard.VCard;
import ezvcard.property.FormattedName;

import static org.junit.Assert.assertTrue;

public class VCardUtilsTest {

    @Test
    public void testVCardToString() {
        VCard vcard = new VCard();
        vcard.addFormattedName(new FormattedName("SFL Test"));

        String result = VCardUtils.vcardToString(vcard);
        assertTrue(result.contentEquals("BEGIN:VCARD\r\n" +
                "VERSION:2.1\r\n" +
                "X-PRODID:ez-vcard 0.10.2\r\n" +
                "FN:SFL Test\r\n" +
                "END:VCARD\r\n"));
    }
}
