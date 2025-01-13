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
        Character.UnicodeBlock.VARIATION_SELECTORS,
        Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT,
        Character.UnicodeBlock.TAGS
    ))

    fun capitalize(s: String): String =
        s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun toPassword(s: String): String {
        if (s.isEmpty()) return ""
        return String(CharArray(s.length).apply { Arrays.fill(this, '●') })
    }

    fun toNumber(s: String): String = s.replace("(", "").replace(")", "").replace("-", "").replace(" ", "")

    fun getFileExtension(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot == -1 || dot == 0) "" else filename.substring(dot + 1)
    }

    private fun codePoints(string: String) = Iterable {
        object : Iterator<Int> {
            var nextIndex = 0
            override fun hasNext() = nextIndex < string.length

            override fun next() = string.codePointAt(nextIndex)
                .apply { nextIndex += Character.charCount(this) }
        }
    }

    fun isOnlyEmoji(message: String?): Boolean {
        if (message.isNullOrEmpty()) {
            return false
        }
        for (codePoint in codePoints(message)) {
            if (Character.isWhitespace(codePoint)) {
                continue
            }
            // Common Emoji range: https://en.wikipedia.org/wiki/Unicode_block
            if (codePoint in 0x1F000..0x1ffff || codePoint == 0x200D) {
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