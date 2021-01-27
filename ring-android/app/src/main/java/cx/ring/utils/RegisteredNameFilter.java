/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils;

import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisteredNameFilter implements InputFilter {
    private static final Pattern REGISTERED_NAME_CHAR_PATTERN = Pattern.compile("[a-z0-9_\\-]", Pattern.CASE_INSENSITIVE);
    private final Matcher nameCharMatcher = REGISTERED_NAME_CHAR_PATTERN.matcher("");

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        boolean keepOriginal = true;
        StringBuilder sb = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (isCharAllowed(c)) {
                sb.append(c);
            } else {
                keepOriginal = false;
            }
        }
        if (keepOriginal) {
            return null;
        } else {
            if (source instanceof Spanned) {
                return new SpannableString(sb);
            } else {
                return sb;
            }
        }
    }

    private boolean isCharAllowed(char c) {
        return nameCharMatcher.reset(String.valueOf(c)).matches();
    }
}
