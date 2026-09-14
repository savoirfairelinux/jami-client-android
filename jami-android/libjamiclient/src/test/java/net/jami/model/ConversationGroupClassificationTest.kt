package net.jami.model

import net.jami.smartlist.ConversationItemViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationGroupClassificationTest {
    private fun conversation(mode: Conversation.Mode, hint: Conversation.Mode? = null) =
        Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), mode).apply {
            requestMode = hint
        }

    @Test
    fun syncingDirectConversationIsNotAGroup() {
        val conversation = conversation(Conversation.Mode.Syncing, Conversation.Mode.OneToOne)
        assertFalse(conversation.isSwarmGroup())
        assertFalse(viewModel(conversation).isGroup())
    }

    @Test
    fun unknownSyncingConversationIsNotAssumedToBeAGroup() {
        val conversation = conversation(Conversation.Mode.Syncing)
        assertFalse(conversation.isSwarmGroup())
        assertFalse(viewModel(conversation).isGroup())
    }

    @Test
    fun knownGroupRemainsAGroupWhileSyncingWithoutParticipants() {
        for (mode in listOf(Conversation.Mode.AdminInvitesOnly, Conversation.Mode.InvitesOnly, Conversation.Mode.Public)) {
            val conversation = conversation(Conversation.Mode.Syncing, mode)
            assertTrue(conversation.contacts.isEmpty())
            assertTrue(conversation.isSwarmGroup())
            assertTrue(viewModel(conversation).isGroup())
        }
    }

    @Test
    fun actualGroupDoesNotDependOnParticipantCount() {
        val conversation = conversation(Conversation.Mode.InvitesOnly)
        assertTrue(conversation.isSwarmGroup())
        conversation.addContact(Contact(Uri.fromString("jami:alice")))
        assertTrue(conversation.isSwarmGroup())
        conversation.addContact(Contact(Uri.fromString("jami:bob")))
        assertTrue(conversation.isSwarmGroup())
    }

    @Test
    fun pendingRequestsUseTheirDeclaredMode() {
        val conversation = conversation(Conversation.Mode.Request)
        assertFalse(conversation.isSwarmGroup())
        conversation.requestMode = Conversation.Mode.OneToOne
        assertFalse(conversation.isSwarmGroup())
        conversation.requestMode = Conversation.Mode.InvitesOnly
        assertTrue(conversation.isSwarmGroup())
        conversation.request = TrustRequest(
            "account", Uri.fromString("jami:alice"), 0, conversation.uri,
            mode = Conversation.Mode.OneToOne
        )
        assertFalse(conversation.isSwarmGroup())
    }

    @Test
    fun loadedModeOverridesStaleImportHint() {
        val conversation = conversation(Conversation.Mode.Syncing, Conversation.Mode.InvitesOnly)
        assertTrue(conversation.isSwarmGroup())
        conversation.setMode(Conversation.Mode.OneToOne)
        assertFalse(conversation.isSwarmGroup())
    }

    @Test
    fun legacyContactsAreNotGroups() {
        val conversation = Conversation("account", Contact(Uri.fromString("jami:alice")))
        assertFalse(conversation.isSwarmGroup())
        assertFalse(viewModel(conversation).isGroup())
    }

    @Test
    fun modeChangesUpdateClassificationWithoutReplacingTheConversation() {
        val conversation = conversation(Conversation.Mode.Syncing)
        val changes = conversation.mode.map { conversation.isSwarmGroup() }.distinctUntilChanged().test()
        val syncing = viewModel(conversation)
        conversation.setMode(Conversation.Mode.InvitesOnly)
        val group = viewModel(conversation)
        conversation.setMode(Conversation.Mode.OneToOne)
        changes.assertValues(false, true, false)
        changes.dispose()
        assertNotEquals(syncing, group)
        assertNotEquals(group, viewModel(conversation))
    }

    @Test
    fun channelMembershipUsesContactWhileDirectConversationIsSyncing() {
        val conversation = conversation(Conversation.Mode.Syncing, Conversation.Mode.OneToOne)
        val contact = Contact(Uri.fromString("jami:alice"))
        conversation.addContact(contact)
        val channel = Channel("friends", "Friends", members = setOf(contact.uri.rawUriString))
        assertTrue(channel.contains(conversation))
        conversation.setMode(Conversation.Mode.OneToOne)
        assertTrue(channel.contains(conversation))
        assertEquals(1, channel.members.size)
    }

    private fun viewModel(conversation: Conversation) =
        ConversationItemViewModel(conversation, Profile.EMPTY_PROFILE, emptyList(), false)
}
