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

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.system.Os
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import cx.ring.R
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Conversation
import net.jami.utils.FileUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object AndroidFileUtils {
    private val TAG = AndroidFileUtils::class.simpleName
    private const val ORIENTATION_LEFT = 270
    private const val ORIENTATION_RIGHT = 90
    private const val MAX_IMAGE_DIMENSION = 1024

    fun linkOrCopy(oldPath: String, newPath: String): Boolean {
        try {
            Os.symlink(oldPath, newPath)
        } catch (e: Exception) {
            return FileUtils.copyFile(File(oldPath), File(newPath))
        }
        return true
    }

    /**
     * Copy assets from a folder recursively ( files and subfolder)
     * @param assetManager Asset Manager ( you can get it from Context.getAssets() )
     * @param fromAssetPath path to the assets folder we want to copy
     * @param toPath a directory in internal storage
     * @return true if success
     */
    fun copyAssetFolder(assetManager: AssetManager, fromAssetPath: String, toPath: File): Boolean = try {
        var res = true

        // mkdirs checks if the folder exists and if not creates it
        toPath.mkdirs()

        // List the files of this asset directory
        val files = assetManager.list(fromAssetPath)
        if (files != null) {
            for (file in files) {
                val subAsset = fromAssetPath + File.separator + file
                if (isAssetDirectory(assetManager, subAsset)) {
                    val destination = toPath.absolutePath + File.separator + file
                    val newDir = File(destination)
                    copyAssetFolder(assetManager, subAsset, newDir)
                    Log.d(TAG, "Copied folder: $subAsset to $newDir")
                } else {
                    val newFile = File(toPath, file)
                    res = res and copyAsset(assetManager, fromAssetPath + File.separator + file, newFile)
                    Log.d(TAG, "Copied file: $subAsset to $newFile")
                }
            }
        }
        res
    } catch (e: IOException) {
        Log.e(TAG, "Error while copying asset folder", e)
        false
    }

    /**
     * Checks whether an asset is a file or a directory
     * @param assetManager Asset Manager ( you can get it from Context.getAssets() )
     * @param fromAssetPath  asset path, if just a file in  assets root folder then it should be
     * the file name, otherwise, folder/filename
     * @return boolean directory or not
     */
    private fun isAssetDirectory(assetManager: AssetManager, fromAssetPath: String): Boolean {
        try {
            if (assetManager.list(fromAssetPath)?.isNotEmpty() == true) {
                return true
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error while reading an asset ", e)
        }
        return false
    }

    /**
     * Prints assets tree
     * @param assetManager Asset Manager ( you can get it from Context.getAssets() )
     * @param rootPath default empty, sub folder otherwise
     * @param fileName the name of the file or folder
     * @param level default 0, should be 0
     */
    fun assetTree(assetManager: AssetManager, rootPath: String, fileName: String, level: Int) {
        try {
            val fromAssetPath: String = if (TextUtils.isEmpty(rootPath)) {
                fileName
            } else {
                rootPath + File.separator + fileName
            }
            val repeated = String(CharArray(level)).replace("\u0000", "\t|")
            val files = assetManager.list(fromAssetPath)
            if (files != null) {
                Log.d(TAG, "|$repeated-- $fileName")
                for (file in files) {
                    assetTree(assetManager, fromAssetPath, file, level + 1)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error while reading asset ", e)
        }
    }

    fun copyAsset(assetManager: AssetManager, fromAssetPath: String, toPath: File): Boolean = try {
        assetManager.open(fromAssetPath).use { input ->
            FileOutputStream(toPath).use { out ->
                FileUtils.copyFile(input, out)
                out.flush()
                return true
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error while copying asset", e)
        false
    }

    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var path: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    path = Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
                path = getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                path = getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            path = getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            path = uri.path
        }
        return path
    }

    private fun getFilename(cr: ContentResolver, uri: Uri): String {
        var result: String? = null
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            cr.query(uri, null, null, null, null)
                .use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        // Specific case where an image is paste from the clipboard.
                        result = if (cr.getType(uri) == MediaStore.Images.Media.CONTENT_TYPE) {
                            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                            path.substring(path.lastIndexOf("/") + 1)
                        } else {
                            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        }
                    }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        if (result!!.lastIndexOf('.') == -1) {
            val mimeType = getMimeType(cr, uri)
            val extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extensionFromMimeType != null) {
                result += ".$extensionFromMimeType"
            }
        }
        return result ?: uri.lastPathSegment!!
    }

    private fun getMimeType(cr: ContentResolver, uri: Uri): String? =
        if (ContentResolver.SCHEME_CONTENT == uri.scheme)
            cr.getType(uri)
        else
            getMimeType(uri.toString())

    fun getMimeType(filename: String): String {
        val pos = filename.lastIndexOf(".")
        var fileExtension: String? = null
        if (pos >= 0) {
            fileExtension = MimeTypeMap.getFileExtensionFromUrl(filename.substring(pos))
        }
        return getMimeTypeFromExtension(fileExtension)
    }

    fun getMimeTypeFromExtension(ext: String?): String {
        if (!ext.isNullOrEmpty()) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            if (!mimeType.isNullOrEmpty()) return mimeType
            if (ext == "gz") {
                return "application/gzip"
            }
        }
        return "application/octet-stream"
    }

    fun getTempShareDir(context: Context) = File(context.cacheDir, "tmp").apply { mkdir() }

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun createImageFile(context: Context): File =
        File.createTempFile("img_${dateFormat.format(Date())}_", ".jpg", getTempShareDir(context))

    fun createAudioFile(context: Context): File =
        File.createTempFile("audio_${dateFormat.format(Date())}_", ".mp3", getTempShareDir(context))

    fun createVideoFile(context: Context): File =
        File.createTempFile("video_${dateFormat.format(Date())}_", ".mp4", getTempShareDir(context))

    fun createLogFile(context: Context): File =
        File.createTempFile("log_${dateFormat.format(Date())}_", ".log", getTempShareDir(context))

    /**
     * Copies a file from a uri whether locally on a remote location to the local cache
     * @param context Context to get access to cache directory
     * @param uri uri of the
     * @return Single<File> which points to the newly created copy in the cache
    </File> */
    fun getCacheFile(context: Context, uri: Uri): Single<File> {
        val contentResolver = context.contentResolver
        val cacheDir = context.cacheDir
        return Single.fromCallable {
            val file = File(cacheDir, getFilename(contentResolver, uri))
            contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(file).use { output ->
                    if (inputStream == null) throw FileNotFoundException()
                    FileUtils.copyFile(inputStream, output)
                    output.flush()
                }
            }
            file
        }.subscribeOn(Schedulers.io())
    }

    fun getFileToSend(context: Context, conversation: Conversation, uri: Uri): Single<File> {
        val contentResolver = context.contentResolver
        val cacheDir = context.cacheDir
        return Single.fromCallable {
            val file = File(cacheDir, getFilename(contentResolver, uri))
            contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(file).use { output ->
                    if (inputStream == null) throw FileNotFoundException()
                    FileUtils.copyFile(inputStream, output)
                    output.flush()
                }
            }
            file
        }.subscribeOn(Schedulers.io())
    }

    fun copyUri(cr: ContentResolver, input: Uri, outUri: Uri): Completable = Completable.fromAction {
        cr.openInputStream(input).use { inputStream ->
            cr.openOutputStream(outUri).use { output ->
                if (inputStream == null || output == null) throw FileNotFoundException()
                FileUtils.copyFile(inputStream, output)
            }
        }
    }.subscribeOn(Schedulers.io())

    fun moveToUri(cr: ContentResolver, input: File, outUri: Uri): Completable = Completable.fromAction {
        FileInputStream(input).use { inputStream ->
            cr.openOutputStream(outUri).use { output ->
                if (output == null) throw FileNotFoundException()
                FileUtils.copyFile(inputStream, output)
            }
        }
        input.delete()
    }.subscribeOn(Schedulers.io())

    /**
     * Copies a file to a predefined Uri destination
     * Uses the underlying copyFile(InputStream,OutputStream)
     * @param cr content resolver
     * @param input the file we want to copy
     * @param outUri the uri destination
     * @return success value
     */
    fun copyFileToUri(cr: ContentResolver, input: File?, outUri: Uri): Completable = Completable.fromAction {
        FileInputStream(input).use { inputStream ->
            cr.openOutputStream(outUri)?.use { outputStream ->
                FileUtils.copyFile(inputStream, outputStream)
            } }
    }.subscribeOn(Schedulers.io())

    fun getCachePath(context: Context, filename: String) = File(context.cacheDir, filename)

    fun getFilePath(context: Context, filename: String): File = context.getFileStreamPath(filename)

    fun getConversationDir(context: Context, conversationId: String): File {
        val conversationsDir = getFilePath(context, "conversation_data")
        if (!conversationsDir.exists()) conversationsDir.mkdir()
        val conversationDir = File(conversationsDir, conversationId)
        if (!conversationDir.exists()) conversationDir.mkdir()
        return conversationDir
    }

    fun getConversationDir(context: Context, accountId: String, conversationId: String): File {
        val conversationsDir = getFilePath(context, "conversation_data")
        if (!conversationsDir.exists()) conversationsDir.mkdir()
        val accountDir = File(conversationsDir, accountId)
        if (!accountDir.exists()) accountDir.mkdir()
        val conversationDir = File(accountDir, conversationId)
        if (!conversationDir.exists()) conversationDir.mkdir()
        return conversationDir
    }

    fun getConversationPath(context: Context, accountId: String, conversationId: String, name: String) =
        File(getConversationDir(context, accountId, conversationId), name)

    fun getAccountPath(context: Context, accountId: String) = File(getFilePath(context, "account_data"), accountId)

    fun getTempPath(context: Context, conversationId: String, name: String): File {
        val conversationsDir = getCachePath(context, "conversation_data")
        if (!conversationsDir.exists()) conversationsDir.mkdir()
        val conversationDir = File(conversationsDir, conversationId)
        if (!conversationDir.exists()) conversationDir.mkdir()
        return File(conversationDir, name)
    }

    @Throws(IOException::class)
    fun writeCacheFileToExtStorage(context: Context, cacheFile: Uri, targetFilename: String): String {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var fileCount = 0
        var finalFile = File(downloadsDirectory, targetFilename)
        val lastDotIndex = targetFilename.lastIndexOf('.')
        val filename = targetFilename.substring(0, lastDotIndex)
        val extension = targetFilename.substring(lastDotIndex + 1)
        while (finalFile.exists()) {
            finalFile = File(downloadsDirectory, filename + "_" + fileCount + '.' + extension)
            fileCount++
        }
        Log.d(TAG, "writeCacheFileToExtStorage: finalFile=" + finalFile + ",exists=" + finalFile.exists())
        context.contentResolver.openInputStream(cacheFile)?.use { inputStream ->
            FileOutputStream(finalFile).use { output ->
                FileUtils.copyFile(inputStream, output)
            } }
        return finalFile.toString()
    }

    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    private fun isExternalStorageDocument(uri: Uri): Boolean =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri): Boolean =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri): Boolean =
        "com.android.providers.media.documents" == uri.authority

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var path: String? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            context.contentResolver.query(uri!!, projection, selection, selectionArgs, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndexOrThrow(column))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while saving the ringtone", e)
        }
        return path
    }

    fun ringtonesPath(context: Context): File {
        return File(context.filesDir, "ringtones")
    }

    /**
     * Get space left in a specific path
     *
     * @return -1L if an error occurred, size otherwise
     */
    fun getSpaceLeft(path: String): Long {
        return try {
            StatFs(path).availableBytes
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "getSpaceLeft: not able to access path on $path")
            -1L
        }
    }

    fun loadBitmap(context: Context, uriImage: Uri): Single<Bitmap> = Single.fromCallable<Bitmap> {
        val dbo = BitmapFactory.Options()
        dbo.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uriImage).use { `is` -> BitmapFactory.decodeStream(`is`, null, dbo) }
        val rotatedWidth: Int
        val rotatedHeight: Int
        val orientation = getOrientation(context, uriImage)
        if (orientation == ORIENTATION_LEFT || orientation == ORIENTATION_RIGHT) {
            rotatedWidth = dbo.outHeight
            rotatedHeight = dbo.outWidth
        } else {
            rotatedWidth = dbo.outWidth
            rotatedHeight = dbo.outHeight
        }
        var srcBitmap: Bitmap?
        context.contentResolver.openInputStream(uriImage).use { stream ->
            if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
                val widthRatio = rotatedWidth.toFloat() / MAX_IMAGE_DIMENSION.toFloat()
                val heightRatio = rotatedHeight.toFloat() / MAX_IMAGE_DIMENSION.toFloat()
                val maxRatio = max(widthRatio, heightRatio)

                // Create the bitmap from file
                val options = BitmapFactory.Options()
                options.inSampleSize = maxRatio.toInt()
                srcBitmap = BitmapFactory.decodeStream(stream, null, options)
            } else {
                srcBitmap = BitmapFactory.decodeStream(stream)
            }
        }
        if (orientation > 0) {
            val matrix = Matrix()
            matrix.postRotate(orientation.toFloat())
            srcBitmap = Bitmap.createBitmap(srcBitmap!!, 0, 0, srcBitmap!!.width,
                    srcBitmap!!.height, matrix, true)
        }
        srcBitmap!!
    }.subscribeOn(Schedulers.io())

    private fun getOrientation(context: Context, photoUri: Uri): Int {
        val resolver = context.contentResolver ?: return 0
        try {
            resolver.query(photoUri, arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)!!.use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(0)
            }
        } catch (e: Exception) {
            return when (getExifOrientation(resolver, photoUri)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    }

    private fun getExifOrientation(resolver: ContentResolver, photoUri: Uri): Int =
        try {
           resolver.openInputStream(photoUri)!!.use { input ->
               ExifInterface(input)
                       .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
           }
       } catch (e: Exception) {
           0
       }

    fun isImage(s: String): Boolean = getMimeType(s).startsWith("image")

    fun getFileName(s: String): String = s.split('/').last()

    fun shareFile(c: Context, path: File, displayName: String) {
        val fileUri: Uri = try {
            ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path, displayName)
        } catch (e: IllegalArgumentException) {
            Log.e("File Selector", "The selected file can't be shared: " + path.name)
            null
        } ?: return
        shareFile(c, fileUri, displayName)
    }

    fun shareFile(c: Context, uri: Uri, displayName: String? = null) {
        val name = displayName ?: uri.getQueryParameter("displayName") ?: ""
        try {
            c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, c.contentResolver.getType(uri.buildUpon().appendPath(name).build()))
                putExtra(Intent.EXTRA_STREAM, uri)
            }, null))
        } catch (e: Exception) {
            Log.w(TAG, "Error sharing file", e)
        }
    }

    fun openFile(c: Context, path: File, displayName: String) {
        var fileUri: Uri? = null
        try {
            fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path, displayName)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "The selected file can't be shared: " + path.name)
        }
        if (fileUri != null)
            openFile(c, fileUri, displayName)
    }

    fun openFile(c: Context, uri: Uri, displayName: String) {
        try {
            c.startActivity(Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, c.contentResolver.getType(uri.buildUpon().appendPath(displayName).build()))
                putExtra(Intent.EXTRA_STREAM, uri)
            })
        } catch (e: Exception) {
            Toast.makeText(c,
                c.getString(R.string.error_open_no_app_found, displayName), Toast.LENGTH_SHORT).show()
        }
    }

}
