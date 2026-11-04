package cx.ring.client

import net.jami.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationDetailsTabsTest {
    @Test
    fun feedsRetainDetailsMembersAndFilesWithoutDocuments() {
        assertEquals(
            listOf(
                ConversationDetailsTab.Details,
                ConversationDetailsTab.Members,
                ConversationDetailsTab.Files
            ),
            groupConversationDetailsTabs(Conversation.Mode.Feed)
        )
    }

    @Test
    fun normalGroupsRetainTheirDocumentsTab() {
        for (mode in listOf(
            Conversation.Mode.AdminInvitesOnly,
            Conversation.Mode.InvitesOnly,
            Conversation.Mode.Public
        )) {
            assertEquals(
                listOf(
                    ConversationDetailsTab.Details,
                    ConversationDetailsTab.Members,
                    ConversationDetailsTab.Files,
                    ConversationDetailsTab.Documents
                ),
                groupConversationDetailsTabs(mode)
            )
        }
    }
}
