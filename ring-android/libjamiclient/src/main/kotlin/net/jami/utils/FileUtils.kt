/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package net.jami.utils

import java.io.*

object FileUtils {
    private val TAG = FileUtils::class.simpleName!!

    // Buffer size based on https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
    inline fun copyFile(input: InputStream, out: OutputStream) = input.copyTo(out, bufferSize = 64 * 1024)

    fun copyFile(src: File, dest: File): Boolean {
        try {
            FileInputStream(src).use { inputStream ->
                FileOutputStream(dest).use { outputStream ->
                    copyFile(inputStream, outputStream)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Can't copy file", e)
            return false
        }
        return true
    }

    fun moveFile(file: File, dest: File): Boolean {
        if (!file.exists() || !file.canRead()) {
            Log.d(TAG, "moveFile: file is not accessible " + file.exists() + " " + file.canRead())
            return false
        }
        if (file == dest) return true
        if (!file.renameTo(dest)) {
            Log.w(TAG, "moveFile: can't rename file, trying copy+delete to $dest")
            if (!copyFile(file, dest)) {
                Log.w(TAG, "moveFile: can't copy file to $dest")
                return false
            }
            if (!file.delete()) {
                Log.w(TAG, "moveFile: can't delete old file from $file")
            }
        }
        Log.d(TAG, "moveFile: moved $file to $dest")
        return true
    }
}