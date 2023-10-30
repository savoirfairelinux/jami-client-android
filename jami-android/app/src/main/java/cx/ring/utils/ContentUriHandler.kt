/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.core.content.FileProvider
import android.os.Build
import android.util.Log
import cx.ring.BuildConfig
import net.jami.utils.FileUtils
import java.io.File
import java.lang.IllegalArgumentException

/**
 * This class distributes content uri used to pass along data in the app
 */
object ContentUriHandler {
    private val TAG = ContentUriHandler::class.simpleName!!
    const val AUTHORITY = BuildConfig.APPLICATION_ID
    const val AUTHORITY_FILES = "$AUTHORITY.file_provider"
    const val SCHEME_TV = "jamitv"
    const val PATH_TV_HOME = "home"
    const val PATH_TV_CONVERSATION = "conversation"
    private val AUTHORITY_URI = Uri.parse("content://$AUTHORITY")

    val CONVERSATION_CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "conversation")
    val ACCOUNTS_CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "accounts")
    val CONTACT_CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "contact")

    fun immutable(flags: Int = 0): Int = PendingIntent.FLAG_IMMUTABLE or flags
    fun mutable(flags: Int = 0): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE or flags else flags

    fun getUriForResource(resources: Resources, resourceId: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(resourceId))
        .appendPath(resources.getResourceTypeName(resourceId))
        .appendPath(resources.getResourceEntryName(resourceId))
        .build()

    /**
     * The following is a workaround used to mitigate getUriForFile exceptions
     * on Huawei devices taken from stackoverflow
     * https://stackoverflow.com/a/41309223
     */
    private const val HUAWEI_MANUFACTURER = "Huawei"
    fun getUriForFile(context: Context, authority: String, file: File, displayName: String? = null): Uri {
        return try {
            if (displayName == null)
                FileProvider.getUriForFile(context, authority, file)
            else
                FileProvider.getUriForFile(context, authority, file, displayName)
        } catch (e: IllegalArgumentException) {
            if (HUAWEI_MANUFACTURER.equals(Build.MANUFACTURER, ignoreCase = true)) {
                Log.w(TAG, "ANR Risk -- Copying the file the location cache to avoid Huawei 'external-files-path' bug for N+ devices", e)
                // Note: Periodically clear this cache
                val cacheFolder = File(context.cacheDir, HUAWEI_MANUFACTURER)
                val cacheLocation = File(cacheFolder, file.name)
                if (FileUtils.copyFile(file, cacheLocation)) {
                    Log.i(TAG, "Completed Android N+ Huawei file copy. Attempting to return the cached file")
                    return if (displayName == null)
                        FileProvider.getUriForFile(context, authority, cacheLocation)
                    else
                        FileProvider.getUriForFile(context, authority, cacheLocation, displayName)
                }
                Log.e(TAG, "Failed to copy the Huawei file. Re-throwing exception")
                throw IllegalArgumentException("Huawei devices are unsupported for Android N")
            } else {
                throw e
            }
        }
    }
}