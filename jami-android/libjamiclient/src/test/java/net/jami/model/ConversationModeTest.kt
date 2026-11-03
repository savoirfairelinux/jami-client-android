package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationModeTest {
    @Test
    fun daemonModesDoNotDependOnLocalEnumOrdinals() {
        val modes = mapOf(
            0 to Conversation.Mode.OneToOne,
            1 to Conversation.Mode.AdminInvitesOnly,
            2 to Conversation.Mode.InvitesOnly,
            3 to Conversation.Mode.Public,
            5 to Conversation.Mode.Feed
        )
        modes.forEach { (value, expected) ->
            assertEquals(expected, Conversation.Mode.fromDaemon(value))
        }
    }

    @Test
    fun unsupportedDaemonModesAreNotLocalConversationStates() {
        for (value in listOf(-1, 4, 6, 99)) {
            assertThrows(IllegalArgumentException::class.java) {
                Conversation.Mode.fromDaemon(value)
            }
        }
    }

    @Test
    fun feedIsASwarmModeWithoutChangingLocalStates() {
        assertTrue(Conversation.Mode.Feed.isSwarm)
        assertTrue(Conversation.Mode.Feed.isGroup)
        val feed = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        assertTrue(feed.isSwarmGroup())
        for (mode in listOf(Conversation.Mode.Syncing, Conversation.Mode.Legacy, Conversation.Mode.Request)) {
            assertFalse(mode.isSwarm)
            assertFalse(mode.isGroup)
        }
    }
}
