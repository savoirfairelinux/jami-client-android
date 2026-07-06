package net.jami.services

import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ConversationCallbackDispatcherTest {
    private val executor = Executors.newFixedThreadPool(2)
    private lateinit var dispatcher: ConversationCallbackDispatcher

    @Before
    fun setUp() {
        dispatcher = ConversationCallbackDispatcher(Schedulers.from(executor))
    }

    @After
    fun tearDown() {
        dispatcher.dispose()
        executor.shutdownNow()
    }

    @Test
    fun callbacksForOneConversationRunInOrder() {
        val releaseFirst = CountDownLatch(1)
        val completed = CountDownLatch(3)
        val calls = Collections.synchronizedList(mutableListOf<Int>())

        dispatcher.dispatch("account", "conversation") {
            releaseFirst.await(5, TimeUnit.SECONDS)
            calls.add(1)
            completed.countDown()
        }
        dispatcher.dispatch("account", "conversation") {
            calls.add(2)
            completed.countDown()
        }
        dispatcher.dispatch("account", "conversation") {
            calls.add(3)
            completed.countDown()
        }

        releaseFirst.countDown()

        assertTrue(completed.await(5, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), calls)
    }

    @Test
    fun blockedConversationDoesNotBlockAnotherConversation() {
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondCompleted = CountDownLatch(1)

        dispatcher.dispatch("account", "blocked") {
            firstStarted.countDown()
            releaseFirst.await(5, TimeUnit.SECONDS)
        }
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

        dispatcher.dispatch("account", "ready") {
            secondCompleted.countDown()
        }

        assertTrue(secondCompleted.await(5, TimeUnit.SECONDS))
        releaseFirst.countDown()
    }

    @Test
    fun callbackFailureDoesNotStopConversationQueue() {
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())
        dispatcher.dispose()
        dispatcher = ConversationCallbackDispatcher(Schedulers.from(executor), errors::add)
        val completed = CountDownLatch(1)

        dispatcher.dispatch("account", "conversation") {
            throw IllegalStateException("expected")
        }
        dispatcher.dispatch("account", "conversation") {
            completed.countDown()
        }

        assertTrue(completed.await(5, TimeUnit.SECONDS))
        assertEquals(1, errors.size)
    }

    @Test
    fun closedConversationKeyCanBeRecreated() {
        val closed = CountDownLatch(1)
        val recreated = CountDownLatch(1)

        dispatcher.dispatchAndClose("account", "conversation") {
            closed.countDown()
        }
        assertTrue(closed.await(5, TimeUnit.SECONDS))

        dispatcher.dispatch("account", "conversation") {
            recreated.countDown()
        }

        assertTrue(recreated.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun recreatedConversationWaitsForCloseToFinish() {
        val closeStarted = CountDownLatch(1)
        val releaseClose = CountDownLatch(1)
        val recreated = CountDownLatch(1)

        dispatcher.dispatchAndClose("account", "conversation") {
            closeStarted.countDown()
            releaseClose.await(5, TimeUnit.SECONDS)
        }
        assertTrue(closeStarted.await(5, TimeUnit.SECONDS))

        dispatcher.dispatch("account", "conversation") {
            recreated.countDown()
        }

        assertFalse(recreated.await(200, TimeUnit.MILLISECONDS))
        releaseClose.countDown()
        assertTrue(recreated.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun concurrentCloseAndDispatchDoNotDropCallbacks() {
        val iterations = 100
        val producersReady = CountDownLatch(2)
        val start = CountDownLatch(1)
        val completed = CountDownLatch(iterations * 2)

        val callbacks = thread {
            producersReady.countDown()
            start.await()
            repeat(iterations) {
                dispatcher.dispatch("account", "conversation") {
                    completed.countDown()
                }
            }
        }
        val closes = thread {
            producersReady.countDown()
            start.await()
            repeat(iterations) {
                dispatcher.dispatchAndClose("account", "conversation") {
                    completed.countDown()
                }
            }
        }

        assertTrue(producersReady.await(5, TimeUnit.SECONDS))
        start.countDown()
        callbacks.join()
        closes.join()
        assertTrue(completed.await(5, TimeUnit.SECONDS))
    }
}