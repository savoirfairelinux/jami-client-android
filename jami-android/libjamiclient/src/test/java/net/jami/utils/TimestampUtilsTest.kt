package net.jami.utils

import net.jami.services.LogService
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class TimestampUtilsTest {
    private lateinit var logger: LogService

    @Before
    fun setUp() {
        logger = mock(LogService::class.java)
        Log.injectLogService(logger)
    }

    @Test
    fun millisecondsTakePrecedenceIncludingZero() {
        assertEquals(1700000000123L, TimestampUtils.fromDetails(
            mapOf("created" to "1700000000", "createdMs" to "1700000000123"), "created"))
        assertEquals(0L, TimestampUtils.fromDetails(
            mapOf("created" to "1700000000", "createdMs" to "0"), "created"))
        assertEquals(Long.MAX_VALUE, TimestampUtils.fromDetails(
            mapOf("createdMs" to Long.MAX_VALUE.toString()), "created"))
        verifyNoInteractions(logger)
    }

    @Test
    fun legacySecondsAreConvertedWithinBounds() {
        val largestSeconds = Long.MAX_VALUE / 1000L
        for (seconds in listOf(0L, 1L, 1700000000L, largestSeconds)) {
            assertEquals(seconds * 1000L, TimestampUtils.fromDetails(
                mapOf("created" to seconds.toString()), "created"))
        }
        verifyNoInteractions(logger)
    }

    @Test
    fun missingDatesStayUnknownWithoutWarnings() {
        assertEquals(0L, TimestampUtils.fromDetails(emptyMap(), "created"))
        verifyNoInteractions(logger)
    }

    @Test
    fun malformedSecondsAreLoggedAndUnknown() {
        val invalid = listOf("invalid", "", "1.5", "9223372036854775808", "-9223372036854775809")
        for (value in invalid) {
            assertEquals(0L, TimestampUtils.fromDetails(mapOf("created" to value), "created"))
        }
        verify(logger, times(invalid.size)).w("TimestampUtils", "Invalid created timestamp metadata")
    }

    @Test
    fun invalidMillisecondsStayUnknownWithoutFallingBackToSeconds() {
        val invalid = listOf("invalid", "", "9223372036854775808", "-1", Long.MIN_VALUE.toString())
        for (value in invalid) {
            assertEquals(0L, TimestampUtils.fromDetails(
                mapOf("created" to "1700000000", "createdMs" to value), "created"))
        }
        verify(logger, times(invalid.size)).w("TimestampUtils", "Invalid createdMs timestamp metadata")
    }

    @Test
    fun negativeAndOverflowingSecondsAreLoggedAndUnknown() {
        val invalid = listOf(-1L, Long.MIN_VALUE, Long.MAX_VALUE / 1000L + 1L, Long.MAX_VALUE)
        for (value in invalid) {
            assertEquals(0L, TimestampUtils.fromDetails(mapOf("created" to value.toString()), "created"))
        }
        verify(logger, times(invalid.size)).w("TimestampUtils", "Invalid created timestamp metadata")
    }
}
