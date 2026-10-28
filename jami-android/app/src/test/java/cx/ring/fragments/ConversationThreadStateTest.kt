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
    fun defaultsToTheExistingConversationDraftAndNormalSends() {
        val state = ConversationThreadState()
        assertFalse(state.enabled)
        assertEquals("pendingMessage", state.draftKey)
        assertNull(state.replyTarget(null))
        assertEquals("quote", state.replyTarget("quote"))
    }

    @Test
    fun disablingThreadsRestoresTheMainComposerContext() {
        val state = ConversationThreadState(true, "root")
        val threadDraftKey = state.draftKey
        state.enable(false)
        assertNull(state.rootId)
        assertNull(state.replyTarget(null))
        assertEquals("pendingMessage", state.draftKey)
        state.enable(true)
        state.openThread("root")
        assertEquals(threadDraftKey, state.draftKey)
    }

    @Test
    fun rootsHaveIndependentTextAndQuoteDraftKeys() {
        val state = ConversationThreadState(true)
        val main = state.draftKey
        state.openThread("one")
        val first = state.draftKey
        val firstQuote = state.replyDraftKey
        state.openThread("two")
        assertNotEquals(first, state.draftKey)
        assertNotEquals(firstQuote, state.replyDraftKey)
        state.closeThread()
        assertEquals(main, state.draftKey)
    }

    @Test
    fun explicitQuotesStayWithinTheirExistingReplyChain() {
        val state = ConversationThreadState(true, "root")
        assertEquals("root", state.replyTarget(null))
        assertEquals("child", state.replyTarget("child"))
        assertEquals("root", state.replyTarget(null))
    }

    @Test
    fun capturedAttachmentTargetsSurviveNavigationAndToggleChanges() {
        val state = ConversationThreadState(true, "root")
        val attachmentTarget = state.replyTarget(null)
        state.openThread("other")
        state.enable(false)
        assertEquals("root", attachmentTarget)
        assertNull(state.replyTarget(null))
    }

    @Test
    fun restoresTheOpenThreadOnlyWhenThreadingIsEnabled() {
        assertEquals("root", ConversationThreadState(true, "root").rootId)
        assertNull(ConversationThreadState(false, "root").rootId)
    }

    @Test
    fun openingAThreadShowsLatestMessagesOnlyOnce() {
        val state = ConversationThreadState(true)
        state.openThread("root")
        assertTrue(state.consumeScrollToLatest(null))
        assertFalse(state.consumeScrollToLatest(null))
    }

    @Test
    fun searchAndQuoteJumpsOverrideTheDefaultLatestPosition() {
        val state = ConversationThreadState(true)
        state.openThread("root")
        assertFalse(state.consumeScrollToLatest("older-reply"))
        assertFalse(state.consumeScrollToLatest(null))
    }

    @Test
    fun restoredThreadsKeepTheirSavedScrollPosition() {
        assertFalse(ConversationThreadState(true, "root").consumeScrollToLatest(null))
    }

    @Test
    fun closingOrDisablingThreadingClearsTheLatestPositionRequest() {
        val state = ConversationThreadState(true)
        state.openThread("root")
        state.closeThread()
        assertFalse(state.consumeScrollToLatest(null))
        state.openThread("root")
        state.enable(false)
        assertFalse(state.consumeScrollToLatest(null))
    }

    @Test
    fun switchingThreadsShowsTheLatestMessageInTheNewThread() {
        val state = ConversationThreadState(true)
        state.openThread("first")
        assertTrue(state.consumeScrollToLatest(null))
        state.openThread("second")
        assertTrue(state.consumeScrollToLatest(null))
    }
}
