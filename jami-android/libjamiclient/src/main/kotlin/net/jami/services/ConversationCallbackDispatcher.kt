package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableSource
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.CompletableSubject
import net.jami.utils.Log

internal class ConversationCallbackDispatcher(
    scheduler: Scheduler = Schedulers.computation(),
    private val onError: (Throwable) -> Unit = { Log.e(TAG, "Conversation callback failed", it) }
) : Disposable {
    private data class ConversationKey(val accountId: String, val conversationId: String)

    private data class Key(val conversation: ConversationKey, val generation: Long)

    private class Route(val generation: Long, val ready: CompletableSource) {
        var started = false
    }

    private data class Task(
        val key: Key,
        val ready: CompletableSource,
        val closeAfter: Boolean,
        val action: () -> Unit
    )

    private val routingLock = Any()
    private val routes = HashMap<ConversationKey, Route>()
    private var nextGeneration = 0L
    private val tasks: FlowableProcessor<Task> = PublishProcessor.create<Task>().toSerialized()
    private val subscription = tasks
        .onBackpressureBuffer()
        .groupBy(Task::key)
        .flatMapCompletable { group ->
            group.takeUntil(Task::closeAfter).concatMapCompletable { task ->
                Completable.wrap(task.ready)
                    .andThen(Completable.fromAction(task.action))
                    .subscribeOn(scheduler)
                    .onErrorComplete { error ->
                        onError(error)
                        true
                    }
            }
        }
        .subscribe({}, onError)

    fun dispatch(accountId: String, conversationId: String, action: () -> Unit) {
        synchronized(routingLock) {
            if (isDisposed)
                return
            val conversation = ConversationKey(accountId, conversationId)
            val route = routes.getOrPut(conversation) {
                Route(nextGeneration++, Completable.complete())
            }
            route.started = true
            tasks.onNext(Task(Key(conversation, route.generation), route.ready, false, action))
        }
    }

    fun dispatchAndClose(accountId: String, conversationId: String, action: () -> Unit) {
        synchronized(routingLock) {
            if (isDisposed)
                return
            val conversation = ConversationKey(accountId, conversationId)
            val route = routes[conversation] ?: Route(nextGeneration++, Completable.complete())
            val successorReady = CompletableSubject.create()
            val successor = Route(nextGeneration++, successorReady)
            routes[conversation] = successor
            tasks.onNext(Task(Key(conversation, route.generation), route.ready, true) {
                try {
                    action()
                } finally {
                    synchronized(routingLock) {
                        if (!successor.started && routes[conversation] === successor)
                            routes.remove(conversation)
                    }
                    successorReady.onComplete()
                }
            })
        }
    }

    override fun dispose() {
        synchronized(routingLock) {
            routes.clear()
            tasks.onComplete()
            subscription.dispose()
        }
    }

    override fun isDisposed(): Boolean = subscription.isDisposed

    private companion object {
        val TAG = ConversationCallbackDispatcher::class.simpleName!!
    }
}