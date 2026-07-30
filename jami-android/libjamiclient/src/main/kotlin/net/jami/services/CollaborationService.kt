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
package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.Blob
import net.jami.daemon.JamiService
import net.jami.model.CollaborativeDocument
import net.jami.model.CollaborativeVersion
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

/**
 * Client side of the daemon's collaborative editing API.
 *
 * The daemon never hands out text: a document's content only ever crosses this
 * boundary as opaque Y-CRDT updates, which the editor merges into its own
 * replica. This service is therefore a thin, byte-faithful transport, plus the
 * document metadata a conversation needs to list what can be opened.
 *
 * Every daemon call is dispatched on the daemon executor, as the rest of the
 * client does; the returned Rx types carry the answer back.
 */
class CollaborationService(private val executor: ExecutorService) {

    /** A Y-CRDT update for a document this device has open. */
    data class DocumentUpdate(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        val update: ByteArray,
    ) {
        // Identity, as for any message: two updates are never interchangeable
        // even when their bytes coincide, and comparing megabyte payloads to
        // find that out would be pure waste.
        override fun equals(other: Any?) = this === other
        override fun hashCode() = System.identityHashCode(this)
    }

    /** A collaborator's ephemeral state (cursor, selection) for a document. */
    data class AwarenessUpdate(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        val peerId: String,
        val clientId: Long,
        val state: String,
    )

    /** A collaborator stopped editing a document. */
    data class ParticipantLeft(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        val peerId: String,
        val clientId: Long,
    )

    data class DocumentRenamed(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        val name: String,
    )

    data class AttachmentAdded(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        val attachmentId: String,
    )

    private val documentUpdateSubject: Subject<DocumentUpdate> = PublishSubject.create()
    private val awarenessSubject: Subject<AwarenessUpdate> = PublishSubject.create()
    private val participantLeftSubject: Subject<ParticipantLeft> = PublishSubject.create()
    private val renamedSubject: Subject<DocumentRenamed> = PublishSubject.create()
    private val attachmentSubject: Subject<AttachmentAdded> = PublishSubject.create()

    val documentUpdates: Observable<DocumentUpdate> = documentUpdateSubject
    val awarenessUpdates: Observable<AwarenessUpdate> = awarenessSubject
    val participantsLeft: Observable<ParticipantLeft> = participantLeftSubject
    val documentsRenamed: Observable<DocumentRenamed> = renamedSubject
    val attachmentsAdded: Observable<AttachmentAdded> = attachmentSubject

    /** Updates for one open document, the form an editor subscribes to. */
    fun updatesFor(accountId: String, conversationId: String, documentId: String): Observable<ByteArray> =
        documentUpdates
            .filter { it.accountId == accountId && it.conversationId == conversationId && it.documentId == documentId }
            .map { it.update }

    fun awarenessFor(accountId: String, conversationId: String, documentId: String): Observable<AwarenessUpdate> =
        awarenessUpdates
            .filter { it.accountId == accountId && it.conversationId == conversationId && it.documentId == documentId }

    fun departuresFor(accountId: String, conversationId: String, documentId: String): Observable<ParticipantLeft> =
        participantsLeft
            .filter { it.accountId == accountId && it.conversationId == conversationId && it.documentId == documentId }

    fun attachmentsFor(accountId: String, conversationId: String, documentId: String): Observable<String> =
        attachmentsAdded
            .filter { it.accountId == accountId && it.conversationId == conversationId && it.documentId == documentId }
            .map { it.attachmentId }

    /**
     * Announce a new document in a conversation.
     * @return its id, empty when the daemon refused to create it.
     */
    fun createDocument(
        accountId: String,
        conversationId: String,
        name: String,
        mimeType: String = CollaborativeDocument.MIME_RICH_TEXT,
    ): Single<String> = fromDaemon {
        JamiService.createCollaborativeDocument(accountId, conversationId, name, mimeType)
    }

    /**
     * Start an editing session and get the whole document as one Y-CRDT update,
     * to seed a fresh replica. Must be paired with [closeDocument].
     */
    fun openDocument(accountId: String, conversationId: String, documentId: String): Single<ByteArray> =
        fromDaemon {
            JamiService.openCollaborativeDocument(accountId, conversationId, documentId).takeBytes()
        }

    fun closeDocument(accountId: String, conversationId: String, documentId: String): Completable =
        onDaemon { JamiService.closeCollaborativeDocument(accountId, conversationId, documentId) }

    /** Broadcast a local edit. The update is a Y-CRDT update, lib0 v1 encoding. */
    fun applyUpdate(
        accountId: String,
        conversationId: String,
        documentId: String,
        update: ByteArray,
    ): Completable = onDaemon {
        withBlob(update) { JamiService.applyCollaborativeUpdate(accountId, conversationId, documentId, it) }
    }

    fun documentState(accountId: String, conversationId: String, documentId: String): Single<ByteArray> =
        fromDaemon {
            JamiService.collaborativeDocumentState(accountId, conversationId, documentId).takeBytes()
        }

    /**
     * Publish this device's ephemeral state for a document. The daemon relays it
     * untouched, so its shape is a matter between clients; the editors use
     * `{"p":<caret>,"a":<anchor>}` in UTF-16 code units. An empty state
     * withdraws this device's cursor.
     */
    fun setAwareness(accountId: String, conversationId: String, documentId: String, state: String): Completable =
        onDaemon { JamiService.setCollaborativeAwareness(accountId, conversationId, documentId, state) }

    fun setName(accountId: String, conversationId: String, documentId: String, name: String): Completable =
        onDaemon { JamiService.setCollaborativeDocumentName(accountId, conversationId, documentId, name) }

    fun name(accountId: String, conversationId: String, documentId: String): Single<String> =
        fromDaemon { JamiService.collaborativeDocumentName(accountId, conversationId, documentId) }

    /** Every document announced in a conversation, newest first. */
    fun documents(accountId: String, conversationId: String): Single<List<CollaborativeDocument>> =
        fromDaemon {
            val vect = JamiService.getCollaborativeDocuments(accountId, conversationId)
            try {
                vect.toNative().mapNotNull { CollaborativeDocument.fromNative(it) }
            } finally {
                vect.delete()
            }
        }

    /**
     * Checkpoints of a document, newest first.
     * @param max 0 for the whole history.
     */
    fun history(
        accountId: String,
        conversationId: String,
        documentId: String,
        max: Int = 0,
    ): Single<List<CollaborativeVersion>> = fromDaemon {
        val vect = JamiService.getCollaborativeDocumentHistory(accountId, conversationId, documentId, max.toLong())
        try {
            vect.toNative().mapNotNull { CollaborativeVersion.fromNative(it) }
        } finally {
            vect.delete()
        }
    }

    /** The document as it stood at a checkpoint, as a Y-CRDT update. */
    fun stateAt(
        accountId: String,
        conversationId: String,
        documentId: String,
        commitId: String,
    ): Single<ByteArray> = fromDaemon {
        JamiService.collaborativeDocumentStateAt(accountId, conversationId, documentId, commitId).takeBytes()
    }

    /**
     * Store bytes in the document's repository.
     * @return the attachment id to reference from the document, empty on refusal.
     */
    fun addAttachment(
        accountId: String,
        conversationId: String,
        documentId: String,
        data: ByteArray,
    ): Single<String> = fromDaemon {
        withBlob(data) { JamiService.addCollaborativeAttachment(accountId, conversationId, documentId, it) }
    }

    /**
     * An attachment's bytes, empty while it has not reached this device yet;
     * [attachmentsFor] then says when to ask again.
     */
    fun attachment(
        accountId: String,
        conversationId: String,
        documentId: String,
        attachmentId: String,
    ): Single<ByteArray> = fromDaemon {
        JamiService.collaborativeAttachment(accountId, conversationId, documentId, attachmentId).takeBytes()
    }

    // Called from DaemonService, on the daemon's own thread. The Blob belongs to
    // the caller and dies when it returns, so its bytes are copied out here
    // rather than anywhere downstream.
    fun documentUpdate(accountId: String, conversationId: String, documentId: String, update: Blob) {
        documentUpdateSubject.onNext(
            DocumentUpdate(accountId, conversationId, documentId, update.bytes)
        )
    }

    fun awarenessChanged(
        accountId: String, conversationId: String, documentId: String,
        peerId: String, clientId: Long, state: String,
    ) {
        awarenessSubject.onNext(
            AwarenessUpdate(accountId, conversationId, documentId, peerId, clientId, state)
        )
    }

    fun participantLeft(
        accountId: String, conversationId: String, documentId: String,
        peerId: String, clientId: Long,
    ) {
        participantLeftSubject.onNext(
            ParticipantLeft(accountId, conversationId, documentId, peerId, clientId)
        )
    }

    fun documentRenamed(accountId: String, conversationId: String, documentId: String, name: String) {
        renamedSubject.onNext(DocumentRenamed(accountId, conversationId, documentId, name))
    }

    fun attachmentAdded(accountId: String, conversationId: String, documentId: String, attachmentId: String) {
        attachmentSubject.onNext(AttachmentAdded(accountId, conversationId, documentId, attachmentId))
    }

    private fun <T : Any> fromDaemon(block: Callable<T>): Single<T> =
        Single.fromCallable(block).subscribeOn(scheduler)

    private fun onDaemon(block: () -> Unit): Completable =
        Completable.fromAction(block).subscribeOn(scheduler)

    private val scheduler = Schedulers.from(executor)

    companion object {
        /**
         * Take a returned Blob's bytes and free it at once. SWIG owns the C++
         * vector until then, and documents are large enough that waiting for a
         * finalizer would keep whole copies alive for no reason.
         */
        private fun Blob.takeBytes(): ByteArray = try {
            bytes
        } finally {
            delete()
        }

        private val Blob.bytes: ByteArray
            get() = getBytes() ?: ByteArray(0)

        /** Hand bytes to the daemon through a Blob that does not outlive the call. */
        private inline fun <T> withBlob(data: ByteArray, block: (Blob) -> T): T {
            // Blob(byte[]) appends one element per JNI call; setBytes() is a
            // single one, which for a megabyte update is the whole difference.
            val blob = Blob()
            return try {
                blob.setBytes(data)
                block(blob)
            } finally {
                blob.delete()
            }
        }
    }
}
