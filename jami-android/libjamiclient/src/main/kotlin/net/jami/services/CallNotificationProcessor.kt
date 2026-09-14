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

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Call
import net.jami.utils.Log
import java.util.concurrent.TimeUnit

internal class CallNotificationProcessor(
    private val callsUpdates: Observable<Call>,
    private val scheduler: Scheduler = Schedulers.computation(),
    private val timeoutSeconds: Long = 30,
    private val onError: (Call, Throwable) -> Unit = { call, error ->
        Log.e(TAG, "Call notification/state processing failed for ${call.account}/${call.id ?: call.confId}", error)
    }
) {
    fun process(handler: (Call) -> Completable): Completable =
        callsUpdates.concatMapCompletable { call ->
            Completable.defer { handler(call) }
                .onErrorComplete { error ->
                    onError(call, error)
                    true
                }
        }

    fun prepareRemoval(call: Call, action: () -> Completable): Completable {
        // Capture pre-removal metadata now, but let notification() handle construction failures
        // after the caller has attached history writes and completed its state cleanup.
        val prepared = try {
            action()
        } catch (error: Exception) {
            Completable.error(error)
        }
        return notification(call, true) { prepared }
    }

    fun notification(call: Call, remove: Boolean, action: () -> Completable): Completable {
        // Bound only notification readiness (profile/Telecom), never history writes or the call's
        // lifetime. Allow cold startup 30s, then dispose the wait without bypassing Telecom.
        val work = Completable.defer {
            if (!remove && call.callStatus.isOver) Completable.complete() else action()
        }.timeout(timeoutSeconds, TimeUnit.SECONDS, scheduler)

        // Listen before subscribing to work, directly on the source: the terminal handler may
        // itself be queued behind this wait. Null-id virtual host calls are identified by instance.
        val cancellable = if (remove) work else work.takeUntil(
            callsUpdates
                .filter { update ->
                    update.callStatus.isOver && (update === call ||
                        (call.id != null && update.id == call.id && update.account == call.account))
                }
                .firstElement()
                .ignoreElement()
        )
        return cancellable.onErrorComplete { error ->
            onError(call, error)
            true
        }
    }

    private companion object {
        const val TAG = "CallNotificationProcessor"
    }
}
