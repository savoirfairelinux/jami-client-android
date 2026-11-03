package net.jami.model

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue

class ConversationPermissionsTest {
    @Test
    fun swarmMessagingRequiresLoadedGrantsAndDistinguishesReplies() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        assertFalse(conversation.canSendText(null))
        assertFalse(conversation.canSendFile("post"))
        assertThrows(SecurityException::class.java) {
            conversation.requirePermission(Conversation.Permission.Text)
        }
        conversation.setPermissions(setOf("textReply", "fileReply", "reaction"))
        assertFalse(conversation.canSendText(null))
        assertFalse(conversation.canSendText(""))
        assertFalse(conversation.canSendFile(null))
        assertTrue(conversation.canSendText("post"))
        assertTrue(conversation.canSendFile("post"))
        assertTrue(conversation.hasPermission(Conversation.Permission.Reaction))
        assertFalse(conversation.hasPermission(Conversation.Permission.Edit))
        conversation.setPermissions(setOf("text", "file"))
        assertTrue(conversation.canSendText(null))
        assertTrue(conversation.canSendFile(null))
        assertFalse(conversation.canSendText("post"))
    }

    @Test
    fun legacyMessagingDoesNotRequireSwarmPermissions() {
        val conversation = Conversation("account", Contact(Uri.fromString("jami://test")))
        Conversation.Permission.entries.forEach {
            assertTrue(conversation.hasPermission(it))
            conversation.requirePermission(it)
        }
    }

    @Test
    fun permissionsAreUnknownUntilLoadedAndReplacePreviousGrants() {
        val conversation = Conversation("account", Contact(Uri.fromString("jami://test")))
        val updates = conversation.permissions.test().assertNoValues()
        val granted = mutableSetOf("textReply", "futurePermission")
        conversation.setPermissions(granted)
        granted.add("text")

        conversation.permissions.test().assertValue(setOf("textReply", "futurePermission"))
        conversation.setPermissions(setOf("textReply", "futurePermission"))
        conversation.setPermissions(emptySet())

        updates.assertValues(setOf("textReply", "futurePermission"), emptySet())
    }
}
