package net.jami.services

import net.jami.daemon.SwarmMessage

internal data class SwarmMessageData(
    val id: String,
    val type: String,
    val linearizedParent: String,
    val body: Map<String, String>,
    val reactions: List<Map<String, String>>,
    val editions: List<Map<String, String>>,
    val status: Map<String, Int>,
    val pluginData: Map<String, String>
) {
    companion object {
        fun from(message: SwarmMessage) = SwarmMessageData(
            id = message.id,
            type = message.type,
            linearizedParent = message.linearizedParent,
            body = message.body.toNative(),
            reactions = message.reactions.toNative(),
            editions = message.editions.toNative(),
            status = HashMap(message.status),
            pluginData = message.pluginData.toNative()
        )
    }
}