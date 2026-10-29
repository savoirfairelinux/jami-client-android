package net.jami.model

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        conversation.updateTimestamps(mapOf("created" to "1700000000", "createdMs" to "1700000000123"))
        conversation.updateTimestamps(mapOf("title" to "Updated title"))
        conversation.updateTimestamps(mapOf("created" to "1700000000", "createdMs" to "0"))

        dates.assertValues(0L, 1700000000123L, 0L)
    }

    @Test
    fun joinedDateAcceptsLegacySecondsAndClearsUnknownDates() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.OneToOne)
        val dates = conversation.joinedDate.test()

        conversation.updateTimestamps(mapOf("created" to "1700000000"))
        conversation.updateTimestamps(mapOf("created" to "0"))

        dates.assertValues(0L, 1700000000000L, 0L)
    }

    @Test
    fun contactDatesAcceptBothUnitsAndClearRemovalOnReaddition() {
        val contact = Contact(Uri.fromId("contact"))
        val dates = contact.updatesSubject.map { it.addedDate?.time to it.removedDate?.time }.test()
        contact.updateDates(mapOf("added" to "1700000000", "removedMs" to "1700000001123"))

        assertEquals(1700000000000L, contact.addedDate!!.time)
        assertEquals(1700000001123L, contact.removedDate!!.time)

        contact.updateDates(mapOf("addedMs" to "1700000002123", "removed" to "0"))

        assertEquals(1700000002123L, contact.addedDate!!.time)
        assertNull(contact.removedDate)
        dates.assertValues(1700000000000L to 1700000001123L, 1700000002123L to null)
    }

    @Test
    fun absentContactDatesStayUnknownAndLegacyZeroIsPreserved() {
        val contact = Contact(Uri.fromId("contact"))
        contact.updateDates(emptyMap())
        assertNull(contact.addedDate)
        assertNull(contact.removedDate)

        contact.updateDates(mapOf("added" to "0", "removed" to "0"))
        assertEquals(0L, contact.addedDate!!.time)
        assertNull(contact.removedDate)
    }

    @Test
    fun accountLoadsAndRefreshesContactDates() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val details = mapOf("id" to "contact", "added" to "1700000000", "removed" to "1700000001")
        val contact = account.addContact(details)

        assertEquals(1700000000000L, contact.addedDate!!.time)
        assertEquals(1700000001000L, contact.removedDate!!.time)

        account.addContact(details + mapOf("removed" to "0"))
        assertNull(contact.removedDate)
    }
}
