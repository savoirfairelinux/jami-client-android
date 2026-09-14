package net.jami.model

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.UUID

/**
 * Account-private wire schema. Each field is a separate daemon LWW register; deletion markers
 * are permanent so an offline rename or membership change cannot resurrect a deleted channel.
 */
object ChannelMetadata {
    const val PREFIX = "jami.channels.v1/"
    const val ALL_ID = "all"
    const val ALL_NAME = "All"
    val defaults = listOf(
        Channel(ALL_ID, ALL_NAME, builtIn = true),
        Channel("default-friends", "Friends", builtIn = true),
        Channel("default-real-friends", "Real Friends", builtIn = true),
        Channel("default-family", "Family", builtIn = true),
        Channel("default-colleague", "Colleague", builtIn = true),
    )
    private val defaultById = defaults.associateBy { it.id }
    private val validId = Regex("[A-Za-z0-9-]{1,128}")

    fun channels(metadata: Map<String, String>): List<Channel> {
        val ids = metadata.keys.asSequence()
            .filter { it.startsWith(PREFIX) }
            .map { it.removePrefix(PREFIX).substringBefore('/') }
            .filter { validId.matches(it) }
            .toSet() + defaultById.keys
        val channels = ids.mapNotNull { id ->
            if (id == ALL_ID) return@mapNotNull defaults.first()
            if (metadata[key(id, "deleted")] == "1") return@mapNotNull null
            val name = metadata[key(id, "name")] ?: defaultById[id]?.name ?: return@mapNotNull null
            require(name.isNotBlank()) { "Invalid Channel name in account metadata" }
            Channel(
                id, name,
                members = memberships(metadata, id, "members"),
                builtIn = id in defaultById,
                groups = memberships(metadata, id, "groups"),
            )
        }
        val order = defaults.mapIndexed { index, channel -> channel.id to index }.toMap()
        return channels.sortedWith(compareBy<Channel> { order[it.id] ?: defaults.size }
            .thenBy { it.name }.thenBy { it.id })
    }

    fun create(metadata: Map<String, String>, name: String, id: String = UUID.randomUUID().toString()): Map<String, String> {
        require(validId.matches(id) && id !in defaultById) { "Invalid Channel identifier" }
        require(metadata.keys.none { it.startsWith("$PREFIX$id/") }) { "Channel identifier already exists" }
        val title = validateName(metadata, name)
        return mapOf(key(id, "name") to title)
    }

    fun rename(metadata: Map<String, String>, id: String, name: String): Map<String, String> {
        val channel = editable(metadata, id)
        val title = validateName(metadata, name, id)
        return if (title == channel.name) emptyMap() else mapOf(key(id, "name") to title)
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
        require(uri.isNotBlank()) { "Missing Channel member" }
        val field = if (group) "groups" else "members"
        val member = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
        return buildMap {
            changes.forEach { (id, selected) ->
                val channel = editable(metadata, id)
                val present = uri in if (group) channel.groups else channel.members
                if (present != selected) put(key(id, "$field/$member"), if (selected) "1" else "0")
            }
        }
    }

    fun legacyChannels(json: String): List<Channel> =
        JsonParser.parseString(json).asJsonArray.map { element ->
            val item = element.asJsonObject
            val builtIn = item["builtIn"]?.asBoolean ?: false
            val name = if (builtIn) migrateName(item["name"].asString) else item["name"].asString
            val id = if (builtIn) defaults.firstOrNull { it.name == name }?.id else null
            Channel(
                id ?: UUID.nameUUIDFromBytes(("jami-channel-legacy:$name").toByteArray(UTF_8)).toString(),
                name, item["members"]?.asJsonArray.toStringSet(), builtIn,
                item["groups"]?.asJsonArray.toStringSet()
            )
        }

    fun migrate(legacy: List<Channel>, current: Map<String, String>): Map<String, String> {
        val updates = mutableMapOf<String, String>()
        for (channel in legacy) {
            if (channel.isAllContacts || current[key(channel.id, "deleted")] == "1") continue
            if (defaultById[channel.id]?.name != channel.name) {
                updates[key(channel.id, "name")] = channel.name
            }
            for ((field, values) in listOf("members" to channel.members, "groups" to channel.groups)) {
                for (uri in values) {
                    val member = Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(UTF_8))
                    updates[key(channel.id, "$field/$member")] = "1"
                }
            }
        }
        for (channel in defaults.drop(1)) {
            if (legacy.none { it.id == channel.id } &&
                current.keys.none { it.startsWith("$PREFIX${channel.id}/") }) {
                updates[key(channel.id, "deleted")] = "1"
            }
        }
        // Migrating an old local snapshot must never overwrite an already synchronized register.
        return updates.filterKeys { it !in current }
    }

    fun migrateName(name: String?): String = when (name) {
        "Contact", "Contacts", null -> ALL_NAME
        "Friend" -> "Friends"
        "Real Friend" -> "Real Friends"
        else -> name
    }

    private fun key(id: String, field: String): String = "$PREFIX$id/$field"

    private fun editable(metadata: Map<String, String>, id: String): Channel {
        val channel = channels(metadata).firstOrNull { it.id == id }
        require(channel != null) { "Channel no longer exists" }
        require(!channel.isAllContacts) { "All cannot be modified" }
        return channel
    }

    private fun validateName(metadata: Map<String, String>, name: String, exceptId: String? = null): String {
        val title = name.trim()
        require(title.isNotEmpty()) { "Channel name cannot be empty" }
        require(channels(metadata).none { it.id != exceptId && it.name == title }) { "Channel name already exists" }
        return title
    }

    private fun memberships(metadata: Map<String, String>, id: String, field: String): Set<String> {
        val prefix = key(id, "$field/")
        return metadata.entries.asSequence().filter { it.key.startsWith(prefix) && it.value == "1" }
            .map { String(Base64.getUrlDecoder().decode(it.key.removePrefix(prefix)), UTF_8) }.toSet()
    }

    private fun JsonArray?.toStringSet(): Set<String> = this?.map { it.asString }?.toSet() ?: emptySet()
}
