/*
 * Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.nio.ByteBuffer

class EncodedVideoPacketTest {
    @Test
    fun prependsCodecDataWithoutChangingSourceBuffers() {
        val codecData = ByteBuffer.allocateDirect(4).apply {
            put(byteArrayOf(0, 0, 0, 1))
            rewind()
        }
        val encodedFrame = ByteBuffer.allocateDirect(6).apply {
            put(byteArrayOf(9, 9, 0x65, 1, 2, 9))
            position(1)
        }

        val packet = prependCodecData(codecData, encodedFrame, 2, 3)
        val bytes = ByteArray(packet.remaining())
        packet.get(bytes)

        assertArrayEquals(byteArrayOf(0, 0, 0, 1, 0x65, 1, 2), bytes)
        assertEquals(0, codecData.position())
        assertEquals(1, encodedFrame.position())
    }

    @Test
    fun reusesPacketBufferWhenItIsLargeEnough() {
        val codecData = ByteBuffer.allocateDirect(2).apply {
            put(byteArrayOf(7, 8))
            rewind()
        }
        val encodedFrame = ByteBuffer.allocateDirect(4).apply {
            put(byteArrayOf(1, 2, 3, 4))
            rewind()
        }
        val reusable = ByteBuffer.allocateDirect(16)

        val packet = prependCodecData(codecData, encodedFrame, 1, 2, reusable)

        assertSame(reusable, packet)
        val bytes = ByteArray(packet.remaining())
        packet.get(bytes)
        assertArrayEquals(byteArrayOf(7, 8, 2, 3), bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFrameRangePastBufferCapacity() {
        prependCodecData(
            ByteBuffer.allocateDirect(2),
            ByteBuffer.allocateDirect(4),
            3,
            2
        )
    }
}
