/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String[] SIZE_UNITS = new String[]{"B", "kB", "MB", "GB", "TB"};
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.##");

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        // Buffer size based on https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static boolean copyFile(File src, File dest) {
        try {
            InputStream inputStream = new FileInputStream(src);
            FileOutputStream outputStream = new FileOutputStream(dest);
            copyFile(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
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

    private static int getSizeDigitGroup(long size) {
        return (int) (Math.log10(size) / Math.log10(1024));
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        int digitGroups = getSizeDigitGroup(size);
        return SIZE_FORMAT.format(size / Math.pow(1024, digitGroups)) + " " + SIZE_UNITS[digitGroups];
    }

    public static CharSequence readableFileProgress(long progress, long total) {
        if (progress < 0 || total < 0)
            return "";
        int digitGroups = getSizeDigitGroup(Math.max(progress, total));
        double den = Math.pow(1024, digitGroups);
        String unit = SIZE_UNITS[digitGroups];
        return SIZE_FORMAT.format(progress / den) + " / " + SIZE_FORMAT.format(total / den) + " " + unit;
    }
}
