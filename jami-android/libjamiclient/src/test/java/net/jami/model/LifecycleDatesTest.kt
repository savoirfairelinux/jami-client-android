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

    @Test
    fun pendingRequestsKeepReceivedDateOrderBeforeAndAfterOpening() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
        val contact = Uri.fromId("a".repeat(40))
        val group = Uri(Uri.SWARM_SCHEME, "group")
        val feed = Uri(Uri.SWARM_SCHEME, "feed")
        account.addRequest(TrustRequest("account", contact, 1000L, contact, mode = Conversation.Mode.OneToOne))
        account.addRequest(TrustRequest("account", Uri.fromId("b".repeat(40)), 2000L, group, mode = Conversation.Mode.InvitesOnly))
        account.addRequest(TrustRequest("account", Uri.fromId("c".repeat(40)), 3000L, feed, mode = Conversation.Mode.Feed))
        val pending = account.getPendingSubject().test()
        val expected = listOf(feed, group, contact)

        assertEquals(expected, pending.values().last().map { it.uri })
        for (uri in listOf(contact, group, feed)) {
            account.getByUri(uri)!!.sortedHistory.blockingGet()
            assertEquals("After opening $uri", expected, pending.values().last().map { it.uri })
        }
        pending.dispose()
    }

    @Test
    fun pendingRequestsWithEqualDatesKeepADeterministicOrderAfterOpening() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
        val first = Uri(Uri.SWARM_SCHEME, "a")
        val second = Uri(Uri.SWARM_SCHEME, "b")
        account.addRequest(TrustRequest("account", Uri.fromId("a".repeat(40)), 1000L, second, mode = Conversation.Mode.InvitesOnly))
        account.addRequest(TrustRequest("account", Uri.fromId("a".repeat(40)), 1000L, first, mode = Conversation.Mode.InvitesOnly))
        val pending = account.getPendingSubject().test()
        val expected = listOf(first, second)

        assertEquals(expected, pending.values().last().map { it.uri })
        account.getByUri(second)!!.sortedHistory.blockingGet()
        assertEquals(expected, pending.values().last().map { it.uri })
        pending.dispose()
    }
}
