package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.CompletableSubject
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PendingIncomingCallTest {
    private val call = Call("account", "call", Uri.fromString("ring:peer"), true).apply {
        setCallState(CallStatus.RINGING)
    }
    private val requests = ConcurrentHashMap<String, PendingIncomingCall>()
    private val updates = PublishSubject.create<Call>()

    private class Connection(private val bind: (Call?) -> Unit = {}) : CallService.SystemCall(true) {
        var boundCall: Call? = null
        var bindingCount = 0
        var disposed = false
        var releaseCount = 0
        val ended = CompletableSubject.create()
        override val termination: Completable get() = ended

        override fun setCall(call: Call?) {
            if (call == null) {
                disposed = true
                releaseCount++
                ended.onComplete()
                return
            }
            bind(call)
            boundCall = call
            bindingCount++
        }
    }

    private fun request(key: String = "request") =
        PendingIncomingCall(call) { requests.remove(key, it) }.also {
            requests[key] = it
            it.trackTermination(updates)
        }

    private fun callback(key: String, connection: Connection, showUi: Boolean = true): Boolean {
        val request = requests[key]
        if (request == null || !request.complete(connection, finalResult = !showUi)) {
            connection.disposed = true
            return false
        }
        return true
    }

    @Test
    fun disposalRemovesPendingRequestAndLateCallbackDisposesConnection() {
        val request = request()
        val observer = request.result.test()
        observer.dispose()
        val connection = Connection()

        assertFalse(callback("request", connection))
        assertTrue(requests.isEmpty())
        assertTrue(connection.disposed)
        assertNull(connection.boundCall)
        assertEquals(CallStatus.RINGING, call.callStatus)
    }

    @Test
    fun callbackThatAlreadyLookedUpRequestCannotCompleteAfterCancellation() {
        val request = request()
        val observer = request.result.test()
        val captured = requests["request"]!!
        observer.dispose()
        val connection = Connection()

        assertFalse(captured.complete(connection))
        assertNull(connection.boundCall)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun callbackBeforeSubscriptionBindsAndCachesSuccess() {
        val request = request()
        val connection = Connection()

        assertTrue(callback("request", connection))
        assertSame(call, connection.boundCall)
        val observer = request.result.test()
        observer.assertValue(connection).assertComplete().assertNoErrors()
        observer.dispose()

        assertSame(request, requests["request"])
        assertFalse(connection.disposed)
        assertEquals(1, connection.bindingCount)
    }

    @Test
    fun disposalAfterShowUiKeepsRequestForLaterAcceptCallback() {
        val request = request()
        val connection = Connection()
        val observer = request.result.flatMapCompletable {
            assertSame(call, connection.boundCall)
            Completable.never()
        }.test()

        assertTrue(callback("request", connection))
        observer.dispose()
        assertSame(request, requests["request"])
        assertTrue(callback("request", connection, showUi = false))
        assertTrue(requests.isEmpty())
        assertEquals(1, connection.bindingCount)
        assertFalse(connection.disposed)
    }

    @Test
    fun disposalAfterShowUiKeepsRequestForLaterRejection() {
        val request = request()
        val connection = Connection()
        val observer = request.result.test()
        assertTrue(callback("request", connection))
        observer.dispose()

        val acceptedRequest = requests["request"]!!
        assertTrue(acceptedRequest.complete(CallService.SystemCall(false), finalResult = true))
        assertEquals(1, connection.bindingCount)
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
        assertFalse(connection.ended.hasObservers())
    }

    @Test
    fun lateCallbackForCancelledGenerationCannotResolveRetry() {
        val old = request("old-generation")
        old.result.test().dispose()
        val retry = request("new-generation")
        val observer = retry.result.test()
        val lateConnection = Connection()
        val currentConnection = Connection()

        assertFalse(callback("old-generation", lateConnection))
        observer.assertNotComplete()
        assertTrue(lateConnection.disposed)
        assertTrue(callback("new-generation", currentConnection))
        observer.assertValue(currentConnection).assertComplete().assertNoErrors()
        assertSame(call, currentConnection.boundCall)
    }

    @Test
    fun cancellationDoesNotRemoveAnotherRequestAtSameKey() {
        val old = request()
        val replacement = request()

        old.result.test().dispose()

        assertSame(replacement, requests["request"])
    }

    @Test
    fun notificationTimeoutCancelsOnlyPendingTelecomRequest() {
        val updates = PublishSubject.create<Call>()
        val scheduler = TestScheduler()
        val errors = mutableListOf<Throwable>()
        val processor = CallNotificationProcessor(updates, scheduler, 30) { _, error ->
            errors.add(error)
        }
        val request = request()
        val observer = processor.notification(call, false) {
            request.result.ignoreElement()
        }.test()

        scheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        observer.assertComplete().assertNoErrors()
        assertTrue(requests.isEmpty())
        val connection = Connection()
        assertFalse(callback("request", connection))
        assertTrue(connection.disposed)
        assertEquals(CallStatus.RINGING, call.callStatus)
        assertTrue(errors.single() is TimeoutException)
    }

    @Test
    fun terminalNotificationCancellationReleasesPendingTelecomRequest() {
        val updates = PublishSubject.create<Call>()
        val processor = CallNotificationProcessor(updates, TestScheduler()) { _, error ->
            throw AssertionError(error)
        }
        val request = request()
        val observer = processor.notification(call, false) {
            request.result.ignoreElement()
        }.test()

        call.setCallState(CallStatus.OVER)
        updates.onNext(call)

        observer.assertComplete().assertNoErrors()
        assertTrue(requests.isEmpty())
        val connection = Connection()
        assertFalse(callback("request", connection))
        assertTrue(connection.disposed)
    }

    @Test
    fun alreadyTerminalCallDoesNotAcquireLateConnection() {
        val request = request()
        val observer = request.result.test()
        call.setCallState(CallStatus.OVER)
        val connection = Connection()

        assertFalse(request.complete(connection))
        assertNull(connection.boundCall)
        assertTrue(requests.isEmpty())
        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertFalse(updates.hasObservers())
    }

    @Test
    fun callTerminatingDuringBindingReleasesRequest() {
        val request = request()
        val observer = request.result.test()
        val connection = Connection { call.setCallState(CallStatus.OVER) }

        assertFalse(callback("request", connection))
        assertTrue(connection.disposed)
        assertEquals(1, connection.releaseCount)
        assertTrue(requests.isEmpty())
        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
    }

    @Test
    fun bindingFailureRemovesRequestAndReportsError() {
        val failure = IllegalStateException("binding")
        val request = request()
        val observer = request.result.test()
        val connection = Connection { throw failure }

        assertFalse(callback("request", connection))
        assertTrue(connection.disposed)
        assertTrue(requests.isEmpty())
        observer.assertError(failure)
        assertEquals(1, connection.releaseCount)
        assertFalse(updates.hasObservers())
    }

    @Test
    fun terminalCallAfterShowUiReleasesResolvedRequestAndConnection() {
        for (status in listOf(CallStatus.HUNGUP, CallStatus.BUSY, CallStatus.FAILURE, CallStatus.OVER)) {
            call.setCallState(CallStatus.RINGING)
            val request = request()
            val observer = request.result.test()
            val connection = Connection()
            assertTrue(callback("request", connection))
            observer.assertValue(connection).assertComplete().assertNoErrors()
            assertTrue(connection.ended.hasObservers())

            call.setCallState(status)
            updates.onNext(call)

            assertTrue(requests.isEmpty())
            assertEquals(1, connection.releaseCount)
            assertFalse(updates.hasObservers())
            assertFalse(connection.ended.hasObservers())
            assertFalse(request.complete(connection, finalResult = true))
            request.result.test().assertValue(connection).assertComplete()
        }
    }

    @Test
    fun disposedConnectionAfterShowUiReleasesResolvedRequestWithoutTerminalCallEvent() {
        val request = request()
        val connection = Connection()
        assertTrue(callback("request", connection))
        connection.ended.onComplete()

        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
        assertFalse(connection.ended.hasObservers())
        assertFalse(request.complete(connection, finalResult = true))
    }

    @Test
    fun rejectedEarlyCallbackReleasesListenersAndSettlesResult() {
        val request = request()
        val observer = request.result.test()

        assertTrue(request.complete(CallService.SystemCall(false), finalResult = true))

        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun answeredCallNoLongerRetainsTheRequestOrItsListeners() {
        val request = request()
        val connection = Connection()
        assertTrue(callback("request", connection))
        assertTrue(callback("request", connection, showUi = false))

        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
        assertFalse(connection.ended.hasObservers())
        assertFalse(connection.disposed)
        assertEquals(1, connection.bindingCount)
        request.cancel()
        assertFalse(connection.disposed)
    }

    @Test
    fun terminalEventBeforeTelecomCallbackSettlesWaitImmediately() {
        val request = request()
        val observer = request.result.test()
        call.setCallState(CallStatus.OVER)
        updates.onNext(call)

        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun terminalCallBeforeTrackingCannotLeaveARegisteredRequest() {
        call.setCallState(CallStatus.OVER)
        val request = request()

        request.result.test().assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun reentrantTerminalEventDuringBindingSettlesAndReleasesExactlyOnce() {
        val request = request()
        val observer = request.result.test()
        val connection = Connection {
            call.setCallState(CallStatus.OVER)
            updates.onNext(call)
        }
        assertFalse(callback("request", connection))

        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertEquals(1, connection.bindingCount)
        assertEquals(1, connection.releaseCount)
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun alreadyDisposedConnectionCannotPublishAnAllowedResult() {
        val request = request()
        val observer = request.result.test()
        val connection = Connection()
        connection.ended.onComplete()

        assertFalse(callback("request", connection))

        observer.assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertEquals(1, connection.releaseCount)
        assertTrue(requests.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun unrelatedTerminalEventsDoNotRemoveResolvedRequest() {
        val request = request()
        val connection = Connection()
        assertTrue(callback("request", connection))
        updates.onNext(Call("other-account", call.id, call.peerUri, true).apply {
            setCallState(CallStatus.OVER)
        })
        updates.onNext(Call(call.account, "other-call", call.peerUri, true).apply {
            setCallState(CallStatus.OVER)
        })
        assertSame(request, requests["request"])
        assertFalse(connection.disposed)
        connection.ended.onComplete()
    }

    @Test
    fun concurrentTerminalAndTelecomCompletionAlwaysReleaseResolvedOrPendingRequest() {
        repeat(100) {
            call.setCallState(CallStatus.RINGING)
            val request = request()
            val observer = request.result.test()
            val connection = Connection()
            val start = CountDownLatch(1)
            val completion = thread {
                start.await(5, TimeUnit.SECONDS)
                callback("request", connection)
            }
            val terminal = thread {
                start.await(5, TimeUnit.SECONDS)
                call.setCallState(CallStatus.OVER)
                updates.onNext(call)
            }
            start.countDown()
            completion.join(5_000)
            terminal.join(5_000)
            assertFalse(completion.isAlive)
            assertFalse(terminal.isAlive)

            observer.assertComplete().assertNoErrors().assertValueCount(1)
            assertTrue(requests.isEmpty())
            assertFalse(updates.hasObservers())
            assertFalse(connection.ended.hasObservers())
            assertTrue(connection.disposed)
            assertTrue(connection.releaseCount <= 1)
        }
    }

    @Test
    fun callbackAndDisposalRaceNeverLeavesAnUnboundOwnedConnection() {
        repeat(100) { iteration ->
            val key = iteration.toString()
            val request = request(key)
            val observer = request.result.test()
            val connection = Connection()
            val start = CountDownLatch(1)
            val completed = AtomicBoolean()
            val completion = thread {
                start.await(5, TimeUnit.SECONDS)
                completed.set(request.complete(connection))
            }
            val disposal = thread {
                start.await(5, TimeUnit.SECONDS)
                observer.dispose()
            }
            start.countDown()
            completion.join(5_000)
            disposal.join(5_000)
            assertFalse(completion.isAlive)
            assertFalse(disposal.isAlive)

            if (completed.get()) {
                assertSame(call, connection.boundCall)
                assertSame(request, requests[key])
            } else {
                assertNull(connection.boundCall)
                assertFalse(requests.containsKey(key))
            }
            connection.ended.onComplete()
            assertFalse(requests.containsKey(key))
            assertFalse(updates.hasObservers())
        }
    }
}
