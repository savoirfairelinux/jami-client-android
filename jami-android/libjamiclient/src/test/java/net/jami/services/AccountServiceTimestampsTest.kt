package net.jami.services

import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.StringVect
import net.jami.daemon.VectMap
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.model.interaction.ContactEvent
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.ScheduledExecutorService

class AccountServiceTimestampsTest {
    @Test
    fun syncingContactEventsPreferMillisecondsIncludingZero() {
        assertSyncingTimestamp(mapOf("created" to "1700000000", "createdMs" to "1700000000123"), 1700000000123L)
        assertSyncingTimestamp(mapOf("createdMs" to "1700000000123"), 1700000000123L)
        assertSyncingTimestamp(mapOf("created" to "1700000000", "createdMs" to "0"), 0L)
    }

    @Test
    fun syncingContactEventsConvertLegacySecondsWithinBounds() {
        for (seconds in listOf(0L, 1700000000L, Long.MAX_VALUE / 1000L)) {
            assertSyncingTimestamp(mapOf("created" to seconds.toString()), seconds * 1000L)
        }
    }

    @Test
    fun invalidSyncingDatesStayUnknownWithoutAbortingLoad() {
        for (details in listOf(
            mapOf("created" to "invalid"),
            mapOf("created" to "9223372036854775808"),
            mapOf("created" to Long.MAX_VALUE.toString()),
            mapOf("created" to (Long.MAX_VALUE / 1000L + 1L).toString()),
            mapOf("created" to "-1"),
            mapOf("created" to "1700000000", "createdMs" to "invalid")
        )) {
            assertSyncingTimestamp(details, 0L)
        }
    }

    @Test
    fun missingSyncingDatesStayUnknown() {
        assertSyncingTimestamp(emptyMap(), 0L)
    }

    private fun assertSyncingTimestamp(dates: Map<String, String>, expected: Long) {
        Log.injectLogService(mock(LogService::class.java))
        val history = mock(HistoryService::class.java)
        val vcard = mock(VCardService::class.java)
        val service = AccountService(
            mock(ScheduledExecutorService::class.java),
            history,
            mock(DeviceRuntimeService::class.java),
            vcard
        )
        val account = Account("account",
            mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
            emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val info = dates + ("syncing" to "true")
        doReturn(Profile.EMPTY_PROFILE_SINGLE).`when`(vcard).loadConversationProfile(info)
        val metadata = mock(StringMap::class.java)
        doReturn(HashMap(info)).`when`(metadata).toNativeFromUtf8()
        val records = mock(VectMap::class.java)
        doReturn(arrayListOf<Map<String, String>>()).`when`(records).toNative()
        `when`(records.iterator()).thenAnswer { emptyList<StringMap>().iterator() }
        val conversations = mock(StringVect::class.java)
        doReturn(1).`when`(conversations).size
        `when`(conversations.iterator()).thenAnswer { listOf("conversation").iterator() }

        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<VectMap> { JamiService.getDeviceList("account") }.thenReturn(records)
            daemon.`when`<VectMap> { JamiService.getContacts("account") }.thenReturn(records)
            daemon.`when`<StringVect> { JamiService.getConversations("account") }.thenReturn(conversations)
            daemon.`when`<StringMap> { JamiService.conversationInfos("account", "conversation") }.thenReturn(metadata)
            daemon.`when`<VectMap> { JamiService.getActiveCalls("account", "conversation") }.thenReturn(records)
            daemon.`when`<StringMap> { JamiService.getConversationPreferences("account", "conversation") }
                .thenReturn(mock(StringMap::class.java))
            daemon.`when`<VectMap> { JamiService.getConversationMembers("account", "conversation") }.thenReturn(records)
            daemon.`when`<VectMap> { JamiService.getConversationRequests("account") }.thenReturn(records)

            AccountService::class.java.getDeclaredMethod("loadAccount", Account::class.java).apply {
                isAccessible = true
            }.invoke(service, account)
        }

        val conversation = account.getSwarm("conversation")!!
        assertTrue(conversation.lastEvent is ContactEvent)
        assertEquals(expected, conversation.lastEvent!!.timestamp)
        assertEquals(expected, conversation.joinedDate.blockingFirst())
        verify(history).getLastMessageNotified("account", Uri(Uri.SWARM_SCHEME, "conversation"))
    }
}
