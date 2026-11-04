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

class ConversationThreadStateTest {
    @Test
    fun feedsAlwaysUseThreadsWhileOtherChatsFollowTheSetting() {
        for (preference in listOf(false, true)) {
            assertTrue(ConversationThreadState.shouldEnable(true, true, preference))
            assertEquals(preference, ConversationThreadState.shouldEnable(true, false, preference))
            assertFalse(ConversationThreadState.shouldEnable(false, false, preference))
        }
    }

    @Test
    fun draftsAndReplyTargetsStayIndependentAcrossThreads() {
        val state = ConversationThreadState()
        assertFalse(state.enabled)
        assertEquals("pendingMessage", state.draftKey)
        assertNull(state.replyTarget(null))
        assertEquals("quote", state.replyTarget("quote"))
        state.enable(true)
        state.openThread("one")
        val first = state.draftKey
        val firstQuote = state.replyDraftKey
        val attachmentTarget = state.replyTarget(null)
        assertEquals("one", attachmentTarget)
        assertEquals("child", state.replyTarget("child"))
        state.openThread("two")
        assertNotEquals(first, state.draftKey)
        assertNotEquals(firstQuote, state.replyDraftKey)
        state.closeThread()
        assertEquals("pendingMessage", state.draftKey)
        state.openThread("one")
        assertEquals(first, state.draftKey)
        state.enable(false)
        assertEquals("one", attachmentTarget)
        assertNull(state.rootId)
        assertNull(state.replyTarget(null))
        assertEquals("pendingMessage", state.draftKey)
    }

    @Test
    fun restoresTheOpenThreadOnlyWhenThreadingIsEnabled() {
        assertEquals("root", ConversationThreadState(true, "root").rootId)
        assertNull(ConversationThreadState(false, "root").rootId)
    }

    @Test
    fun openingAndSwitchingThreadsScrollsToLatestOnlyOnce() {
        val state = ConversationThreadState(true)
        state.openThread("first")
        assertTrue(state.consumeScrollToLatest(null))
        assertFalse(state.consumeScrollToLatest(null))
        state.openThread("second")
        assertTrue(state.consumeScrollToLatest(null))
        state.closeThread()
        assertFalse(state.consumeScrollToLatest(null))
        state.openThread("first")
        state.enable(false)
        assertFalse(state.consumeScrollToLatest(null))
    }

    @Test
    fun restoredThreadsAndHistoryJumpsKeepTheirPosition() {
        assertFalse(ConversationThreadState(true, "root").consumeScrollToLatest(null))
        val state = ConversationThreadState(true)
        state.openThread("root")
        assertFalse(state.consumeScrollToLatest("older-reply"))
        assertFalse(state.consumeScrollToLatest(null))
    }
}
