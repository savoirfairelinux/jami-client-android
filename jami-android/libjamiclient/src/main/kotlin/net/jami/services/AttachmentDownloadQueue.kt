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
 * Pure bookkeeping: the caller starts the downloads it is given by [start],
 * reports their progress through [update] and the daemon's verdict through
 * [finish] or [suspend]. A download occupies one of the [MAX_CONCURRENT] slots
 * while in flight; it frees it once complete, or once it has not progressed for
 * [STALL_TIMEOUT_NANOS], in which case it stays pending: the daemon still holds
 * the request and retries it on the next sync with the device, so the attachment
 * is only over once [finish] says so.
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

    private class Queued(val attachment: Attachment, val attempts: Int)
    private class InFlight(val attachment: Attachment, val attempts: Int, var progress: Long, var lastProgress: Long)

    private val queue = ArrayDeque<Queued>()
    private val inFlight = LinkedHashMap<String, InFlight>()
    private val pending = LinkedHashMap<String, Attachment>()

    @Synchronized
    fun enqueue(attachment: Attachment) {
        if (isKnown(attachment.fileId)) return
        queue.addLast(Queued(attachment, 0))
    }

    /**
     * Moves as many queued attachments as there are free slots to in-flight.
     * @return the attachments whose download must be started now
     */
    @Synchronized
    fun start(now: Long): List<Attachment> {
        val started = ArrayList<Attachment>()
        while (inFlight.size < MAX_CONCURRENT && queue.isNotEmpty()) {
            val queued = queue.removeFirst()
            inFlight[queued.attachment.fileId] = InFlight(queued.attachment, queued.attempts + 1, 0, now)
            started.add(queued.attachment)
        }
        return started
    }

    @Synchronized
    fun update(fileId: String, progress: Long, total: Long, now: Long) {
        val entry = inFlight[fileId] ?: return
        if (progress != entry.progress) {
            entry.progress = progress
            entry.lastProgress = now
        }
        if (total > 0 && progress >= total)
            inFlight.remove(fileId)
        else if (now - entry.lastProgress >= STALL_TIMEOUT_NANOS)
            suspend(fileId)
    }

    /** The daemon is done with the request (file received, path conflict, or canceled by the user): the attachment is over. */
    @Synchronized
    fun finish(fileId: String) {
        inFlight.remove(fileId)
        pending.remove(fileId)
    }

    /** The daemon lost the peer but keeps the request for a later retry: the attachment frees its slot and stays pending. */
    @Synchronized
    fun suspend(fileId: String) {
        val entry = inFlight.remove(fileId) ?: return
        pending[fileId] = entry.attachment
    }

    /**
     * The download request never reached the daemon: put the attachment back in the queue.
     * @return false if the attachment is given up after [MAX_ATTEMPTS]
     */
    @Synchronized
    fun requeue(fileId: String): Boolean {
        val entry = inFlight.remove(fileId) ?: return false
        if (entry.attempts >= MAX_ATTEMPTS) return false
        queue.addLast(Queued(entry.attachment, entry.attempts))
        return true
    }

    /** Forgets every attachment of a removed conversation. */
    @Synchronized
    fun discardConversation(accountId: String, conversationId: String) =
        discard { it.accountId == accountId && it.conversationId == conversationId }

    /** Forgets every attachment of a removed account. */
    @Synchronized
    fun discardAccount(accountId: String) = discard { it.accountId == accountId }

    @Synchronized
    fun inFlight(): List<Attachment> = inFlight.values.map { it.attachment }

    /** @return whether the daemon has been asked for the file (in flight or pending) */
    @Synchronized
    fun isRequested(fileId: String): Boolean = fileId in inFlight || fileId in pending

    /** Whether nothing is left to poll or to start (pending attachments need neither). */
    val isIdle: Boolean
        @Synchronized get() = queue.isEmpty() && inFlight.isEmpty()

    /** @return whether the conversation still has attachments queued, in flight or pending */
    @Synchronized
    fun hasWork(conversationId: String): Boolean =
        queue.any { it.attachment.conversationId == conversationId } ||
            inFlight.values.any { it.attachment.conversationId == conversationId } ||
            pending.values.any { it.conversationId == conversationId }

    private fun discard(matches: (Attachment) -> Boolean) {
        queue.removeAll { matches(it.attachment) }
        inFlight.values.removeAll { matches(it.attachment) }
        pending.values.removeAll(matches)
    }

    private fun isKnown(fileId: String): Boolean =
        isRequested(fileId) || queue.any { it.attachment.fileId == fileId }

    companion object {
        const val MAX_CONCURRENT = 3
        const val MAX_ATTEMPTS = 3
        val STALL_TIMEOUT_NANOS: Long = TimeUnit.SECONDS.toNanos(10)
    }
}
