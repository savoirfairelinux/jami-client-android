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

import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

/**
 * Paces the attachment downloads of a synchronized account so that a source
 * device is never asked for too many files at once.
 *
 * Pure bookkeeping: the caller starts the downloads it is given by [start] and
 * reports their progress through [update]. A download leaves the in-flight set
 * once complete, or once it has not progressed for [STALL_TIMEOUT_NANOS]; in the
 * latter case the daemon still holds the request and retries it on the next
 * sync with the device, so nothing is lost.
 *
 * Times are monotonic nanoseconds, as returned by [System.nanoTime].
 */
class AttachmentDownloadQueue {
    data class Attachment(
        val accountId: String,
        val conversationId: String,
        val messageId: String,
        val fileId: String,
        val displayName: String
    )

    private class InFlight(val attachment: Attachment, var progress: Long, var lastProgress: Long)

    private val queue = ArrayDeque<Attachment>()
    private val inFlight = LinkedHashMap<String, InFlight>()

    @Synchronized
    fun enqueue(attachment: Attachment) {
        if (isKnown(attachment.fileId)) return
        queue.addLast(attachment)
    }

    /**
     * Moves as many queued attachments as there are free slots to in-flight.
     * @return the attachments whose download must be started now
     */
    @Synchronized
    fun start(now: Long): List<Attachment> {
        val started = ArrayList<Attachment>()
        while (inFlight.size < MAX_CONCURRENT && queue.isNotEmpty()) {
            val attachment = queue.removeFirst()
            inFlight[attachment.fileId] = InFlight(attachment, 0, now)
            started.add(attachment)
        }
        return started
    }

    @Synchronized
    fun update(fileId: String, progress: Long, total: Long, now: Long) {
        val entry = inFlight[fileId] ?: return
        val complete = total > 0 && progress >= total
        if (progress != entry.progress) {
            entry.progress = progress
            entry.lastProgress = now
        }
        if (complete || now - entry.lastProgress >= STALL_TIMEOUT_NANOS)
            inFlight.remove(fileId)
    }

    @Synchronized
    fun inFlight(): List<Attachment> = inFlight.values.map { it.attachment }

    @Synchronized
    fun isInFlight(fileId: String): Boolean = inFlight.containsKey(fileId)

    val isIdle: Boolean
        @Synchronized get() = queue.isEmpty() && inFlight.isEmpty()

    private fun isKnown(fileId: String): Boolean =
        inFlight.containsKey(fileId) || queue.any { it.fileId == fileId }

    companion object {
        const val MAX_CONCURRENT = 3
        val STALL_TIMEOUT_NANOS: Long = TimeUnit.SECONDS.toNanos(10)
    }
}
