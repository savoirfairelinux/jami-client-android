/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class AndroidFileUtils {

    private static final String TAG = AndroidFileUtils.class.getSimpleName();
    private final static int ORIENTATION_LEFT = 270;
    private final static int ORIENTATION_RIGHT = 90;
    private final static int MAX_IMAGE_DIMENSION = 1024;

    public static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, File toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            if (!toPath.exists()) {
                toPath.mkdirs();
            }
            boolean res = true;
            for (String file : files) {
                File newFile = new File(toPath, file);
                if (file.contains("")) {
                    Log.d(TAG, "Copying file :" + fromAssetPath + File.separator + file + " to " + newFile);
                    res &= copyAsset(assetManager, fromAssetPath + File.separator + file, newFile);
                } else {
                    Log.d(TAG, "Copying folder :" + fromAssetPath + File.separator + file + " to " + newFile);
                    res &= copyAssetFolder(assetManager, fromAssetPath + File.separator + file, newFile);
                }
            }
            return res;
        } catch (IOException e) {
            Log.e(TAG, "Error while copying asset folder", e);
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, File toPath) {
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(fromAssetPath);
            toPath.createNewFile();
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
        if (DocumentsContract.isDocumentUri(context, uri)) {
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

    public static String getFilename(ContentResolver cr, Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = cr.query(uri, null, null, null, null)) {
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
            String mimeType = getMimeType(cr, uri);
            String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extensionFromMimeType != null) {
                result += '.' + extensionFromMimeType;
            }
        }
        return result;
    }

    public static String getMimeType(ContentResolver cr, Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = cr.getType(uri);
        } else {
            mimeType = getMimeType(uri.toString());
        }
        return mimeType;
    }

    public static String getMimeType(String filename) {
        int pos = filename.lastIndexOf(".");
        String fileExtension = null;
        if (pos >= 0) {
            fileExtension = MimeTypeMap.getFileExtensionFromUrl(filename.substring(pos));
            return getMimeTypeFromExtension(fileExtension);
        }
        return getMimeTypeFromExtension(fileExtension);
    }

    public static String getMimeTypeFromExtension(String ext) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            if (!TextUtils.isEmpty(mimeType))
                return mimeType;
            if (ext.contentEquals("gz")) {
                return "application/gzip";
            }
        return "application/octet-stream";
    }

    public static File createImageFile(@NonNull Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "img_" + timeStamp + "_";

        // Save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(imageFileName, ".jpg", context.getExternalCacheDir());
    }

    public static @NonNull Single<File> getCacheFile(@NonNull Context context, @NonNull Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        File cacheDir = context.getCacheDir();
        return Single.fromCallable(() -> {
            File file = new File(cacheDir, getFilename(contentResolver, uri));
            FileOutputStream output = new FileOutputStream(file);
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null)
                throw new FileNotFoundException();
            FileUtils.copyFile(inputStream, output);
            inputStream.close();
            output.flush();
            output.close();
            return file;
        }).subscribeOn(Schedulers.io());
    }

    public static Completable moveToUri(@NonNull ContentResolver cr, @NonNull File input, @NonNull Uri outUri) {
        return Completable.fromAction(() -> {
            InputStream inputStream = null;
            OutputStream output = null;
            try {
                inputStream = new FileInputStream(input);
                output = cr.openOutputStream(outUri);
                FileUtils.copyFile(inputStream, output);
                input.delete();
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (output != null)
                        output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Copies a file to a predefined Uri destination
     * Uses the underlying copyFile(InputStream,OutputStream)
     * @param cr content resolver
     * @param input the file we want to copy
     * @param outUri the uri destination
     * @return success value
     */
    public static Completable copyFileToUri(ContentResolver cr, File input, Uri outUri){
        return Completable.fromAction(() -> {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(input);
                outputStream = cr.openOutputStream(outUri);
                FileUtils.copyFile(inputStream, outputStream);
            } finally {
                if (outputStream != null)
                    outputStream.close();
                if (inputStream != null)
                    inputStream.close();
            }
        }).subscribeOn(Schedulers.io());
    }

    public static File getConversationFile(Context context, Uri uri, String conversationId, String name) throws IOException {
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
        String path = null;
        final String column = "_data";
        final String[] projection = {column};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                path = cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while saving the ringtone", e);
        }
        return path;
    }

    public static File ringtonesPath(Context context) {
        return new File(context.getFilesDir(), "ringtones");
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

    public static Single<Bitmap> loadBitmap(Context context, Uri uriImage) {
        return Single.fromCallable(() -> {
            InputStream is = context.getContentResolver().openInputStream(uriImage);
            BitmapFactory.Options dbo = new BitmapFactory.Options();
            dbo.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, dbo);
            is.close();

            int rotatedWidth, rotatedHeight;
            int orientation = getOrientation(context, uriImage);

            if (orientation == ORIENTATION_LEFT || orientation == ORIENTATION_RIGHT) {
                rotatedWidth = dbo.outHeight;
                rotatedHeight = dbo.outWidth;
            } else {
                rotatedWidth = dbo.outWidth;
                rotatedHeight = dbo.outHeight;
            }

            Bitmap srcBitmap;
            is = context.getContentResolver().openInputStream(uriImage);
            if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
                float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
                float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
                float maxRatio = Math.max(widthRatio, heightRatio);

                // Create the bitmap from file
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = (int) maxRatio;
                srcBitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                srcBitmap = BitmapFactory.decodeStream(is);
            }
            is.close();

            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                        srcBitmap.getHeight(), matrix, true);
            }
            return srcBitmap;
        }).subscribeOn(Schedulers.io());
    }

    public static int getOrientation(@NonNull Context context, Uri photoUri) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null)
            return 0;
        try (Cursor cursor = resolver.query(photoUri, new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch (Exception e) {
            switch (getExifOrientation(resolver, photoUri)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private static int getExifOrientation(@NonNull ContentResolver resolver, Uri photoUri) {
        if (Build.VERSION.SDK_INT > 23) {
            try (InputStream input = resolver.openInputStream(photoUri)) {
                return new ExifInterface(input)
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (Exception e) {
                return 0;
            }
        } else {
            try {
                return new ExifInterface(photoUri.getPath())
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (Exception e) {
                return 0;
            }
        }
    }

}
