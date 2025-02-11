package net.jami.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class Event(val name: String, val data: Map<String, String>?)

interface EventService {
    fun logEvent(name: String, data: Map<String, String>? = null)
    fun subscribeToEvents(on: CoroutineScope, callback: suspend (Event) -> Unit): Job
}

class EventServiceImpl : EventService {
    private val eventBuffer = MutableSharedFlow<Event>(100, 100, BufferOverflow.DROP_OLDEST)
    override fun logEvent(name: String, data: Map<String, String>?) {
        eventBuffer.tryEmit(Event(name, data))
    }

    override fun subscribeToEvents(on: CoroutineScope, callback: suspend (Event) -> Unit): Job {
        return eventBuffer
            .onEach { callback(it) }
            .launchIn(on)
    }
}