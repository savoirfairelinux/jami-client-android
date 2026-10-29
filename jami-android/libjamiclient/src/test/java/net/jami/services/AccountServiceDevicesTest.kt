package net.jami.services

import net.jami.model.Account
import net.jami.model.AccountDevice
import net.jami.model.ConfigKey
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.ScheduledExecutorService

class AccountServiceDevicesTest {
    @Test
    fun deviceNotificationReloadsTimestampsAndRemovals() {
        Log.injectLogService(mock(LogService::class.java))
        val service = spy(AccountService(
            mock(ScheduledExecutorService::class.java),
            mock(HistoryService::class.java),
            mock(DeviceRuntimeService::class.java),
            mock(VCardService::class.java)
        ))
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
}
