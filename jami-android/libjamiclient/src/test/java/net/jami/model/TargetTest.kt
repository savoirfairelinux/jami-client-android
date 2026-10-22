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
package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetTest {
    private val contact = Target.ContactTarget(Uri.fromString("jami:4d1a5d5b2d0d6e1c3f1b8a8d5d6f8e2a1b3c4d5e"), "Alice")
    private val group = Target.GroupTarget(Uri.fromString("swarm:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b"), "Team")
    private val circle = Target.CircleTarget("circle", "Family", memberCount = 3)
    private val board = Target.BoardTarget("swarm:2c26b46b68ffc68ff99b453c1d30413413422d70", "News")

    /** A document is written in one conversation, so it can only start with a contact or in a group. */
    @Test
    fun documentsStartWithContactsAndGroupsOnly() {
        assertTrue(contact.hasCapability(TargetCapability.CAN_SHARE_DOC))
        assertTrue(group.hasCapability(TargetCapability.CAN_SHARE_DOC))
        assertFalse(circle.hasCapability(TargetCapability.CAN_SHARE_DOC))
        assertFalse(board.hasCapability(TargetCapability.CAN_SHARE_DOC))
    }

    @Test
    fun circlesAndBoardsKeepTheirOtherCapabilities() {
        assertEquals(setOf(TargetCapability.CAN_MESSAGE, TargetCapability.CAN_POST), circle.capabilities)
        assertEquals(setOf(TargetCapability.CAN_MESSAGE, TargetCapability.CAN_POST), board.capabilities)
    }
}
