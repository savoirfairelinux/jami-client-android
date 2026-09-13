package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CallNotificationProcessorTest {
    private val updates = PublishSubject.create<Call>()
    private val scheduler = TestScheduler()
    private val errors = mutableListOf<Throwable>()
    private val processor = CallNotificationProcessor(updates, scheduler, 30) { _, error ->
        errors.add(error)
    }

    private fun call(id: String? = "call", account: String = "account") =
        Call(account, id, Uri.fromString("ring:peer"), false).apply {
            setCallState(CallStatus.CURRENT)
        }

    @Test
    fun notificationsStaySerializedUntilReady() {
        val first = call("first")
        val ready = CompletableSubject.create()
        val started = mutableListOf<String?>()
        val observer = processor.process { call ->
            processor.notification(call, false) {
                started.add(call.id)
                if (call === first) ready else Completable.complete()
            }
        }.test()

        updates.onNext(first)
        updates.onNext(call("second"))
        scheduler.advanceTimeBy(29, TimeUnit.SECONDS)
        assertEquals(listOf("first"), started)
        ready.onComplete()
        assertEquals(listOf("first", "second"), started)
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertTrue(errors.isEmpty())
        updates.onComplete()
        observer.assertComplete().assertNoErrors()
    }

    @Test
    fun timeoutDisposesWaitAndProcessesNextCallWithoutStartingStaleNotification() {
        val first = call("first")
        val ready = CompletableSubject.create()
        val started = mutableListOf<String?>()
        val observer = processor.process { call ->
            processor.notification(call, false) {
                val permission = if (call === first) ready else Completable.complete()
                permission.andThen(Completable.fromAction { started.add(call.id) })
            }
        }.test()

        updates.onNext(first)
        updates.onNext(call("second"))
        assertTrue(ready.hasObservers())
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        assertFalse(ready.hasObservers())
        assertEquals(listOf("second"), started)
        assertTrue(first.isOnGoing)
        ready.onComplete()
        assertEquals(listOf("second"), started)
        assertEquals(1, errors.size)
        assertTrue(errors.single() is TimeoutException)
        updates.onComplete()
        observer.assertComplete().assertNoErrors()
    }

    @Test
    fun rawTerminalUpdateCancelsPendingWorkBeforeQueuedTerminalHandler() {
        val call = call()
        val ready = CompletableSubject.create()
        val handled = mutableListOf<CallStatus>()
        val observer = processor.process { update ->
            val state = update.callStatus
            processor.notification(update, state.isOver) {
                handled.add(state)
                if (state.isOver) Completable.complete() else ready
            }
        }.test()

        updates.onNext(call)
        call.setCallState(CallStatus.OVER)
        updates.onNext(call)

        assertFalse(ready.hasObservers())
        assertEquals(listOf(CallStatus.CURRENT, CallStatus.OVER), handled)
        assertTrue(errors.isEmpty())
        updates.onComplete()
        observer.assertComplete().assertNoErrors()
    }

    @Test
    fun unrelatedTerminalCallsDoNotCancelWait() {
        val call = call()
        val ready = CompletableSubject.create()
        val observer = processor.notification(call, false) { ready }.test()

        updates.onNext(call("other").apply { setCallState(CallStatus.OVER) })
        updates.onNext(call(account = "other-account").apply { setCallState(CallStatus.OVER) })
        updates.onNext(call().apply { setCallState(CallStatus.HOLD) })
        observer.assertNotComplete()
        assertTrue(ready.hasObservers())

        updates.onNext(call().apply { setCallState(CallStatus.HUNGUP) })
        observer.assertComplete().assertNoErrors()
        assertFalse(ready.hasObservers())
    }

    @Test
    fun virtualHostIsCancelledOnlyByItsOwnTerminalUpdate() {
        val host = call(null).apply { confId = "conference" }
        val otherHost = call(null).apply { confId = "conference" }
        val ready = CompletableSubject.create()
        val observer = processor.notification(host, false) { ready }.test()

        otherHost.setCallState(CallStatus.OVER)
        updates.onNext(otherHost)
        observer.assertNotComplete()
        host.confId = null
        host.setCallState(CallStatus.OVER)
        updates.onNext(host)
        observer.assertComplete().assertNoErrors()
        assertFalse(ready.hasObservers())
    }

    @Test
    fun everyTerminalStatusCancelsNonterminalNotification() {
        for (state in listOf(CallStatus.HUNGUP, CallStatus.BUSY, CallStatus.FAILURE, CallStatus.OVER)) {
            val call = call()
            val ready = CompletableSubject.create()
            val observer = processor.notification(call, false) { ready }.test()

            call.setCallState(state)
            updates.onNext(call)

            observer.assertComplete().assertNoErrors()
            assertFalse(ready.hasObservers())
        }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun alreadyTerminalMutableCallSkipsNonterminalWorkAtSubscription() {
        val call = call()
        var started = false
        val notification = processor.notification(call, false) {
            started = true
            Completable.never()
        }
        call.setCallState(CallStatus.OVER)

        notification.test().assertComplete().assertNoErrors()
        assertFalse(started)
        assertFalse(updates.hasObservers())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun terminalNotificationIsBoundedButItsHistoryIsNot() {
        val call = call().apply { setCallState(CallStatus.OVER) }
        val notification = CompletableSubject.create()
        val history = CompletableSubject.create()
        val observer = processor.notification(call, true) { notification }
            .andThen(history)
            .test()

        updates.onNext(call)
        assertTrue(notification.hasObservers())
        assertFalse(history.hasObservers())
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        assertFalse(notification.hasObservers())
        assertTrue(history.hasObservers())
        scheduler.advanceTimeBy(300, TimeUnit.SECONDS)
        observer.assertNotComplete()
        history.onComplete()
        observer.assertComplete().assertNoErrors()
        assertEquals(1, errors.size)
        assertTrue(errors.single() is TimeoutException)
    }

    @Test
    fun synchronousNotificationFailureIsLoggedAndDoesNotSkipHistory() {
        val failure = IllegalStateException("notification factory")
        var historySaved = false
        processor.notification(call(), true) { throw failure }
            .andThen(Completable.fromAction { historySaved = true })
            .test()
            .assertComplete()
            .assertNoErrors()

        assertTrue(historySaved)
        assertSame(failure, errors.single())
    }

    @Test
    fun asynchronousNotificationFailureIsLoggedAndDoesNotSkipHistory() {
        val failure = IllegalStateException("notification readiness")
        val ready = CompletableSubject.create()
        var historySaved = false
        val observer = processor.notification(call(), true) { ready }
            .andThen(Completable.fromAction { historySaved = true })
            .test()

        ready.onError(failure)

        observer.assertComplete().assertNoErrors()
        assertTrue(historySaved)
        assertSame(failure, errors.single())
    }

    @Test
    fun synchronousAndAsynchronousHandlerFailuresDoNotStopQueue() {
        val syncFailure = IllegalStateException("handler")
        val asyncFailure = IllegalStateException("history")
        val completed = mutableListOf<String?>()
        val observer = processor.process { call ->
            when (call.id) {
                "sync" -> throw syncFailure
                "async" -> Completable.error(asyncFailure)
                else -> Completable.fromAction { completed.add(call.id) }
            }
        }.test()

        updates.onNext(call("sync"))
        updates.onNext(call("async"))
        updates.onNext(call("next"))
        updates.onComplete()

        observer.assertComplete().assertNoErrors()
        assertEquals(listOf("next"), completed)
        assertEquals(listOf(syncFailure, asyncFailure), errors)
    }

    @Test
    fun disposingProcessorDisposesNotificationAndTerminalListener() {
        val ready = CompletableSubject.create()
        val observer = processor.process { call ->
            processor.notification(call, false) { ready }
        }.test()
        updates.onNext(call())

        observer.dispose()
        assertFalse(ready.hasObservers())
        assertFalse(updates.hasObservers())
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        assertTrue(errors.isEmpty())
    }
}
