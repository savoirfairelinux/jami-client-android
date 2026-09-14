/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.jami.services

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import net.jami.model.Call
import java.util.UUID

/** One live Telecom request per native call; callbacks still identify individual attempts. */
class IncomingCallRequests(private val callsUpdates: Observable<Call>) {
    private data class CallKey(val account: String, val id: String)
    private data class Entry(val generation: String, val pending: PendingIncomingCall)

    private val lock = Any()
    private val byCall = mutableMapOf<CallKey, Entry>()
    private val byGeneration = mutableMapOf<String, Entry>()

    fun request(call: Call, start: (generation: String) -> Unit): Single<CallService.SystemCall> {
        val key = CallKey(call.account, requireNotNull(call.id) { "Incoming call has no native ID" })
        val (entry, created) = synchronized(lock) {
            byCall[key]?.let { it to false } ?: run {
                if (call.callStatus.isOver) return CallService.CALL_DISALLOWED
                // Answer may have removed the request before the daemon emits CURRENT.
                call.resolvedSystemConnection?.let { return Single.just(it) }
                val generation = UUID.randomUUID().toString()
                val pending = PendingIncomingCall(call) { removed ->
                    synchronized(lock) {
                        byGeneration.remove(generation)
                        if (byCall[key]?.pending === removed) byCall.remove(key)
                    }
                }
                val entry = Entry(generation, pending)
                byCall[key] = entry
                byGeneration[generation] = entry
                entry to true
            }
        }
        // Do not call Telecom or acquire a request's monitor while holding the registry lock.
        if (created && entry.pending.trackTermination(callsUpdates)) {
            try {
                start(entry.generation)
            } catch (error: Exception) {
                entry.pending.cancel()
                throw error
            }
        }
        return entry.pending.result
    }

    operator fun get(generation: String): PendingIncomingCall? =
        synchronized(lock) { byGeneration[generation]?.pending }
}
