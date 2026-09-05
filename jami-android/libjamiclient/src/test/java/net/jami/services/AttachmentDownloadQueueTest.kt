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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.services

import net.jami.services.AttachmentDownloadQueue.Attachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Asking a source device for every attachment at once overloads its connection
 * (observed: 17 parallel transfers repeatedly killing the peer TLS session), so
 * the downloads are paced: a few at a time, a slot being released when a file
 * is complete or when it has not progressed for a while. A released download is
 * not lost: the daemon keeps the request and retries it on the next sync.
 */
class AttachmentDownloadQueueTest {
    private val now = 1_000_000_000L
    private val stallTimeout = AttachmentDownloadQueue.STALL_TIMEOUT_NANOS
    private val second = 1_000_000_000L

    private fun attachment(n: Int) =
        Attachment("account", "conv", "msg$n", "msg${n}_tid.jpg", "photo$n.jpg")

    private fun queueOf(count: Int) = AttachmentDownloadQueue().apply {
        for (i in 0 until count) enqueue(attachment(i))
    }

    @Test
    fun startsAtMostMaxConcurrentDownloads() {
        val queue = queueOf(5)

        val started = queue.start(now)

        assertEquals(AttachmentDownloadQueue.MAX_CONCURRENT, started.size)
        assertEquals(AttachmentDownloadQueue.MAX_CONCURRENT, queue.inFlight().size)
        assertTrue(queue.start(now).isEmpty())
        assertFalse(queue.isIdle)
    }

    @Test
    fun completedDownloadReleasesItsSlot() {
        val queue = queueOf(4)
        queue.start(now)

        queue.update(attachment(0).fileId, progress = 100, total = 100, now = now + second)

        val started = queue.start(now + second)
        assertEquals(listOf(attachment(3).fileId), started.map { it.fileId })
    }

    @Test
    fun stalledDownloadReleasesItsSlotAfterTimeout() {
        val queue = queueOf(4)
        queue.start(now)

        val beforeTimeout = now + stallTimeout - second
        queue.update(attachment(1).fileId, progress = 0, total = 100, now = beforeTimeout)
        assertTrue(queue.start(beforeTimeout).isEmpty())

        val afterTimeout = now + stallTimeout
        queue.update(attachment(1).fileId, progress = 0, total = 100, now = afterTimeout)
        assertEquals(listOf(attachment(3).fileId), queue.start(afterTimeout).map { it.fileId })
    }

    @Test
    fun progressingDownloadKeepsItsSlot() {
        val queue = queueOf(4)
        queue.start(now)

        var later = now
        for (step in 1..3) {
            later += stallTimeout - second
            queue.update(attachment(2).fileId, progress = step * 10L, total = 100, now = later)
        }

        assertTrue(queue.start(later).isEmpty())
        assertEquals(AttachmentDownloadQueue.MAX_CONCURRENT, queue.inFlight().size)
    }

    /**
     * A zero-byte attachment reports total == progress == 0 whether it is still
     * waiting or already complete, so its completion comes from the daemon's
     * terminal transfer event rather than from the progress.
     */
    @Test
    fun finishedDownloadReleasesItsSlotWhateverItsSize() {
        val queue = queueOf(4)
        queue.start(now)

        queue.update(attachment(0).fileId, progress = 0, total = 0, now = now + second)
        assertTrue(queue.start(now + second).isEmpty())

        queue.finish(attachment(0).fileId)
        queue.finish("unknown_tid.jpg")
        assertEquals(listOf(attachment(3).fileId), queue.start(now + second).map { it.fileId })
    }

    @Test
    fun ignoresDuplicatesAndUnknownFiles() {
        val queue = AttachmentDownloadQueue()
        queue.enqueue(attachment(0))
        queue.enqueue(attachment(0))

        assertEquals(1, queue.start(now).size)
        queue.enqueue(attachment(0))
        assertTrue(queue.start(now).isEmpty())

        queue.update("unknown_tid.jpg", progress = 5, total = 10, now = now)
        assertEquals(1, queue.inFlight().size)
        assertTrue(queue.isInFlight(attachment(0).fileId))

        queue.update(attachment(0).fileId, progress = 10, total = 10, now = now)
        assertTrue(queue.isIdle)
        assertFalse(queue.isInFlight(attachment(0).fileId))
    }
}
