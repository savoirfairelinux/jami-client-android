/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package net.jami.model

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.UUID

/**
 * Account-private wire schema for Circles (formerly Squads / Channels).
 * Retains the historical `jami.channels.v1/` namespace for compatibility.
 */
object CircleMetadata {
    const val LEGACY_PREFIX = "jami.channels.v1/"
    const val PREFIX = LEGACY_PREFIX
    const val ALL_ID = "all"
    const val FRIENDS_ID = "default-friends"
    const val REAL_FRIENDS_ID = "default-real-friends"
    const val FAMILY_ID = "default-family"
    const val COLLEAGUE_ID = "default-colleague"

    // Canonical names of the built-in Circles, stored in the account metadata and shared
    // with Jami Qt: they are wire values, not display text. Clients localize them by id.
    const val ALL_NAME = "Tous"
    private const val FRIENDS_NAME = "Amis"
    private const val REAL_FRIENDS_NAME = "Amis proches"
    private const val FAMILY_NAME = "Famille"
    private const val COLLEAGUE_NAME = "Coll\u00e8gues"

    val defaults = listOf(
        Circle(ALL_ID, ALL_NAME, builtIn = true),
        Circle(FRIENDS_ID, FRIENDS_NAME, builtIn = true),
        Circle(REAL_FRIENDS_ID, REAL_FRIENDS_NAME, builtIn = true),
        Circle(FAMILY_ID, FAMILY_NAME, builtIn = true),
        Circle(COLLEAGUE_ID, COLLEAGUE_NAME, builtIn = true),
    )
    private val defaultById = defaults.associateBy { it.id }
    private val validId = Regex("[A-Za-z0-9-]{1,128}")

    /** Whether [circle] is a built-in Circle still bearing its canonical name. */
    fun hasDefaultName(circle: Circle): Boolean = defaultById[circle.id]?.name == circle.name

    /** A rejected Circle edit; [reason] lets clients show a localized message. */
    class CircleException(val reason: Reason, message: String) : IllegalArgumentException(message)

    enum class Reason { INVALID_ID, ALREADY_EXISTS, MISSING_MEMBER, NOT_EDITABLE, NOT_FOUND, NAME_REQUIRED, NAME_TAKEN }

    private fun fail(reason: Reason, message: String): Nothing = throw CircleException(reason, message)

    fun circles(metadata: Map<String, String>): List<Circle> {
        val ids = metadata.keys.asSequence()
            .filter { it.startsWith(PREFIX) }
            .map { it.removePrefix(PREFIX).substringBefore('/') }
            .filter { validId.matches(it) }
            .toSet() + defaultById.keys
        val circles = ids.mapNotNull { id ->
            if (id == ALL_ID) return@mapNotNull defaults.first()
            if (metadata[key(id, "deleted")] == "1") return@mapNotNull null
            val rawName = metadata[key(id, "name")] ?: defaultById[id]?.name ?: return@mapNotNull null
            val name = migrateName(rawName) ?: rawName
            if (name.isBlank()) return@mapNotNull null
            Circle(
                id = id,
                name = name,
                members = memberships(metadata, id, "members"),
                builtIn = id in defaultById,
                groups = memberships(metadata, id, "groups"),
            )
        }
        val order = defaults.mapIndexed { index, circle -> circle.id to index }.toMap()
        return circles.sortedWith(
            compareBy<Circle> { order[it.id] ?: defaults.size }
                .thenBy { it.name }
                .thenBy { it.id }
        )
    }

    fun create(
        metadata: Map<String, String>,
        name: String,
        id: String = UUID.randomUUID().toString()
    ): Map<String, String> {
        if (!validId.matches(id) || id in defaultById) fail(Reason.INVALID_ID, "Invalid circle identifier")
        if (metadata.keys.any { it.startsWith("$PREFIX$id/") }) fail(Reason.ALREADY_EXISTS, "This circle already exists")
        val title = validateName(metadata, name)
        return mapOf(key(id, "name") to title)
    }

    fun rename(metadata: Map<String, String>, id: String, name: String): Map<String, String> {
        val circle = editable(metadata, id)
        val title = validateName(metadata, name, id)
        return if (title == circle.name) emptyMap() else mapOf(key(id, "name") to title)
    }

    fun delete(metadata: Map<String, String>, id: String): Map<String, String> {
        editable(metadata, id)
        return mapOf(key(id, "deleted") to "1")
    }

    fun setMemberships(
        metadata: Map<String, String>,
        uri: String,
        group: Boolean,
        changes: Map<String, Boolean>
    ): Map<String, String> {
        if (uri.isBlank()) fail(Reason.MISSING_MEMBER, "Missing circle member")
        val field = if (group) "groups" else "members"
        val member = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
        return buildMap {
            changes.forEach { (id, selected) ->
                val circle = editable(metadata, id)
                val present = uri in if (group) circle.groups else circle.members
                if (present != selected) put(key(id, "$field/$member"), if (selected) "1" else "0")
            }
        }
    }

    fun setCircleMembers(
        metadata: Map<String, String>,
        circleId: String,
        newMembers: Set<String>,
        newGroups: Set<String>
    ): Map<String, String> {
        val circle = editable(metadata, circleId)
        return buildMap {
            val allMemberUris = circle.members + newMembers
            for (uri in allMemberUris) {
                if (uri.isBlank()) continue
                val shouldBePresent = uri in newMembers
                val wasPresent = uri in circle.members
                if (shouldBePresent != wasPresent) {
                    val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
                    put(key(circleId, "members/$encoded"), if (shouldBePresent) "1" else "0")
                }
            }
            val allGroupUris = circle.groups + newGroups
            for (uri in allGroupUris) {
                if (uri.isBlank()) continue
                val shouldBePresent = uri in newGroups
                val wasPresent = uri in circle.groups
                if (shouldBePresent != wasPresent) {
                    val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
                    put(key(circleId, "groups/$encoded"), if (shouldBePresent) "1" else "0")
                }
            }
        }
    }

    fun legacyCircles(json: String): List<Circle> =
        try {
            JsonParser.parseString(json).asJsonArray.map { element ->
                val item = element.asJsonObject
                val builtIn = item["builtIn"]?.asBoolean ?: false
                val rawName = item["name"].asString
                val name = migrateName(rawName) ?: rawName
                val id = if (builtIn) defaults.firstOrNull { it.name == name }?.id else null
                Circle(
                    id = id ?: UUID.nameUUIDFromBytes(("jami-channel-legacy:$rawName").toByteArray(UTF_8)).toString(),
                    name = name,
                    members = item["members"]?.asJsonArray.toStringSet(),
                    builtIn = builtIn,
                    groups = item["groups"]?.asJsonArray.toStringSet()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

    fun migrate(legacy: List<Circle>, existing: Map<String, String>): Map<String, String> {
        val current = circles(existing).associateBy { it.id }
        return buildMap {
            legacy.filterNot { it.isAllContacts }.forEach { circle ->
                val targetId = current.values.firstOrNull { it.name == circle.name }?.id ?: circle.id
                if (existing[key(targetId, "deleted")] == "1") return@forEach
                put(key(targetId, "name"), circle.name)
                circle.members.forEach { uri ->
                    val member = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
                    put(key(targetId, "members/$member"), "1")
                }
                circle.groups.forEach { uri ->
                    val group = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
                    put(key(targetId, "groups/$group"), "1")
                }
            }
        }
    }

    fun migrateName(name: String?): String? = when (name) {
        "All" -> ALL_NAME
        "Friends", "Friend" -> FRIENDS_NAME
        "Real Friends", "Real friend" -> REAL_FRIENDS_NAME
        "Family" -> FAMILY_NAME
        "Colleague", "Colleagues" -> COLLEAGUE_NAME
        else -> name
    }

    private fun editable(metadata: Map<String, String>, id: String): Circle {
        if (id == ALL_ID) fail(Reason.NOT_EDITABLE, "The All circle cannot be modified")
        return circles(metadata).firstOrNull { it.id == id }
            ?: fail(Reason.NOT_FOUND, "This circle no longer exists")
    }

    private fun validateName(metadata: Map<String, String>, raw: String, existingId: String? = null): String {
        val name = raw.trim()
        if (name.isEmpty()) fail(Reason.NAME_REQUIRED, "The circle name is required")
        if (circles(metadata).any { it.id != existingId && it.name.equals(name, ignoreCase = true) })
            fail(Reason.NAME_TAKEN, "A circle with this name already exists")
        return name
    }

    private fun memberships(metadata: Map<String, String>, id: String, field: String): Set<String> {
        val prefix = key(id, "$field/")
        return metadata.entries.asSequence()
            .filter { (k, v) -> k.startsWith(prefix) && v == "1" }
            .mapNotNull { (k, _) -> decode(k.removePrefix(prefix)) }
            .toSet()
    }

    private fun decode(encoded: String): String? =
        try {
            String(Base64.getUrlDecoder().decode(encoded), UTF_8).takeIf { it.isNotBlank() }
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun key(id: String, suffix: String): String = "$PREFIX$id/$suffix"

    private fun JsonArray?.toStringSet(): Set<String> =
        this?.mapNotNull { element ->
            if (element.isJsonPrimitive) element.asString.takeIf { it.isNotBlank() } else null
        }?.toSet() ?: emptySet()
}
