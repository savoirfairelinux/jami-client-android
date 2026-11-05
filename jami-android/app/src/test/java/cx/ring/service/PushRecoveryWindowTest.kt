package cx.ring.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PushRecoveryWindowTest {
    @Test
    fun freshWakeupsExtendRecoveryButNeverPastTheEpisodeCap() {
        val window = PushRecoveryWindow()
        assertEquals(60_000L, window.extend(1_000L))
        assertEquals(60_000L, window.extend(31_000L))
        assertEquals(10_000L, window.extend(591_000L))
        assertEquals(0L, window.extend(601_000L))
        assertEquals(0L, window.extend(700_000L))
    }

    @Test
    fun zeroClockOriginDoesNotResetTheEpisode() {
        val window = PushRecoveryWindow()
        window.extend(0L)
        assertEquals(1L, window.extend(599_999L))
        assertEquals(0L, window.extend(600_000L))
    }
}
