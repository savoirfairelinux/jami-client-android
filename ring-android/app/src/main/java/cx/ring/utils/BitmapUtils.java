/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * Helper calls to manipulates Bitmaps
 */
public class BitmapUtils {

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

    @Nullable
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

    public static byte[] stringToBytes(String text) {
        String[] byteValues = text.substring(1, text.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];

        for (int i = 0, len = bytes.length; i < len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }
        return bytes;
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

    public static Bitmap reduceBitmap(Bitmap bmp, int size) {
        Log.d(TAG, "bitmap size before reduce " + bmp.getByteCount());
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        while (bmp.getByteCount() > size) {
            height /= 2;
            width /= 2;
            bmp = Bitmap.createScaledBitmap(bmp, width, height, false);
        }

        Log.d(TAG, "bitmap size after reduce " + bmp.getByteCount());
        return bmp;
    }
}