package cx.ring.account

import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.text.DateFormat

class DeviceAdapterTest {
    private val warnings = mutableListOf<String>()

    @Before
    fun setUp() {
        Log.injectLogService(object : LogService {
            override fun e(tag: String, message: String) {}
            override fun d(tag: String, message: String) {}
            override fun w(tag: String, message: String) {
                warnings.add(message)
            }
            override fun i(tag: String, message: String) {}
            override fun e(tag: String, message: String, e: Throwable) {}
            override fun d(tag: String, message: String, e: Throwable) {}
            override fun w(tag: String, message: String, e: Throwable) {}
            override fun i(tag: String, message: String, e: Throwable) {}
        })
    }

    @Test
    fun validSecondsAreConvertedWithoutOverflow() {
        assertEquals(1700000000000L, DeviceAdapter.lastSyncDate(1700000000L)!!.time)
        val largestSeconds = Long.MAX_VALUE / 1000L
        val largestDate = DeviceAdapter.lastSyncDate(largestSeconds)!!
        assertEquals(largestSeconds * 1000L, largestDate.time)
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(largestDate)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun unknownTimestampsHaveNoDate() {
        for (timestamp in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertNull(DeviceAdapter.lastSyncDate(timestamp))
        }
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun overflowingSecondsAreLoggedAndHaveNoDate() {
        assertNull(DeviceAdapter.lastSyncDate(Long.MAX_VALUE / 1000L + 1L))
        assertNull(DeviceAdapter.lastSyncDate(Long.MAX_VALUE))
        assertEquals(2, warnings.size)
    }
}
