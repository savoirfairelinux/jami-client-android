package net.jami.model

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AccountDeviceTest {
    @Test
    fun readsDeviceMetadataWithoutChangingTimestampUnits() {
        val device = AccountDevice.fromMap(
            mapOf("id" to "device-id", "label" to "T\u00e9l\u00e9phone", "lastSync" to "1700000000")
        )

        assertEquals("device-id", device.id)
        assertEquals("T\u00e9l\u00e9phone", device.name)
        assertEquals(1700000000L, device.lastSync)
    }

    @Test
    fun missingOrInvalidTimestampsMeanNeverSynced() {
        Log.injectLogService(mock(LogService::class.java))
        val details = mapOf("id" to "device-id", "label" to "Phone")
        assertEquals(0L, AccountDevice.fromMap(details).lastSync)
        for (timestamp in listOf("0", "invalid", "", "1.5", "9223372036854775808", "-9223372036854775809")) {
            assertEquals("lastSync=$timestamp", 0L, AccountDevice.fromMap(details + ("lastSync" to timestamp)).lastSync)
        }
    }
}
