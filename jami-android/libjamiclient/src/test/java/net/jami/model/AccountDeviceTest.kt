package net.jami.model

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

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
    fun missingAndZeroTimestampsMeanNeverSynced() {
        val details = mapOf("id" to "device-id", "label" to "Phone")

        assertEquals(0L, AccountDevice.fromMap(details).lastSync)
        assertEquals(0L, AccountDevice.fromMap(details + ("lastSync" to "0")).lastSync)
    }

    @Test
    fun malformedTimestampsAreLoggedAndMeanNeverSynced() {
        val logger = mock(LogService::class.java)
        Log.injectLogService(logger)
        val invalidTimestamps = listOf("invalid", "", "1.5", "9223372036854775808", "-9223372036854775809")

        for (timestamp in invalidTimestamps) {
            val device = AccountDevice.fromMap(
                mapOf("id" to "device-id", "label" to "Phone", "lastSync" to timestamp)
            )

            assertEquals("device-id", device.id)
            assertEquals("Phone", device.name)
            assertEquals("lastSync=$timestamp", 0L, device.lastSync)
        }

        verify(logger, times(invalidTimestamps.size)).w("AccountDevice", "Invalid lastSync metadata for device device-id")
    }

    @Test(expected = NoSuchElementException::class)
    fun rejectsMissingDeviceId() {
        AccountDevice.fromMap(mapOf("label" to "Phone"))
    }
}
