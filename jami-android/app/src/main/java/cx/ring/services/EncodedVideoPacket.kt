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

import java.nio.ByteBuffer

internal fun prependCodecData(
    codecData: ByteBuffer,
    encodedFrame: ByteBuffer,
    offset: Int,
    size: Int,
    reusableBuffer: ByteBuffer? = null
): ByteBuffer {
    require(offset >= 0 && size >= 0 && offset <= encodedFrame.capacity() - size)

    val config = codecData.duplicate().apply {
        position(0)
        limit(codecData.capacity())
    }
    val frame = encodedFrame.duplicate().apply {
        position(offset)
        limit(offset + size)
    }
    val requiredCapacity = config.remaining() + frame.remaining()
    val packet = reusableBuffer?.takeIf { it.capacity() >= requiredCapacity }
        ?: ByteBuffer.allocateDirect(requiredCapacity)
    return packet.apply {
        clear()
        put(config)
        put(frame)
        flip()
    }
}
