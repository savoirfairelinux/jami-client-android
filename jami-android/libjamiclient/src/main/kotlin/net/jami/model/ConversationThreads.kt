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
package net.jami.model

import net.jami.model.interaction.Interaction
import net.jami.model.interaction.Interaction.InteractionType

/** A display-only projection of one conversation's already ordered history. */
class ConversationThreads(history: List<Interaction>) {
    private val byId = history.mapNotNull { message ->
        message.messageId?.let { it to message }
    }.toMap()
    private val roots = HashMap<String, String?>()
    private val threads: Map<String, List<Interaction>>
    val timeline: List<Interaction>

    init {
        byId.keys.forEach { resolveRoot(it) }
        val seen = HashSet<String>()
        val messages = history.filter { message ->
            message.messageId?.let { seen.add(it) } ?: true
        }.map { message ->
            message.messageId?.let { byId.getValue(it) } ?: message
        }
        timeline = messages.filter { message ->
            val root = message.messageId?.let { roots[it] }
            root == null || root == message.messageId
        }
        threads = messages.filter { roots[it.messageId] != null }
            .groupBy { roots.getValue(it.messageId!!)!! }
            .mapValues { (root, replies) ->
                listOf(byId.getValue(root)) + replies.filter { it.messageId != root }
            }
    }

    fun rootId(messageId: String): String? = roots[messageId]

    fun messages(rootId: String): List<Interaction> = threads[rootId].orEmpty()

    fun replyCount(rootId: String): Int = (messages(rootId).size - 1).coerceAtLeast(0)

    private fun resolveRoot(id: String) {
        var current = id
        var root: String? = null
        val path = ArrayList<String>()
        val visited = HashSet<String>()
        while (visited.add(current)) {
            if (roots.containsKey(current)) {
                root = roots[current]
                break
            }
            val message = byId[current] ?: break
            if (message.type == InteractionType.INVALID
                || !message.edit.isNullOrEmpty() || !message.reactToId.isNullOrEmpty()
            ) break
            path.add(current)
            val replyTo = message.replyToId?.takeIf { it.isNotEmpty() }
            if (replyTo == null) {
                root = current
                break
            }
            current = replyTo
        }
        // An unresolved or cyclic chain stays in the timeline until its root is available.
        path.forEach { roots[it] = root }
        if (path.isEmpty()) roots[id] = root
    }
}
