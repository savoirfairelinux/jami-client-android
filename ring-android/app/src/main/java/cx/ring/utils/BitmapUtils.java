/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import androidx.annotation.NonNull;

import net.jami.utils.Log;

import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Helper calls to manipulates Bitmaps
 */
public final class BitmapUtils
{
    private static final String TAG = BitmapUtils.class.getSimpleName();
    private BitmapUtils() {}

    public static Photo bitmapToPhoto(@NonNull Bitmap image) {
        return new Photo(bitmapToPng(image), ImageType.PNG);
    }

    public static byte[] bitmapToPng(@NonNull Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] bitmapToBytes(Bitmap bmp) {
        int bytes = bmp.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        bmp.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
        return buffer.array();
    }

    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null)
            return null;
        try {
            return bytesToBitmap(Base64.decode(base64, Base64.DEFAULT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        int width = options.outWidth;
        int height = options.outHeight;
        int scale = 1;
        while (3 * width * height > maxSize) {
            scale *= 2;
            width /= 2;
            height /= 2;
        }
        options.inSampleSize = scale;
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

        net.jami.utils.Log.d(TAG, "reduceBitmap: bitmap size after x" + ratio + " reduce " + bmp.getByteCount());
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
        } else {
            width = maxSize;
            height = maxSize;
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        return drawableToBitmap(drawable, -1);
    }
    public static Bitmap drawableToBitmap(Drawable drawable, int size) {
        return drawableToBitmap(drawable, size, 0);
    }
    public static Bitmap drawableToBitmap(Drawable drawable, int size, int padding) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : size;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : size;

        Bitmap bitmap = Bitmap.createBitmap(width + 2*padding, height + 2*padding, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(padding, padding, canvas.getWidth()-padding, canvas.getHeight()-padding);
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
