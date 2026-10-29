package net.jami.services

import net.jami.daemon.JamiService
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.AccountDevice
import net.jami.model.ConfigKey
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.model.Uri
import net.jami.model.interaction.ContactEvent
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledExecutorService

class AccountServiceTest {
    private lateinit var service: AccountService

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        val executor = mock(ScheduledExecutorService::class.java)
        doAnswer {
            it.getArgument<Runnable>(0).run()
            null
        }.`when`(executor).execute(any(Runnable::class.java))
        doAnswer {
            FutureTask(it.getArgument<Callable<Any?>>(0)).apply { run() }
        }.`when`(executor).submit(any(Callable::class.java))
        service = spy(AccountService(
            executor,
            mock(HistoryService::class.java),
            mock(DeviceRuntimeService::class.java),
            mock(VCardService::class.java)
        ))
    }

    @Test
    fun rejectedNameLookupCompletesWithFailure() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenReturn(false)

            service.findRegistrationByName("account", "", "alice").test()
                .assertValue { it.query == "alice" && it.state == AccountService.LookupState.NetworkError }
                .assertComplete()
        }
    }

    @Test
    fun rejectedAddressLookupCompletesWithFailure() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupAddress("account", "", "address") }.thenReturn(false)

            service.findRegistrationByAddress("account", "", "address").test()
                .assertValue { it.query == "address" && it.state == AccountService.LookupState.NetworkError }
                .assertComplete()
        }
    }

    @Test
    fun acceptedLookupWaitsForMatchingCallback() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenReturn(true)
            val result = service.findRegistrationByName("account", "", "alice").test()
            result.assertNoValues().assertNotComplete()

            service.registeredNameFound("other-account", "alice", 0, "other-address", "alice")
            service.registeredNameFound("account", "other-name", 0, "other-address", "other-name")
            result.assertNoValues()

            service.registeredNameFound("account", "alice", 0, "address", "alice")
            result.assertValue { it.address == "address" && it.state == AccountService.LookupState.Success }
                .assertComplete()
        }
    }

    @Test
    fun synchronousCallbackIsNotLost() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenAnswer {
                service.registeredNameFound("account", "alice", 0, "address", "alice")
                true
            }

            service.findRegistrationByName("account", "", "alice").test()
                .assertValue { it.address == "address" && it.state == AccountService.LookupState.Success }
                .assertComplete()
        }
    }

    @Test
    fun deviceNotificationReloadsTimestampsAndRemovals() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val previous = AccountDevice("device", "Phone", 10)
        val updated = previous.copy(lastSync = 20)
        account.devices = mapOf("device" to previous, "revoked" to AccountDevice("revoked", "Old phone"))
        doReturn(account).`when`(service).getAccount("account")
        doReturn(mapOf("device" to updated)).`when`(service).loadAccountDevices("account")

        service.knownDevicesChanged("account")

        verify(service).loadAccountDevices("account")
        assertEquals(mapOf("device" to updated), account.devices)
    }

    @Test
    fun historyEventsPreserveContactAddedDate() {
        val parser = AccountService::class.java.getDeclaredMethod(
            "getInteraction", Account::class.java, Conversation::class.java, Map::class.java
        ).apply { isAccessible = true }
        val peer = "a".repeat(40)
        for (added in listOf(null, 1700000000123L)) {
            val account = Account("account",
                mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
                emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
            val contact = account.getContactFromCache(Uri.fromId(peer))
            added?.let { contact.updateDates(mapOf("addedMs" to it.toString())) }
            val conversation = account.newSwarm("conversation", Conversation.Mode.OneToOne)
            conversation.addContact(contact, MemberRole.MEMBER)
            for (type in listOf("initial", "member")) {
                val message = mapOf(
                    "id" to "message", "type" to type, "author" to peer,
                    "timestamp" to "1700000100", "invited" to peer, "uri" to peer, "action" to "join"
                )
                val event = parser.invoke(service, account, conversation, message) as ContactEvent

                assertEquals(1700000100000L, event.timestamp)
                assertSame(contact, event.contact)
                assertEquals(added, contact.addedDate?.time)
                assertNull(contact.removedDate)
                assertEquals(
                    if (type == "initial") ContactEvent.Event.INVITED else ContactEvent.Event.ADDED,
                    event.event
                )
            }
        }
    }
}
