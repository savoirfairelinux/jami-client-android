/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
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
package cx.ring.fragments

class ConversationThreadNavigation {
    class Request internal constructor(val loadedIds: Set<String?>)

    var target: String? = null
        private set
    var failed = false
        private set
    private var pending: Request? = null

    fun start(messageId: String) {
        target = messageId
        failed = false
    }

    fun cancel() {
        target = null
        failed = false
    }

    fun reset() {
        cancel()
        pending = null
    }

    fun begin(loadedIds: Set<String?>): Request? {
        if (target == null || pending != null) return null
        return Request(loadedIds).also { pending = it }
    }

    fun complete(request: Request, loadedIds: Set<String?>, resolved: Boolean): Boolean {
        if (pending !== request) return false
        pending = null
        if (target == null) return false
        if (!resolved && loadedIds.all { it in request.loadedIds }) {
            target = null
            failed = true
            return false
        }
        return true
    }
}
