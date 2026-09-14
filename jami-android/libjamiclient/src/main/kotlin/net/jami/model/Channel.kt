package net.jami.model

data class Channel(
    val id: String,
    val name: String,
    val members: Set<String> = emptySet(),
    val builtIn: Boolean = false,
    val groups: Set<String> = emptySet(),
) {
    val isAllContacts: Boolean get() = id == ChannelMetadata.ALL_ID

    fun containsContact(uri: String): Boolean = isAllContacts || uri in members

    fun contains(conversation: Conversation): Boolean {
        if (isAllContacts) return true
        if (conversation.isSwarmGroup()) return conversation.uri.rawUriString in groups
        val others = conversation.contacts.filter { !it.isUser }
        return others.isNotEmpty() && others.all { it.uri.rawUriString in members }
    }
}
