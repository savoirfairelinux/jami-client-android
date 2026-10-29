package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Test

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

    @Test(expected = NoSuchElementException::class)
    fun rejectsMissingDeviceId() {
        AccountDevice.fromMap(mapOf("label" to "Phone"))
    }
}
