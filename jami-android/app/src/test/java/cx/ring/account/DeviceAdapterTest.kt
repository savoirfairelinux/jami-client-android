package cx.ring.account

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceAdapterTest {
    @Test
    fun lastSyncDatesHandleSecondsAndUnknownValues() {
        Log.injectLogService(object : LogService {
            override fun e(tag: String, message: String) {}
            override fun d(tag: String, message: String) {}
            override fun w(tag: String, message: String) {}
            override fun i(tag: String, message: String) {}
            override fun e(tag: String, message: String, e: Throwable) {}
            override fun d(tag: String, message: String, e: Throwable) {}
            override fun w(tag: String, message: String, e: Throwable) {}
            override fun i(tag: String, message: String, e: Throwable) {}
        })
        assertEquals(1700000000000L, DeviceAdapter.lastSyncDate(1700000000L)!!.time)
        val largestSeconds = Long.MAX_VALUE / 1000L
        assertEquals(largestSeconds * 1000L, DeviceAdapter.lastSyncDate(largestSeconds)!!.time)
        for (timestamp in listOf(0L, -1L, Long.MIN_VALUE, largestSeconds + 1L, Long.MAX_VALUE)) {
            assertNull(DeviceAdapter.lastSyncDate(timestamp))
        }
    }
}
