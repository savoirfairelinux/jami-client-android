/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package net.jami.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        // Buffer size based on https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static boolean copyFile(File src, File dest) {
        try (InputStream inputStream = new FileInputStream(src);
             FileOutputStream outputStream = new FileOutputStream(dest)) {
            copyFile(inputStream, outputStream);
        } catch (IOException e) {
            Log.w(TAG, "Can't copy file", e);
            return false;
        }
        return true;
    }

    public static boolean moveFile(File file, File dest) {
        if (!file.exists() || !file.canRead()) {
            Log.d(TAG, "moveFile: file is not accessible " + file.exists() + " " + file.canRead());
            return false;
        }
        if (file.equals(dest))
            return true;
        if (!file.renameTo(dest)) {
            Log.w(TAG, "moveFile: can't rename file, trying copy+delete to " + dest);
            if (!copyFile(file, dest)) {
                Log.w(TAG, "moveFile: can't copy file to " + dest);
                return false;
            }
            if (!file.delete()) {
                Log.w(TAG, "moveFile: can't delete old file from " + file);
            }
        }
        Log.d(TAG, "moveFile: moved " + file + " to " + dest);
        return true;
    }
}
