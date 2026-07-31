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
import net.jami.model.Uri
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
/**
 * The documents a conversation writes together.
 *
 * A conversation is named here by its [Uri] rather than by a string, because
 * the daemon only ever accepts the bare hexadecimal id: handing it the "swarm:"
 * form is rejected silently, and both forms are strings.
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

    /** A document is no longer held here. */
    data class DocumentRemoved(
        val accountId: String,
        val conversationId: String,
        val documentId: String,
        /**
         * True when its author retired it for every member, and nothing brings
         * it back. False when this device alone dropped it: the other members
         * keep it, and opening it again fetches it back.
         */
        val everywhere: Boolean,
    )

    // Serialized: these are fed straight from the daemon's callbacks, which run
    // on whatever thread the daemon happens to be on, and a PublishSubject on
    // its own would let two of them call onNext at once.
    private val documentUpdateSubject: Subject<DocumentUpdate> = PublishSubject.create<DocumentUpdate>().toSerialized()
    private val awarenessSubject: Subject<AwarenessUpdate> = PublishSubject.create<AwarenessUpdate>().toSerialized()
    private val participantLeftSubject: Subject<ParticipantLeft> = PublishSubject.create<ParticipantLeft>().toSerialized()
    private val renamedSubject: Subject<DocumentRenamed> = PublishSubject.create<DocumentRenamed>().toSerialized()
    private val attachmentSubject: Subject<AttachmentAdded> = PublishSubject.create<AttachmentAdded>().toSerialized()
    private val removedSubject: Subject<DocumentRemoved> = PublishSubject.create<DocumentRemoved>().toSerialized()

    // Handed over off the daemon's thread. onNext() runs subscribers where it
    // stands, and these are called from the daemon's own callbacks: a subscriber
    // that reads a database, or asks the daemon something in turn, would be
    // holding up the thread the daemon is waiting to have back. What each one
    // carries is already a copy by then, so the daemon is free to go.
    val documentUpdates: Observable<DocumentUpdate> = documentUpdateSubject.observeOn(Schedulers.io())
    val awarenessUpdates: Observable<AwarenessUpdate> = awarenessSubject.observeOn(Schedulers.io())
    val participantsLeft: Observable<ParticipantLeft> = participantLeftSubject.observeOn(Schedulers.io())
    val documentsRenamed: Observable<DocumentRenamed> = renamedSubject.observeOn(Schedulers.io())
    val attachmentsAdded: Observable<AttachmentAdded> = attachmentSubject.observeOn(Schedulers.io())
    val documentsRemoved: Observable<DocumentRemoved> = removedSubject.observeOn(Schedulers.io())

    /** Updates for one open document, the form an editor subscribes to. */
    fun updatesFor(accountId: String, conversation: Uri, documentId: String): Observable<ByteArray> =
        documentUpdates
            .filter { it.accountId == accountId && it.conversationId == conversation.rawRingId && it.documentId == documentId }
            .map { it.update }

    fun awarenessFor(accountId: String, conversation: Uri, documentId: String): Observable<AwarenessUpdate> =
        awarenessUpdates
            .filter { it.accountId == accountId && it.conversationId == conversation.rawRingId && it.documentId == documentId }

    fun departuresFor(accountId: String, conversation: Uri, documentId: String): Observable<ParticipantLeft> =
        participantsLeft
            .filter { it.accountId == accountId && it.conversationId == conversation.rawRingId && it.documentId == documentId }

    fun attachmentsFor(accountId: String, conversation: Uri, documentId: String): Observable<String> =
        attachmentsAdded
            .filter { it.accountId == accountId && it.conversationId == conversation.rawRingId && it.documentId == documentId }
            .map { it.attachmentId }

    /**
     * Removals of one document, for a screen showing that document alone.
     * The value tells the two removals apart.
     */
    fun removalsFor(accountId: String, conversation: Uri, documentId: String): Observable<Boolean> =
        documentsRemoved
            .filter { it.accountId == accountId && it.conversationId == conversation.rawRingId && it.documentId == documentId }
            .map { it.everywhere }

    /**
     * Announce a new document in a conversation.
     * @return its id, empty when the daemon refused to create it.
     */
    fun createDocument(
        accountId: String,
        conversation: Uri,
        name: String,
        mimeType: String = CollaborativeDocument.MIME_RICH_TEXT,
    ): Single<String> = fromDaemon {
        JamiService.createCollaborativeDocument(accountId, conversation.rawRingId, name, mimeType)
    }

    /**
     * Start an editing session and get the whole document as one Y-CRDT update,
     * to seed a fresh replica. Must be paired with [closeDocument].
     */
    fun openDocument(accountId: String, conversation: Uri, documentId: String): Single<ByteArray> =
        fromDaemon {
            JamiService.openCollaborativeDocument(accountId, conversation.rawRingId, documentId).takeBytes()
        }

    fun closeDocument(accountId: String, conversation: Uri, documentId: String): Completable =
        onDaemon { JamiService.closeCollaborativeDocument(accountId, conversation.rawRingId, documentId) }

    /**
     * Retire a document from the conversation, for every member and every
     * device.
     *
     * Only its author can: the removal is an edition of the announcement, and
     * the swarm takes an edition only from the author of what it edits. The
     * answer says the removal was committed, not that the members applied it:
     * [documentsRemoved] reports that, here as everywhere else.
     */
    fun removeDocument(accountId: String, conversation: Uri, documentId: String): Single<Boolean> =
        fromDaemon { JamiService.removeCollaborativeDocument(accountId, conversation.rawRingId, documentId) }

    /**
     * Drop a document from this device alone, leaving the other members with
     * it. Any member may, on any document: nothing is said to the conversation.
     *
     * The document stays listed, with [CollaborativeDocument.storedLocally]
     * false, and opening it fetches it back.
     */
    fun removeDocumentLocally(accountId: String, conversation: Uri, documentId: String): Single<Boolean> =
        fromDaemon { JamiService.removeCollaborativeDocumentLocally(accountId, conversation.rawRingId, documentId) }

    /** Broadcast a local edit. The update is a Y-CRDT update, lib0 v1 encoding. */
    fun applyUpdate(
        accountId: String,
        conversation: Uri,
        documentId: String,
        update: ByteArray,
    ): Completable = onDaemon {
        withBlob(update) { JamiService.applyCollaborativeUpdate(accountId, conversation.rawRingId, documentId, it) }
    }

    fun documentState(accountId: String, conversation: Uri, documentId: String): Single<ByteArray> =
        fromDaemon {
            JamiService.collaborativeDocumentState(accountId, conversation.rawRingId, documentId).takeBytes()
        }

    /**
     * Publish this device's ephemeral state for a document. The daemon relays it
     * untouched, so its shape is a matter between clients; the editors use
     * `{"p":<caret>,"a":<anchor>}` in UTF-16 code units. An empty state
     * withdraws this device's cursor.
     */
    fun setAwareness(accountId: String, conversation: Uri, documentId: String, state: String): Completable =
        onDaemon { JamiService.setCollaborativeAwareness(accountId, conversation.rawRingId, documentId, state) }

    fun setName(accountId: String, conversation: Uri, documentId: String, name: String): Completable =
        onDaemon { JamiService.setCollaborativeDocumentName(accountId, conversation.rawRingId, documentId, name) }

    fun name(accountId: String, conversation: Uri, documentId: String): Single<String> =
        fromDaemon { JamiService.collaborativeDocumentName(accountId, conversation.rawRingId, documentId) }

    /** Every document announced in a conversation, newest first. */
    fun documents(accountId: String, conversation: Uri): Single<List<CollaborativeDocument>> =
        fromDaemon {
            val vect = JamiService.getCollaborativeDocuments(accountId, conversation.rawRingId)
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
        conversation: Uri,
        documentId: String,
        max: Int = 0,
    ): Single<List<CollaborativeVersion>> = fromDaemon {
        val vect = JamiService.getCollaborativeDocumentHistory(accountId, conversation.rawRingId, documentId, max.toLong())
        try {
            vect.toNative().mapNotNull { CollaborativeVersion.fromNative(it) }
        } finally {
            vect.delete()
        }
    }

    /** The document as it stood at a checkpoint, as a Y-CRDT update. */
    fun stateAt(
        accountId: String,
        conversation: Uri,
        documentId: String,
        commitId: String,
    ): Single<ByteArray> = fromDaemon {
        JamiService.collaborativeDocumentStateAt(accountId, conversation.rawRingId, documentId, commitId).takeBytes()
    }

    /**
     * Store bytes in the document's repository.
     * @return the attachment id to reference from the document, empty on refusal.
     */
    fun addAttachment(
        accountId: String,
        conversation: Uri,
        documentId: String,
        data: ByteArray,
    ): Single<String> = fromDaemon {
        withBlob(data) { JamiService.addCollaborativeAttachment(accountId, conversation.rawRingId, documentId, it) }
    }

    /**
     * An attachment's bytes, empty while it has not reached this device yet;
     * [attachmentsFor] then says when to ask again.
     */
    fun attachment(
        accountId: String,
        conversation: Uri,
        documentId: String,
        attachmentId: String,
    ): Single<ByteArray> = fromDaemon {
        JamiService.collaborativeAttachment(accountId, conversation.rawRingId, documentId, attachmentId).takeBytes()
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

    fun documentRemoved(accountId: String, conversationId: String, documentId: String, everywhere: Boolean) {
        removedSubject.onNext(DocumentRemoved(accountId, conversationId, documentId, everywhere))
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
