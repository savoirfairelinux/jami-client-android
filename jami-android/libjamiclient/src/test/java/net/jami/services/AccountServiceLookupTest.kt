package net.jami.services

import net.jami.daemon.JamiService
import net.jami.utils.Log
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledExecutorService

class AccountServiceLookupTest {
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
        service = AccountService(
            executor,
            mock(HistoryService::class.java),
            mock(DeviceRuntimeService::class.java),
            mock(VCardService::class.java)
        )
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
}
