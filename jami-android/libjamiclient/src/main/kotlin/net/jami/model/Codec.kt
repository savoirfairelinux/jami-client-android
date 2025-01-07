/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.model

class Codec(val payload: Long, audioCodecDetails: Map<String, String>, enabled: Boolean) {
    enum class Type {
        AUDIO, VIDEO
    }

    private val mName: String? = audioCodecDetails["CodecInfo.name"]
    val type: Type = if (audioCodecDetails["CodecInfo.type"].contentEquals("AUDIO")) Type.AUDIO else Type.VIDEO
    var sampleRate: String? = audioCodecDetails["CodecInfo.sampleRate"]
    var bitRate: String? = audioCodecDetails["CodecInfo.bitrate"]
    var channels: String? = audioCodecDetails["CodecInfo.channelNumber"]
    var isEnabled: Boolean = enabled
    override fun toString(): String {
        return """
               Codec: $name
               Payload: $payload
               Sample Rate: $sampleRate
               Bit Rate: $bitRate
               Channels: $channels
               """.trimIndent()
    }

    val name: CharSequence?
        get() = mName

    fun toggleState() {
        isEnabled = !isEnabled
    }

    override fun equals(other: Any?): Boolean {
        return other is Codec && other.payload == payload
    }

    val isSpeex: Boolean
        get() = mName.contentEquals("Speex")
}