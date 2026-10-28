/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import org.junit.Assert.*
import org.junit.Test

class ConversationThreadNavigationTest {
    @Test
    fun allowsOnlyOneHistoryRequestAtATime() {
        val navigation = ConversationThreadNavigation()
        navigation.start("target")
        val request = navigation.begin(setOf("new"))!!
        assertNull(navigation.begin(setOf("new")))
        assertTrue(navigation.complete(request, setOf("new", "older"), false))
        assertNotNull(navigation.begin(setOf("new", "older")))
    }

    @Test
    fun stopsWhenHistoryMakesNoProgress() {
        val navigation = ConversationThreadNavigation()
        navigation.start("missing")
        val request = navigation.begin(setOf("new"))!!
        assertFalse(navigation.complete(request, setOf("new"), false))
        assertTrue(navigation.failed)
        assertNull(navigation.target)
        assertNull(navigation.begin(setOf("new")))
    }

    @Test
    fun cancellationDoesNotContinueAfterThePendingRequestFinishes() {
        val navigation = ConversationThreadNavigation()
        navigation.start("target")
        val request = navigation.begin(emptySet())!!
        navigation.cancel()
        assertFalse(navigation.complete(request, setOf("target"), true))
        assertFalse(navigation.failed)
        assertNull(navigation.target)
    }

    @Test
    fun aNewTargetCanReuseThePendingBatch() {
        val navigation = ConversationThreadNavigation()
        navigation.start("first")
        val request = navigation.begin(emptySet())!!
        navigation.start("second")
        assertNull(navigation.begin(emptySet()))
        assertTrue(navigation.complete(request, setOf("second"), true))
        assertEquals("second", navigation.target)
        navigation.cancel()
        assertNull(navigation.target)
    }

    @Test
    fun aResolvedAncestorDoesNotRequireAnotherNewMessage() {
        val navigation = ConversationThreadNavigation()
        navigation.start("reply")
        val request = navigation.begin(setOf("reply", "root"))!!
        assertTrue(navigation.complete(request, setOf("reply", "root"), true))
        assertFalse(navigation.failed)
    }

    @Test
    fun callbacksFromADestroyedViewCannotCompleteANewRequest() {
        val navigation = ConversationThreadNavigation()
        navigation.start("old")
        val stale = navigation.begin(emptySet())!!
        navigation.reset()
        navigation.start("new")
        val current = navigation.begin(emptySet())!!
        assertFalse(navigation.complete(stale, emptySet(), false))
        assertEquals("new", navigation.target)
        assertNull(navigation.begin(emptySet()))
        assertTrue(navigation.complete(current, setOf("new"), true))
    }

    @Test
    fun aNewNavigationClearsTheUnavailableState() {
        val navigation = ConversationThreadNavigation()
        navigation.start("missing")
        val request = navigation.begin(emptySet())!!
        navigation.complete(request, emptySet(), false)
        navigation.start("other")
        assertFalse(navigation.failed)
        assertEquals("other", navigation.target)
    }
}
