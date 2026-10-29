package net.jami.model

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class LifecycleDatesTest {
    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
    }

    @Test
    fun joinedDateUsesMillisecondsAndSurvivesProfileUpdates() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.OneToOne)
        val dates = conversation.joinedDate.test()

        conversation.updateTimestamps(mapOf("created" to "1700000000"))
        conversation.updateTimestamps(mapOf("created" to "1700000000", "createdMs" to "1700000000123"))
        conversation.updateTimestamps(mapOf("title" to "Updated title"))
        conversation.updateTimestamps(mapOf("created" to "1700000000", "createdMs" to "0"))

        dates.assertValues(0L, 1700000000000L, 1700000000123L, 0L)
    }

    @Test
    fun contactDatesAcceptBothUnitsAndClearRemovalOnReaddition() {
        val contact = Contact(Uri.fromId("contact"))
        contact.updateDates(emptyMap())
        assertNull(contact.addedDate)
        assertNull(contact.removedDate)
        val dates = contact.updatesSubject.map { it.addedDate?.time to it.removedDate?.time }.test()
        contact.updateDates(mapOf("added" to "1700000000", "removedMs" to "1700000001123"))

        assertEquals(1700000000000L, contact.addedDate!!.time)
        assertEquals(1700000001123L, contact.removedDate!!.time)

        contact.updateDates(mapOf("addedMs" to "1700000002123", "removed" to "0"))

        assertEquals(1700000002123L, contact.addedDate!!.time)
        assertNull(contact.removedDate)
        dates.assertValues(null to null, 1700000000000L to 1700000001123L, 1700000002123L to null)
    }

    @Test
    fun replacingConversationPreservesContactAddedDate() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val details = mapOf(
            "id" to "a".repeat(40),
            "addedMs" to "1700000000123",
            "removed" to "0",
            "conversationId" to "original"
        )
        val contact = account.addContact(details)
        val replacement = account.newSwarm("replacement", Conversation.Mode.OneToOne)
        replacement.updateTimestamps(mapOf("createdMs" to "1700000100123"))

        assertSame(contact, account.addContact(details + ("conversationId" to "replacement")))
        assertEquals("replacement", contact.conversationUri.blockingFirst().rawRingId)
        assertEquals(1700000000123L, contact.addedDate!!.time)
        assertEquals(1700000100123L, replacement.joinedDate.blockingFirst())
        assertNull(contact.removedDate)
    }
}
