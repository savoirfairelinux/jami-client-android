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
package net.jami.model

import net.jami.model.interaction.Interaction
import net.jami.model.interaction.Interaction.InteractionType
import org.junit.Assert.*
import org.junit.Test

class ConversationThreadsTest {
    private fun message(
        id: String,
        replyTo: String? = null,
        type: InteractionType = InteractionType.TEXT
    ) = Interaction("account").apply {
        setSwarmInfo("conversation", id, null)
        this.type = type
        replyToId = replyTo
        body = id
    }

    @Test
    fun groupsDirectAndNestedRepliesIntoOneThread() {
        val root = message("root").apply { timestamp = 300 }
        val first = message("first", "root").apply { timestamp = 200 }
        val other = message("other")
        val nested = message("nested", "first").apply { timestamp = 150 }
        val second = message("second", "root").apply { timestamp = 100 }
        val threads = ConversationThreads(listOf(root, first, other, nested, second))

        assertEquals(listOf(root, other), threads.timeline)
        assertEquals(listOf(root, first, nested, second), threads.messages("root"))
        assertEquals("root", threads.rootId("nested"))
        assertEquals(3, threads.replyCount("root"))
    }

    @Test
    fun missingAncestorsStayVisibleUntilTheirRootLoads() {
        val first = message("first", "missing")
        val nested = message("nested", "first")
        val threads = ConversationThreads(listOf(first, nested))

        assertEquals(listOf(first, nested), threads.timeline)
        assertNull(threads.rootId("first"))
        assertNull(threads.rootId("nested"))
        assertTrue(threads.messages("missing").isEmpty())
        val root = message("missing")
        val loaded = ConversationThreads(listOf(root, first, nested))
        assertEquals(listOf(root), loaded.timeline)
        assertEquals(listOf(root, first, nested), loaded.messages("missing"))
    }

    @Test
    fun cyclesAndInvalidAncestorsNeverHideMessages() {
        val first = message("first", "second")
        val second = message("second", "first")
        val self = message("self", "self")
        val invalid = message("invalid", type = InteractionType.INVALID)
        val reply = message("reply", "invalid")
        val history = listOf(first, second, self, invalid, reply)
        val threads = ConversationThreads(history)

        assertEquals(history, threads.timeline)
        history.forEach { assertNull(threads.rootId(it.messageId!!)) }
    }

    @Test
    fun attachmentsAndCallsCanBeThreadRoots() {
        val file = message("file", type = InteractionType.DATA_TRANSFER)
        val fileReply = message("file-reply", "file")
        val call = message("call", type = InteractionType.CALL)
        val callReply = message("call-reply", "call")
        val threads = ConversationThreads(listOf(file, fileReply, call, callReply))

        assertEquals(listOf(file, call), threads.timeline)
        assertEquals(listOf(file, fileReply), threads.messages("file"))
        assertEquals(listOf(call, callReply), threads.messages("call"))
    }

    @Test
    fun preservesDeletedRootsAndOriginalObjectsReactionsAndEdits() {
        val root = message("root").apply { body = "" }
        val reply = message("reply", "root")
        val reaction = message("reaction").apply { reactToId = "reply" }
        val edit = message("edit").apply { this.edit = "reply" }
        reply.addReaction(reaction)
        reply.addEdit(edit, true)
        val history = listOf(root, reply)
        val threads = ConversationThreads(history)

        assertSame(reply, threads.messages("root")[1])
        assertSame(root, threads.messages("root").first())
        assertSame(reaction, threads.messages("root")[1].reactions.single())
        assertSame(edit, threads.messages("root")[1].history.last())
        assertEquals(listOf(root, reply), history)
    }

    @Test
    fun duplicateIdsAreCountedOnceAndUseLatestObject() {
        val root = message("root")
        val oldReply = message("reply", "root")
        val updatedReply = message("reply", "root").apply { body = "edited" }
        val threads = ConversationThreads(listOf(root, oldReply, updatedReply))

        assertEquals(1, threads.replyCount("root"))
        assertSame(updatedReply, threads.messages("root")[1])
        assertEquals(0, ConversationThreads(listOf(root)).replyCount("root"))
    }

    @Test
    fun aLongReplyChainDoesNotUseTheCallStack() {
        val history = listOf(message("0")) + (1..10_000).map {
            message(it.toString(), (it - 1).toString())
        }
        val threads = ConversationThreads(history)

        assertEquals("0", threads.rootId("10000"))
        assertEquals(10_000, threads.replyCount("0"))
        assertEquals(1, threads.timeline.size)
        val backwards = ConversationThreads(history.reversed())
        assertEquals("0", backwards.rootId("10000"))
        assertEquals(10_000, backwards.replyCount("0"))
    }
}
