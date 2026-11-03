package net.jami.model

import org.junit.Test

class ConversationPermissionsTest {
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
