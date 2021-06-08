/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import net.jami.utils.FileUtils;

import java.io.File;

import cx.ring.BuildConfig;

/**
 * This class distributes content uri used to pass along data in the app
 */
public class ContentUriHandler {
    private final static String TAG = ContentUriHandler.class.getSimpleName();

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final String AUTHORITY_FILES = AUTHORITY + ".file_provider";
    public static final String SCHEME_TV = "jamitv";
    public static final String PATH_TV_HOME = "home";
    public static final String PATH_TV_CONVERSATION = "conversation";

    private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "conversation");
    public static final Uri ACCOUNTS_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "accounts");
    public static final Uri CONTACT_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "contact");

    private ContentUriHandler() {
        // hidden constructor
    }

    /**
     * The following is a workaround used to mitigate getUriForFile exceptions
     * on Huawei devices taken from stackoverflow
     * https://stackoverflow.com/a/41309223
     */
    private static final String HUAWEI_MANUFACTURER = "Huawei";

    public static Uri getUriForResource(@NonNull Context context, int resourceId) {
        Resources resources = context.getResources();
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();
    }

    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority, @NonNull File file) {
        return getUriForFile(context, authority, file, null);
    }
    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority, @NonNull File file, @Nullable String displayName) {
        try {
            return displayName == null ? FileProvider.getUriForFile(context, authority, file)
                    : FileProvider.getUriForFile(context, authority, file, displayName);
        } catch (IllegalArgumentException e) {
            if (HUAWEI_MANUFACTURER.equalsIgnoreCase(Build.MANUFACTURER)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Log.w(TAG, "Returning Uri.fromFile to avoid Huawei 'external-files-path' bug for pre-N devices", e);
                    return Uri.fromFile(file);
                } else {
                    Log.w(TAG, "ANR Risk -- Copying the file the location cache to avoid Huawei 'external-files-path' bug for N+ devices", e);
                    // Note: Periodically clear this cache
                    final File cacheFolder = new File(context.getCacheDir(), HUAWEI_MANUFACTURER);
                    final File cacheLocation = new File(cacheFolder, file.getName());
                    if (FileUtils.copyFile(file, cacheLocation)) {
                        Log.i(TAG, "Completed Android N+ Huawei file copy. Attempting to return the cached file");
                        return displayName == null ? FileProvider.getUriForFile(context, authority, cacheLocation)
                                : FileProvider.getUriForFile(context, authority, cacheLocation, displayName);
                    }
                    Log.e(TAG, "Failed to copy the Huawei file. Re-throwing exception");
                    throw new IllegalArgumentException("Huawei devices are unsupported for Android N");
                }
            } else {
                throw e;
            }
        }
    }
}
