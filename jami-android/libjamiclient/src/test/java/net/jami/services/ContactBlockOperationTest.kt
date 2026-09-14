package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import net.jami.model.ContactBlockingPolicy
import net.jami.model.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactBlockOperationTest {
    private val own = "0123456789abcdef0123456789abcdef01234567"
    private val peer = Uri.fromId("abcdef0123456789abcdef0123456789abcdef01")
    private val events = mutableListOf<String>()
    private var allowed = true
    private var nativeBanned = false
    private var nativeAccepts = true
    private val operation = ContactBlockOperation(
        { account, target -> account == "account" && allowed && ContactBlockingPolicy.canBlock(own, target, true) },
        { account, target ->
            assertEquals("account", account)
            assertEquals(peer, target)
            events.add("block")
            if (nativeAccepts) nativeBanned = true
        },
        { _, _ ->
            events.add("confirm")
            nativeBanned
        }
    )

    private fun blockRequest(target: Single<Uri>): Completable =
        ContactBlockOperation.afterSuccess(
            target.flatMapCompletable { operation.block("account", it) }
        ) { events.add("discard") }

    @Test
    fun unresolvedTargetDoesNotDiscardInvitation() {
        val target = SingleSubject.create<Uri>()
        val observer = blockRequest(target).test()
        observer.assertNotComplete()
        assertTrue(events.isEmpty())
        target.onError(IllegalArgumentException("No external contact"))
        observer.assertError(IllegalArgumentException::class.java)
        assertTrue(events.isEmpty())
    }

    @Test
    fun asynchronouslyResolvedTargetBlocksAndConfirmsBeforeDiscarding() {
        val target = SingleSubject.create<Uri>()
        val observer = blockRequest(target).test()
        assertTrue(events.isEmpty())
        target.onSuccess(peer)
        observer.assertComplete().assertNoErrors()
        assertEquals(listOf("block", "confirm", "discard"), events)
    }

    @Test
    fun nativeNoOpOrRejectionPreservesInvitation() {
        nativeAccepts = false
        blockRequest(Single.just(peer)).test().assertError(IllegalStateException::class.java)
        assertFalse(nativeBanned)
        assertEquals(listOf("block", "confirm"), events)
    }

    @Test
    fun selfIdentityAndMissingAccountDoNotReachNativeOrDiscard() {
        blockRequest(Single.just(Uri.fromId(own))).test().assertError(IllegalStateException::class.java)
        operation.block("missing", peer).test().assertError(IllegalStateException::class.java)
        assertTrue(events.isEmpty())
    }

    @Test
    fun accountValidityIsRecheckedWhenScheduledWorkRuns() {
        val scheduler = TestScheduler()
        val observer = ContactBlockOperation.afterSuccess(
            operation.block("account", peer).subscribeOn(scheduler)
        ) { events.add("discard") }.test()
        allowed = false
        scheduler.triggerActions()
        observer.assertError(IllegalStateException::class.java)
        assertTrue(events.isEmpty())
    }

    @Test
    fun asynchronousBlockFailureNeverDiscards() {
        val native = CompletableSubject.create()
        val observer = ContactBlockOperation.afterSuccess(native) { events.add("discard") }.test()
        observer.assertNotComplete()
        native.onError(IllegalStateException("Block failed"))
        observer.assertError(IllegalStateException::class.java)
        assertTrue(events.isEmpty())
    }

    @Test
    fun nativeExceptionIsPropagatedWithoutDiscard() {
        val failure = IllegalStateException("Native write failed")
        val failing = ContactBlockOperation({ _, _ -> true }, { _, _ -> throw failure }, { _, _ -> true })
        val observer = ContactBlockOperation.afterSuccess(failing.block("account", peer)) {
            events.add("discard")
        }.test()
        observer.assertError { it === failure }
        assertTrue(events.isEmpty())
    }

    @Test
    fun alreadyBlockedContactStillCompletesBeforeDiscard() {
        nativeBanned = true
        blockRequest(Single.just(peer)).test().assertComplete().assertNoErrors()
        assertEquals(listOf("block", "confirm", "discard"), events)
    }

    @Test
    fun cachedAcceptedOperationSurvivesViewDisposalWithoutRepeatingSideEffects() {
        val scheduler = TestScheduler()
        val request = ContactBlockOperation.afterSuccess(
            operation.block("account", peer).subscribeOn(scheduler)
        ) { events.add("discard") }.cache()
        val observer = request.test()
        observer.dispose()
        scheduler.triggerActions()
        request.test().assertComplete().assertNoErrors()
        assertEquals(listOf("block", "confirm", "discard"), events)
    }
}
