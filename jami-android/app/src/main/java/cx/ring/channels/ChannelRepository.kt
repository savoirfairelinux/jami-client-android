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

class ChannelRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): List<Channel> {
        val stored = preferences.getString(KEY_CHANNELS, null)
        if (stored == null) {
            val defaults = DEFAULT_CHANNELS.map { Channel(it, emptySet(), true) }
            save(defaults)
            return defaults
        }
        val channels = JSONArray(stored).let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val builtIn = item.optBoolean("builtIn")
                val name = item.getString("name")
                Channel(
                    // Migrate names used by the first prototype to the product names.
                    if (builtIn && (name == LEGACY_ALL_CONTACTS || name == LEGACY_CONTACTS))
                        ALL_CHANNEL
                    else if (builtIn && name == LEGACY_FRIEND)
                        "Friends"
                    else if (builtIn && name == LEGACY_REAL_FRIEND)
                        "Real Friends"
                    else name,
                    item.optJSONArray("members").toStringSet(),
                    builtIn,
                    item.optJSONArray("groups").toStringSet(),
                )
            }
        }
        return channels.ifEmpty { DEFAULT_CHANNELS.map { Channel(it, emptySet(), true) } }
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
        preferences.edit().putString(KEY_CHANNELS, array.toString()).apply()
    }

    var activeChannelName: String
        get() = preferences.getString(KEY_ACTIVE, null) ?: ALL_CHANNEL
        set(value) = preferences.edit().putString(KEY_ACTIVE, value).apply()

    fun activeChannel(): Channel {
        val channels = load()
        return channels.firstOrNull { it.name == activeChannelName } ?: channels.first()
    }

    fun addGroup(channelName: String, conversationUri: String) {
        save(load().map { if (it.name == channelName) it.copy(groups = it.groups + conversationUri) else it })
    }

    companion object {
        private const val PREFERENCES = "jami_channels"
        private const val KEY_CHANNELS = "channels"
        private const val KEY_ACTIVE = "active"
        const val ALL_CHANNEL = "All"
        private const val LEGACY_ALL_CONTACTS = "Contact"
        private const val LEGACY_CONTACTS = "Contacts"
        private const val LEGACY_FRIEND = "Friend"
        private const val LEGACY_REAL_FRIEND = "Real Friend"
        val DEFAULT_CHANNELS = listOf(ALL_CHANNEL, "Friends", "Real Friends", "Family", "Colleague")

        private fun JSONArray?.toStringSet(): Set<String> =
            if (this == null) emptySet() else (0 until length()).map { getString(it) }.toSet()
    }
}
