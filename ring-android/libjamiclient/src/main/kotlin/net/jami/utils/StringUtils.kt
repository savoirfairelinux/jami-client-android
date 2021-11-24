/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.utils

import java.util.*

object StringUtils {
    private val EMOJI_BLOCKS: Set<Character.UnicodeBlock> = HashSet(listOf(
        Character.UnicodeBlock.EMOTICONS,
        Character.UnicodeBlock.DINGBATS,
        Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS,
        Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
        Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_ARROWS,
        Character.UnicodeBlock.ALCHEMICAL_SYMBOLS,
        Character.UnicodeBlock.ARROWS,
        Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT,
        Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS,
        Character.UnicodeBlock.VARIATION_SELECTORS // Ignore modifier
    ))

    fun isEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }

    fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.isEmpty()
    }

    fun capitalize(s: String): String {
        if (isEmpty(s)) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    fun toPassword(s: String): String {
        if (isEmpty(s)) {
            return ""
        }
        val chars = CharArray(s.length)
        Arrays.fill(chars, '*')
        return String(chars)
    }

    fun toNumber(s: String?): String? {
        return s?.replace("(", "")?.replace(")", "")?.replace("-", "")?.replace(" ", "")
    }

    fun getFileExtension(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot == -1 || dot == 0) "" else filename.substring(dot + 1)
    }

    private fun codePoints(string: String): Iterable<Int> {
        return Iterable {
            object : Iterator<Int> {
                var nextIndex = 0
                override fun hasNext(): Boolean {
                    return nextIndex < string.length
                }

                override fun next(): Int {
                    val result = string.codePointAt(nextIndex)
                    nextIndex += Character.charCount(result)
                    return result
                }
            }
        }
    }

    fun isOnlyEmoji(message: String?): Boolean {
        if (message == null || message.isEmpty()) {
            return false
        }
        for (codePoint in codePoints(message)) {
            if (Character.isWhitespace(codePoint)) {
                continue
            }
            // Common Emoji range: https://en.wikipedia.org/wiki/Unicode_block
            if (codePoint in 0x1F000..0x1ffff) {
                continue
            }
            val block = Character.UnicodeBlock.of(codePoint)
            if (!EMOJI_BLOCKS.contains(block)) {
                return false
            }
        }
        return true
    }
}