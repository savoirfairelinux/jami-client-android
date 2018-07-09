/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AndroidFileUtils {

    private static final String TAG = AndroidFileUtils.class.getSimpleName();

    public static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            File fileTmp = new File(toPath);
            if (!fileTmp.exists()) {
                fileTmp.mkdirs();
            }
            Log.d(TAG, "Creating :" + toPath);
            boolean res = true;
            for (String file : files)
                if (file.contains("")) {
                    Log.d(TAG, "Copying file :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAsset(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                } else {
                    Log.d(TAG, "Copying folder :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAssetFolder(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                }
            return res;
        } catch (IOException e) {
            Log.e(TAG, "Error while copying asset folder", e);
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            FileUtils.copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error while copying asset", e);
            return false;
        }
    }

    public static String getRealPathFromURI(Context context, Uri uri) {
        String path = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                path = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            path = getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
    }

    public static String getFilename(Context context, Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        if (result.lastIndexOf('.') == -1) {
            String mimeType = getMimeType(context, uri);
            String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extensionFromMimeType != null) {
                result += '.' + extensionFromMimeType;
            }
        }
        return result;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            mimeType = getMimeType(uri.toString());
        }
        return mimeType;
    }

    public static String getMimeType(String filename) {
        String extension = filename.substring(filename.lastIndexOf("."));
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(extension);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
    }

    public static File getCacheFile(Context context, android.net.Uri uri) throws IOException {
        String filename = getFilename(context, uri);
        File file = new File(context.getCacheDir(), filename);
        FileOutputStream output = new FileOutputStream(file);
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        FileUtils.copyFile(inputStream, output);
        inputStream.close();
        output.close();
        return file;
    }

    public static File getConversationFile(Context context, android.net.Uri uri, String conversationId, String name) throws IOException {
        File file = getConversationPath(context, conversationId, name);
        FileOutputStream output = new FileOutputStream(file);
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        FileUtils.copyFile(inputStream, output);
        return file;
    }

    public static File getCachePath(Context context, String filename) {
        return new File(context.getCacheDir(), filename);
    }

    public static File getFilePath(Context context, String filename) {
        return context.getFileStreamPath(filename);
    }

    public static File getConversationPath(Context context, String conversationId, String name) {
        File conversationsDir = getFilePath(context, "conversation_data");

        if (!conversationsDir.exists())
            conversationsDir.mkdir();

        File conversationDir = new File(conversationsDir, conversationId);
        if (!conversationDir.exists())
            conversationDir.mkdir();

        return new File(conversationDir, name);
    }

    public static File getTempPath(Context context, String conversationId, String name) {
        File conversationsDir = getCachePath(context, "conversation_data");

        if (!conversationsDir.exists())
            conversationsDir.mkdir();

        File conversationDir = new File(conversationsDir, conversationId);
        if (!conversationDir.exists())
            conversationDir.mkdir();

        return new File(conversationDir, name);
    }

    public static String writeCacheFileToExtStorage(Context context, Uri cacheFile, String targetFilename) throws IOException {
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        int fileCount = 0;
        File finalFile = new File(downloadsDirectory, targetFilename);
        int lastDotIndex = targetFilename.lastIndexOf('.');
        String filename = targetFilename.substring(0, lastDotIndex);
        String extension = targetFilename.substring(lastDotIndex + 1);
        while (finalFile.exists()) {
            finalFile = new File(downloadsDirectory, filename + "_" + fileCount + '.' + extension);
            fileCount++;
        }

        Log.d(TAG, "writeCacheFileToExtStorage: finalFile=" + finalFile + ",exists=" + finalFile.exists());
        InputStream inputStream = context.getContentResolver().openInputStream(cacheFile);
        FileOutputStream output = new FileOutputStream(finalFile);
        FileUtils.copyFile(inputStream, output);
        return finalFile.toString();
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String path = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                path = cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while saving the ringtone", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return path;
    }

    public static String ringtonesPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "ringtones";
    }

    /**
     * Get space left in a specific path
     *
     * @return -1L if an error occurred, size otherwise
     */
    public static long getSpaceLeft(String path) {
        try {
            StatFs statfs = new StatFs(path);
            return statfs.getAvailableBytes();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getSpaceLeft: not able to access path on " + path);
            return -1L;
        }
    }
}
