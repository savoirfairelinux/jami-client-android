/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.service

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks foreground services that represent ongoing background work (calls, file transfers,
 * peer/hosted service tunnels, location sharing). Each such service reports its lifecycle so the
 * application can keep accounts active while the count is greater than zero.
 */
object ActiveServiceMonitor {
    private val count = AtomicInteger(0)
    private val countSubject = BehaviorSubject.createDefault(0)

    val activeServiceCount: Observable<Int> = countSubject.distinctUntilChanged()

    fun hasActiveServices(): Boolean = count.get() > 0

    fun onServiceStarted() {
        countSubject.onNext(count.incrementAndGet())
    }

    fun onServiceStopped() {
        countSubject.onNext(count.updateAndGet { if (it > 0) it - 1 else 0 })
    }
}
