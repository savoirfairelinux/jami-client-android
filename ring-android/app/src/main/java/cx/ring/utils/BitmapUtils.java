/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Helper calls to manipulates Bitmaps
 */
public final class BitmapUtils {

    private static final String TAG = BitmapUtils.class.getName();

    private BitmapUtils() {
    }



    @Nullable
    public static Bitmap cropImageToCircle(@NonNull byte[] bArray) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        if (bitmap != null) {
            return cropImageToCircle(bitmap);
        }

        return null;
    }

    public static Bitmap cropImageToCircle(@NonNull Bitmap image) {
        int side = Math.min(image.getWidth(), image.getHeight());

        final Bitmap externalBMP = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);

        BitmapShader shader;
        shader = new BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);

        Paint paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setDither(true);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setColor(Color.WHITE);
        internalCanvas.drawOval(
                new RectF(0, 0, externalBMP.getWidth(), externalBMP.getHeight()),
                paint);

        return externalBMP;
    }

    public static byte[] bitmapToBytes(Bitmap bmp) {
        int bytes = bmp.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        bmp.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
        return buffer.array();
    }

    public static Bitmap bytesToBitmap(byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        }
        return null;
    }

    public static Bitmap bytesToBitmap(byte[] data, int maxSize) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static Bitmap reduceBitmap(Bitmap bmp, int size) {
        if (bmp.getByteCount() <= size)
            return bmp;
        Log.d(TAG, "reduceBitmap: bitmap size before reduce " + bmp.getByteCount());
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        int minRatio = bmp.getByteCount()/size;

        int ratio = 2;
        while (ratio*ratio < minRatio)
            ratio *= 2;

        height /= ratio;
        width /= ratio;
        bmp = Bitmap.createScaledBitmap(bmp, width, height, true);

        Log.d(TAG, "reduceBitmap: bitmap size after x" + ratio + " reduce " + bmp.getByteCount());
        return bmp;
    }

    public static Bitmap createScaledBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null || maxSize < 0) {
            throw new IllegalArgumentException();
        }
        int width = bitmap.getHeight();
        int height = bitmap.getWidth();
        if (width != height) {
            if (width < height) {
                // portrait
                height = maxSize;
                width = (maxSize * bitmap.getWidth()) / bitmap.getHeight();
            } else {
                // landscape
                height = (maxSize * bitmap.getHeight()) / bitmap.getWidth();
                width = maxSize;
            }
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException();
        }

        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


}
