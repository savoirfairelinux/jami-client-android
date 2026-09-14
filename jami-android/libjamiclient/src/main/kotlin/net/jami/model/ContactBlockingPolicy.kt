package net.jami.model

/** Contact moderation must never target the identity authorizing this account's devices. */
object ContactBlockingPolicy {
    fun canBlock(ownIdentity: String?, target: Uri, isJami: Boolean): Boolean {
        if (target.isEmpty || target.isSwarm) return false
        if (!isJami) return true
        if (ownIdentity.isNullOrBlank()) return false
        val own = Uri.fromString(ownIdentity).rawRingId
        return own.isNotBlank() && !own.equals(target.rawRingId, ignoreCase = true)
    }

    fun target(conversation: Conversation, ownIdentity: String?, isJami: Boolean): Contact? {
        val requestedPeer = conversation.request?.from
        if (conversation.isSwarmGroup() && requestedPeer == null) return null
        return conversation.contacts.filter { contact ->
            !contact.isUser && canBlock(ownIdentity, contact.uri, isJami) &&
                (requestedPeer == null || contact.uri == requestedPeer)
        }.singleOrNull()
    }
}
