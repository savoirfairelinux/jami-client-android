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
import io.reactivex.rxjava3.subjects.SingleSubject
import net.jami.model.Call

/**
 * Owns an incoming Telecom request until its result is bound to the native call or its wait is
 * cancelled. Successful requests remain available to route later accept/reject callbacks.
 */
class PendingIncomingCall(
    private val call: Call,
    private val onCancelled: (PendingIncomingCall) -> Unit
) {
    private enum class State { PENDING, RESOLVED, CANCELLED }

    private var state = State.PENDING
    private val subject = SingleSubject.create<CallService.SystemCall>()
    val result: Single<CallService.SystemCall> = subject.doOnDispose { cancel() }

    @Synchronized
    fun cancel() {
        if (state == State.PENDING) {
            state = State.CANCELLED
            onCancelled(this)
        }
    }

    /** False means that the caller must dispose the unclaimed Telecom connection. */
    fun complete(result: CallService.SystemCall): Boolean {
        synchronized(this) {
            if (state == State.CANCELLED) return false
            if (state == State.RESOLVED) return true
            if (call.callStatus.isOver) {
                cancel()
                return false
            }
            // Bind before publishing, even when the callback arrives before the first subscriber.
            // Disposal after ownership is transferred must not remove a live SHOW_UI request.
            state = State.RESOLVED
            try {
                if (result.allowed) result.setCall(call)
            } catch (error: Exception) {
                state = State.CANCELLED
                onCancelled(this)
                subject.onError(error)
                return false
            }
            if (call.callStatus.isOver) {
                state = State.CANCELLED
                onCancelled(this)
                return false
            }
        }
        subject.onSuccess(result)
        return true
    }
}
