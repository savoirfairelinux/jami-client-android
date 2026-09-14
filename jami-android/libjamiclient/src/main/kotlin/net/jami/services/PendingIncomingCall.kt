/*
 * Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.services

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.SingleSubject
import net.jami.model.Call

/**
 * Owns an incoming Telecom request until its result is bound to the native call or its wait is
 * cancelled. Successful requests remain available until answer/reject or call/connection
 * termination; disposing the notification wait alone does not discard a live SHOW_UI request.
 */
class PendingIncomingCall(
    private val call: Call,
    private val onRemoved: (PendingIncomingCall) -> Unit
) {
    private enum class State { PENDING, RESOLVED, CANCELLED, FINISHED }

    private var state = State.PENDING
    private var binding = false
    private var connection: CallService.SystemCall? = null
    private val lifetime = CompositeDisposable()
    private val subject = SingleSubject.create<CallService.SystemCall>()
    val result: Single<CallService.SystemCall> = subject.doOnDispose { cancel() }

    /** Register after inserting the request, before invoking Telecom. */
    @Synchronized
    fun trackTermination(updates: Observable<Call>): Boolean {
        lifetime.add(updates
            .filter { update ->
                update.callStatus.isOver && (update === call ||
                    (call.id != null && update.id == call.id && update.account == call.account))
            }
            .take(1)
            .subscribe({ terminate() }, { fail(it) }))
        if (call.callStatus.isOver) terminate()
        return state != State.CANCELLED
    }

    @Synchronized
    fun cancel() {
        if (state == State.PENDING) terminate()
    }

    @Synchronized
    private fun terminate() {
        if (state == State.CANCELLED || state == State.FINISHED) return
        state = State.CANCELLED
        lifetime.dispose()
        onRemoved(this)
        try {
            // A reentrant terminal event during setCall must not dispose a half-bound connection.
            if (!binding) releaseConnection()
        } finally {
            subject.onSuccess(CallService.CALL_DISALLOWED_VAL)
        }
    }

    @Synchronized
    private fun fail(error: Throwable) {
        if (state == State.CANCELLED || state == State.FINISHED) return
        state = State.CANCELLED
        lifetime.dispose()
        onRemoved(this)
        try {
            if (!binding) releaseConnection()
        } finally {
            subject.onError(error)
        }
    }

    private fun releaseConnection() {
        val owned = connection
        connection = null
        owned?.setCall(null)
    }

    private fun finish() {
        state = State.FINISHED
        lifetime.dispose()
        onRemoved(this)
        // Answer/reject routing is finished; an accepted connection belongs to the ongoing call.
        connection = null
    }

    /** False means that the caller must dispose the unclaimed Telecom connection. */
    @Synchronized
    fun complete(result: CallService.SystemCall, finalResult: Boolean = false): Boolean {
        if (state == State.CANCELLED || state == State.FINISHED) return false
        if (call.callStatus.isOver) {
            terminate()
            return false
        }
        if (state == State.RESOLVED) {
            if (finalResult) finish()
            return true
        }
        connection = result.takeIf { it.allowed }
        binding = true
        try {
            if (result.allowed) result.setCall(call)
        } catch (error: Exception) {
            binding = false
            fail(error)
            releaseConnection()
            return false
        }
        binding = false
        if (state == State.CANCELLED || call.callStatus.isOver) {
            terminate()
            releaseConnection()
            return false
        }
        state = State.RESOLVED
        lifetime.add(result.termination.subscribe({ terminate() }, { fail(it) }))
        if (state != State.RESOLVED) return false
        subject.onSuccess(result)
        if (state == State.CANCELLED) return false
        if (finalResult || !result.allowed) finish()
        return true
    }
}
