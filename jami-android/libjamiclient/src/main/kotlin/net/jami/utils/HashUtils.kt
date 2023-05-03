/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package net.jami.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

object HashUtils {
    private val TAG = HashUtils::class.simpleName!!
    fun md5(s: String) = hash(s, "MD5")!!
    fun sha1(s: String) = hash(s, "SHA-1")!!

    private fun hashRaw(bytes: ByteArray, algo: String) = try {
        MessageDigest.getInstance(algo)
            .apply { update(bytes, 0, bytes.size) }
            .digest()
    } catch (e: NoSuchAlgorithmException) {
        Log.e(TAG, "Can't find hash algorithm $algo")
        null
    }

    private fun hash(s: String, algo: String) = hashRaw(s.toByteArray(), algo)
}