/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.jami.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class HashUtils {

    private static final String TAG = HashUtils.class.getSimpleName();

    private HashUtils() {
    }

    public static String md5(String s) {
        return hash(s, "MD5");
    }

    public static String sha1(String s) {
        return hash(s, "SHA-1");
    }

    private static String hash(final String s, final String algo) {
        String result = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algo);
            messageDigest.update(s.getBytes(), 0, s.length());
            result = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Not able to find MD5 algorithm", e);
        }
        return result;
    }

    @SafeVarargs
    public static <T> Set<T> asSet(T... items) {
        HashSet<T> s = new HashSet<>(items.length);
        for (T t : items)
            s.add(t);
        return s;
    }
}
