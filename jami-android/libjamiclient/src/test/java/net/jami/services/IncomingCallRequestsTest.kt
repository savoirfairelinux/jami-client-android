package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Uri
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class IncomingCallRequestsTest {
    private val updates = PublishSubject.create<Call>()
    private val requests = IncomingCallRequests(updates)
    private val generations = mutableListOf<String>()
    private val call = incoming()

    private fun incoming(account: String = "account", id: String = "call") =
        Call(account, id, Uri.fromString("ring:peer"), true).apply {
            setCallState(CallStatus.RINGING)
        }

    private class Connection : CallService.SystemCall(true) {
        val ended = CompletableSubject.create()
        var bindings = 0
        var releases = 0
        override val termination: Completable get() = ended

        override fun setCall(call: Call?) {
            if (call != null) {
                bindings++
                call.setSystemConnection(this)
            } else {
                releases++
                ended.onComplete()
            }
        }
    }

    @Before
    fun configureLogger() {
        Log.injectLogService(object : LogService {
            override fun d(tag: String, message: String) {}
            override fun i(tag: String, message: String) {}
            override fun w(tag: String, message: String) {}
            override fun e(tag: String, message: String) {}
            override fun d(tag: String, message: String, e: Throwable) {}
            override fun i(tag: String, message: String, e: Throwable) {}
            override fun w(tag: String, message: String, e: Throwable) {}
            override fun e(tag: String, message: String, e: Throwable) {}
        })
    }

    @Test
    fun repeatedRingingWhilePendingStartsTelecomOnlyOnce() {
        val first = requests.request(call, generations::add).test()
        val second = requests.request(call, generations::add).test()
        assertEquals(1, generations.size)
        val connection = Connection()

        assertTrue(requests[generations.single()]!!.complete(connection))

        first.assertValue(connection).assertComplete().assertNoErrors()
        second.assertValue(connection).assertComplete().assertNoErrors()
        assertEquals(1, connection.bindings)
        connection.ended.onComplete()
        assertFalse(updates.hasObservers())
    }

    @Test
    fun repeatedRingingAfterShowUiReusesTheBoundConnection() {
        val observer = requests.request(call, generations::add).test()
        val generation = generations.single()
        val pending = requests[generation]!!
        val connection = Connection()
        pending.complete(connection)
        observer.assertValue(connection)

        repeat(5) {
            requests.request(call, generations::add).test()
                .assertValue(connection).assertComplete().assertNoErrors()
        }

        assertEquals(1, generations.size)
        assertEquals(1, connection.bindings)
        assertSame(pending, requests[generation])
        assertSame(connection, call.resolvedSystemConnection)
        connection.ended.onComplete()
        assertNull(requests[generation])
        assertFalse(updates.hasObservers())
    }

    @Test
    fun ringingBetweenAnswerAndCurrentCannotCreateAnotherConnection() {
        requests.request(call, generations::add).test()
        val generation = generations.single()
        val pending = requests[generation]!!
        val connection = Connection()
        pending.complete(connection)
        assertTrue(pending.complete(connection, finalResult = true))
        assertNull(requests[generation])
        assertFalse(updates.hasObservers())

        requests.request(call, generations::add).test().assertValue(connection).assertComplete()

        assertEquals(CallStatus.RINGING, call.callStatus)
        assertEquals(1, generations.size)
        assertEquals(1, connection.bindings)
        assertEquals(0, connection.releases)
    }

    @Test
    fun cancellingOneSharedWaitDoesNotAbandonTheOther() {
        val first = requests.request(call, generations::add).test()
        val second = requests.request(call, generations::add).test()
        val generation = generations.single()
        val pending = requests[generation]!!
        first.dispose()
        assertSame(pending, requests[generation])
        val connection = Connection()
        assertTrue(pending.complete(connection))
        second.assertValue(connection).assertComplete().assertNoErrors()
        connection.ended.onComplete()
        assertNull(requests[generation])
    }

    @Test
    fun lastWaiterCancellationAllowsRetryWithoutAcceptingAnOldGeneration() {
        val first = requests.request(call, generations::add).test()
        val second = requests.request(call, generations::add).test()
        val oldGeneration = generations.single()
        val oldRequest = requests[oldGeneration]!!
        first.dispose()
        second.dispose()
        assertNull(requests[oldGeneration])

        val retried = requests.request(call, generations::add).test()
        val newGeneration = generations.last()
        assertNotEquals(oldGeneration, newGeneration)
        assertFalse(oldRequest.complete(Connection()))
        assertNull(requests[oldGeneration])
        val connection = Connection()
        assertTrue(requests[newGeneration]!!.complete(connection))
        retried.assertValue(connection).assertComplete()
        connection.ended.onComplete()
        assertNull(requests[newGeneration])
        assertFalse(updates.hasObservers())
    }

    @Test
    fun callsAndAccountsUseSeparateRegistrations() {
        val first = requests.request(call, generations::add).test()
        val otherAccount = requests.request(incoming(account = "other"), generations::add).test()
        val otherCall = requests.request(incoming(id = "other"), generations::add).test()
        assertEquals(3, generations.toSet().size)
        first.dispose()
        otherAccount.dispose()
        otherCall.dispose()
        assertTrue(generations.all { requests[it] == null })
        assertFalse(updates.hasObservers())
    }

    @Test
    fun duplicateNativeIdReusesPendingRequestEvenWithAnotherCallInstance() {
        val first = requests.request(call, generations::add).test()
        val second = requests.request(incoming(), generations::add).test()
        assertEquals(1, generations.size)
        first.dispose()
        second.dispose()
        assertNull(requests[generations.single()])
    }

    @Test
    fun terminalCallDoesNotStartTelecomOrLeaveARegistration() {
        call.setCallState(CallStatus.OVER)
        requests.request(call, generations::add).test()
            .assertValue { !it.allowed }.assertComplete().assertNoErrors()
        assertTrue(generations.isEmpty())
        assertFalse(updates.hasObservers())
    }

    @Test
    fun terminatedResolvedCallDoesNotReuseItsDisposedConnection() {
        requests.request(call, generations::add).test()
        val connection = Connection()
        requests[generations.single()]!!.complete(connection)
        call.setCallState(CallStatus.OVER)
        updates.onNext(call)

        requests.request(call, generations::add).test()
            .assertValue { !it.allowed }.assertComplete().assertNoErrors()

        assertEquals(1, generations.size)
        assertEquals(1, connection.releases)
        assertFalse(updates.hasObservers())
    }

    @Test
    fun synchronousTelecomFailureReleasesRegistrationForRetry() {
        val failure = SecurityException("Telecom denied")
        try {
            requests.request(call) {
                generations.add(it)
                throw failure
            }
            throw AssertionError("Telecom failure should be propagated")
        } catch (error: SecurityException) {
            assertSame(failure, error)
        }
        assertNull(requests[generations.single()])
        assertFalse(updates.hasObservers())
        val observer = requests.request(call, generations::add).test()
        assertEquals(2, generations.size)
        observer.dispose()
        assertNull(requests[generations.last()])
    }

    @Test
    fun earlyTelecomCallbackBeforeSubscriptionIsReused() {
        val connection = Connection()
        val result = requests.request(call) {
            generations.add(it)
            assertTrue(requests[it]!!.complete(connection))
        }
        result.test().assertValue(connection).assertComplete()
        requests.request(call, generations::add).test().assertValue(connection)
        assertEquals(1, generations.size)
        assertEquals(1, connection.bindings)
        connection.ended.onComplete()
    }

    @Test
    fun concurrentRingingUpdatesCreateOnlyOneGeneration() {
        val start = CountDownLatch(1)
        val started = AtomicInteger()
        val ready = CountDownLatch(8)
        val threads = (0 until 8).map {
            thread {
                start.await(5, TimeUnit.SECONDS)
                requests.request(call) { started.incrementAndGet() }
                ready.countDown()
            }
        }
        start.countDown()
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        threads.forEach {
            it.join(5_000)
            assertFalse(it.isAlive)
        }
        assertEquals(1, started.get())
        call.setCallState(CallStatus.OVER)
        updates.onNext(call)
        assertFalse(updates.hasObservers())
    }
}
