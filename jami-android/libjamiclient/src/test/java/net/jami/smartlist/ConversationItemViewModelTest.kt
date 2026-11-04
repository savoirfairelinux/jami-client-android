package net.jami.smartlist

import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.model.TrustRequest
import net.jami.model.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationItemViewModelTest {
    private val contacts = emptyList<ContactViewModel>()
    private val profile = Profile("Conversation", null)

    private fun viewModel(conversation: Conversation) =
        ConversationItemViewModel(conversation, profile, contacts, false)

    @Test
    fun feedsAreDistinguishedFromNormalGroups() {
        val feed = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        val group = Conversation("account", Uri(Uri.SWARM_SCHEME, "group"), Conversation.Mode.InvitesOnly)
        assertTrue(viewModel(feed).isFeed())
        assertFalse(viewModel(group).isFeed())
    }

    @Test
    fun modeChangesEmitEvenWhenTitleAndContactsStayTheSame() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Syncing)
        val previous = viewModel(conversation)
        conversation.setMode(Conversation.Mode.Feed)
        assertNotEquals(previous, viewModel(conversation))
    }

    @Test
    fun pendingFeedInvitationsHaveTheFeedIndicator() {
        val uri = Uri(Uri.SWARM_SCHEME, "feed")
        val conversation = Conversation("account", uri, Conversation.Mode.Request)
        conversation.request = TrustRequest("account", Uri.fromId("sender"), 1L, uri, mode = Conversation.Mode.Feed)
        assertTrue(viewModel(conversation).isFeed())
    }

    @Test
    fun syncingFeedMetadataUpdatesTheIndicatorAndGroupActions() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Syncing)
        val previous = viewModel(conversation)
        conversation.requestMode = Conversation.Mode.Feed
        val updated = viewModel(conversation)
        assertNotEquals(previous, updated)
        assertTrue(updated.isFeed())
        assertTrue(updated.isGroup())
    }
}
