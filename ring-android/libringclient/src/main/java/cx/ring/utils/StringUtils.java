/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public final class StringUtils {

    static private final HashSet<Character.UnicodeBlock> EMOJI_BLOCKS = new HashSet<>(Arrays.asList(
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
    ));

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String capitalize(String s) {
        if (isEmpty(s)) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
    public static String toPassword(String s){
        if(s == null || s.isEmpty()){
            return "";
        }
        char[] chars = new char[s.length()];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public static String toNumber(String s) {
        if (s == null)
            return null;
        return s.replace("(", "")
                .replace(")", "")
                .replace("-", "")
                .replace(" ", "");
    }

    public static String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == 0)
            return "";
        return filename.substring(dot + 1);
    }

    public static Iterable<Integer> codePoints(final String string) {
        return () -> new Iterator<Integer>() {
            int nextIndex = 0;
            public boolean hasNext() {
                return nextIndex < string.length();
            }
            public Integer next() {
                int result = string.codePointAt(nextIndex);
                nextIndex += Character.charCount(result);
                return result;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static boolean isOnlyEmoji(final String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        for (int codePoint : StringUtils.codePoints(message)) {
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            // Common Emoji range: https://en.wikipedia.org/wiki/Unicode_block
            if (codePoint >= 0x1F000 && codePoint < 0x20000) {
                continue;
            }
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            if (!EMOJI_BLOCKS.contains(block)) {
                return false;
            }
        }
        return true;
    }

}
