/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.channels

import android.content.Context
import net.jami.model.Conversation
import org.json.JSONArray
import org.json.JSONObject

data class Channel(
    val name: String,
    val members: Set<String>,
    val builtIn: Boolean = false,
    /** Conversation URIs of the groups created from this channel. */
    val groups: Set<String> = emptySet(),
) {
    /** The built-in "All" channel implicitly contains every contact. */
    val isAllContacts get() = builtIn && name == ChannelRepository.ALL_CHANNEL

    fun containsContact(uri: String) = isAllContacts || uri in members

    /**
     * A group belongs to the channels it was put in, and to no other; a one-to-one conversation
     * belongs where its contact does. "All" holds every conversation.
     */
    fun contains(conversation: Conversation): Boolean {
        if (isAllContacts) return true
        if (conversation.isSwarmGroup()) return conversation.uri.rawUriString in groups
        val others = conversation.contacts.filter { !it.isUser }
        if (others.isEmpty()) return false
        return others.all { it.uri.rawUriString in members }
    }
}

class ChannelRepository(context: Context, accountId: String? = null) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val accountKey = accountId?.takeIf { it.isNotBlank() }
    private val keySuffix = accountKey ?: DEFAULT_ACCOUNT
    private val channelsKey = "$KEY_CHANNELS_PREFIX$keySuffix"
    private val activeKey = "$KEY_ACTIVE_PREFIX$keySuffix"
    private val legacyMigratedKey = "$LEGACY_MIGRATED_PREFIX$keySuffix"

    fun load(): List<Channel> {
        val accountStored = preferences.getString(channelsKey, null)
        // Legacy data is migrated only after a real account is known. A temporary repository
        // created while accounts are loading must never claim the legacy data for "default".
        val migrateLegacy = accountKey != null &&
            !preferences.getBoolean(legacyMigratedKey, false)
        val legacyStored = if (migrateLegacy)
            preferences.getString(LEGACY_CHANNELS_KEY, null)
        else null
        val stored = accountStored ?: legacyStored
        if (stored == null) {
            val defaults = DEFAULT_CHANNELS.map { Channel(it, emptySet(), true) }
            save(defaults)
            return defaults
        }
        val parsed = JSONArray(stored).let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val builtIn = item.optBoolean("builtIn")
                val name = item.getString("name")
                Channel(
                    // Migrate names used by the first prototype to the product names.
                    if (builtIn) migrateName(name) else name,
                    item.optJSONArray("members").toStringSet(),
                    builtIn,
                    item.optJSONArray("groups").toStringSet(),
                )
            }
        }
        val channels = parsed.ifEmpty { DEFAULT_CHANNELS.map { Channel(it, emptySet(), true) } }
        if (accountStored == null || channels != parsed)
            save(channels)
        if (legacyStored != null) {
            val migration = preferences.edit()
                .putBoolean(legacyMigratedKey, true)
                .remove(LEGACY_CHANNELS_KEY)
                .remove(LEGACY_ACTIVE_KEY)
            if (accountStored == null)
                migration.putString(
                    activeKey,
                    migrateName(preferences.getString(LEGACY_ACTIVE_KEY, null))
                )
            migration.apply()
        }
        return channels
    }

    fun save(channels: List<Channel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().apply {
                put("name", channel.name)
                put("builtIn", channel.builtIn)
                put("members", JSONArray(channel.members.toList()))
                put("groups", JSONArray(channel.groups.toList()))
            })
        }
        preferences.edit().putString(channelsKey, array.toString()).apply()
    }

    var activeChannelName: String
        get() {
            val raw = preferences.getString(activeKey, null)
                ?: if (accountKey != null && !preferences.getBoolean(legacyMigratedKey, false))
                    preferences.getString(LEGACY_ACTIVE_KEY, null)
                else null
            val channels = load()
            val active = migrateName(raw)
                .takeIf { name -> channels.any { it.name == name } }
                ?: channels.firstOrNull()?.name
                ?: ALL_CHANNEL
            if (preferences.getString(activeKey, null) != active)
                preferences.edit().putString(activeKey, active).apply()
            return active
        }
        set(value) = preferences.edit().putString(activeKey, value).apply()

    fun activeChannel(): Channel {
        val channels = load()
        return channels.firstOrNull { it.name == activeChannelName } ?: channels.first()
    }

    fun addGroup(channelName: String, conversationUri: String) {
        save(load().map { if (it.name == channelName) it.copy(groups = it.groups + conversationUri) else it })
    }

    companion object {
        private const val PREFERENCES = "jami_channels"
        private const val KEY_CHANNELS_PREFIX = "channels_"
        private const val KEY_ACTIVE_PREFIX = "active_"
        private const val LEGACY_CHANNELS_KEY = "channels"
        private const val LEGACY_ACTIVE_KEY = "active"
        private const val LEGACY_MIGRATED_PREFIX = "legacy_migrated_"
        private const val DEFAULT_ACCOUNT = "default"
        const val ALL_CHANNEL = "All"
        private const val LEGACY_ALL_CONTACTS = "Contact"
        private const val LEGACY_CONTACTS = "Contacts"
        private const val LEGACY_FRIEND = "Friend"
        private const val LEGACY_REAL_FRIEND = "Real Friend"
        val DEFAULT_CHANNELS = listOf(ALL_CHANNEL, "Friends", "Real Friends", "Family", "Colleague")

        private fun migrateName(name: String?): String =
            when (name) {
                LEGACY_ALL_CONTACTS, LEGACY_CONTACTS -> ALL_CHANNEL
                LEGACY_FRIEND -> "Friends"
                LEGACY_REAL_FRIEND -> "Real Friends"
                null -> ALL_CHANNEL
                else -> name
            }

        private fun JSONArray?.toStringSet(): Set<String> =
            if (this == null) emptySet() else (0 until length()).map { getString(it) }.toSet()
    }
}
