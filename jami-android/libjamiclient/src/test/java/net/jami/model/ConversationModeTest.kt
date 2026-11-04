package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationModeTest {
    @Test
    fun feedIdentityIsRetainedForPendingAndSyncingConversations() {
        val uri = Uri(Uri.SWARM_SCHEME, "feed")
        val syncing = Conversation("account", uri, Conversation.Mode.Syncing)
        syncing.requestMode = Conversation.Mode.Feed
        assertEquals(Conversation.Mode.Feed, syncing.effectiveMode)
        assertTrue(syncing.effectiveMode.isGroup)

        val pending = Conversation("account", uri, Conversation.Mode.Request)
        pending.request = TrustRequest("account", Uri.fromId("sender"), 1L, uri, mode = Conversation.Mode.Feed)
        assertEquals(Conversation.Mode.Feed, pending.effectiveMode)
        assertTrue(pending.effectiveMode.isGroup)
    }

    @Test
    fun ordinaryConversationModesKeepTheirUiIdentity() {
        for (mode in listOf(Conversation.Mode.OneToOne, Conversation.Mode.InvitesOnly, Conversation.Mode.Legacy)) {
            val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), mode)
            assertEquals(mode, conversation.effectiveMode)
        }
    }

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
    fun nullableDecoderRejectsUnsupportedDaemonModes() {
        for (value in listOf(-1, 4, 6, 99)) {
            assertNull(Conversation.Mode.fromDaemonOrNull(value))
        }
    }

    @Test
    fun malformedModeMetadataIsNotDecoded() {
        for (value in listOf(null, "", "bad", "2147483648")) {
            assertNull(value?.toIntOrNull()?.let { Conversation.Mode.fromDaemonOrNull(it) })
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
