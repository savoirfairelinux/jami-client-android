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

class ConversationThreadState(enabled: Boolean = false, rootId: String? = null) {
    var enabled = enabled
        private set
    var rootId = rootId.takeIf { enabled }
        private set
    private var scrollToLatest = false

    val draftKey: String
        get() = rootId?.let { "$MAIN_DRAFT_KEY:thread:$it" } ?: MAIN_DRAFT_KEY
    val replyDraftKey: String
        get() = "$draftKey:reply"

    fun enable(enabled: Boolean) {
        this.enabled = enabled
        scrollToLatest = false
        if (!enabled) closeThread()
    }

    fun openThread(rootId: String) {
        check(enabled)
        require(rootId.isNotEmpty())
        this.rootId = rootId
        scrollToLatest = true
    }

    fun closeThread() {
        rootId = null
        scrollToLatest = false
    }

    fun replyTarget(quotedId: String?): String? = quotedId ?: rootId

    fun consumeScrollToLatest(explicitTarget: String?): Boolean {
        val scroll = scrollToLatest && explicitTarget == null
        scrollToLatest = false
        return scroll
    }

    companion object {
        const val MAIN_DRAFT_KEY = "pendingMessage"
    }
}
