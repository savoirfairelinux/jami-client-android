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
package cx.ring.utils

import android.text.InputFilter
import android.text.Spanned
import android.text.SpannableString
import java.lang.StringBuilder
import java.util.regex.Pattern

class RegisteredNameFilter : InputFilter {
    private val nameCharMatcher = REGISTERED_NAME_CHAR_PATTERN.matcher("")
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned,dstart: Int, dend: Int): CharSequence? {
        var keepOriginal = true
        val sb = StringBuilder(end - start)
        for (i in start until end) {
            val c = source[i]
            if (isCharAllowed(c)) {
                sb.append(c.lowercase())
                if (c.isUpperCase()) keepOriginal = false
            } else {
                keepOriginal = false
            }
        }
        return if (keepOriginal) {
            null
        } else {
            if (source is Spanned) {
                SpannableString(sb)
            } else {
                sb
            }
        }
    }

    private fun isCharAllowed(c: Char): Boolean {
        return nameCharMatcher.reset(c.toString()).matches()
    }

    companion object {
        private val REGISTERED_NAME_CHAR_PATTERN =
            Pattern.compile("[a-z0-9_\\-]", Pattern.CASE_INSENSITIVE)
    }
}