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
package net.jami.utils

import net.jami.utils.VCardUtils.vcardToString
import ezvcard.VCard
import ezvcard.property.FormattedName
import net.jami.utils.VCardUtils
import org.junit.Assert
import org.junit.Test

class VCardUtilsTest {
    @Test
    fun testVCardToString() {
        val result = vcardToString(VCard().apply {
            addFormattedName(FormattedName("SFL Test"))
        })
        Assert.assertTrue(result.contentEquals(
                """
    BEGIN:VCARD
    VERSION:2.1
    X-PRODID:ez-vcard 0.10.2
    FN:SFL Test
    END:VCARD
    
    """.trimIndent()))
    }
}