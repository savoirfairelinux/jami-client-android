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

import com.google.gson.JsonArray
import com.google.gson.JsonParser

object ContactLabels {
    const val METADATA_KEY = "labels"

    fun normalize(labels: Collection<String>): Set<String> =
        labels.map { it.trim() }.filter { it.isNotEmpty() }.toSortedSet().also {
            require(it.none { label -> '\u0000' in label }) { "Labels cannot contain NUL" }
        }

    fun encode(labels: Collection<String>): ByteArray =
        JsonArray().apply { normalize(labels).forEach { add(it) } }.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= 65536) { "Contact labels exceed the metadata value limit" }
        }

    fun decode(value: ByteArray?): Set<String> {
        if (value == null || value.isEmpty()) return emptySet()
        val json = JsonParser.parseString(String(value, Charsets.UTF_8))
        require(json.isJsonArray) { "Contact labels must be a JSON array" }
        return normalize(json.asJsonArray.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "Contact labels must be strings" }
            it.asString
        })
    }
}
