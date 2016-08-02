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
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class CropImageUtils {
    @Nullable
    public static Bitmap cropImageToCircle(@NonNull Bitmap image) {
        int w = image.getWidth(), h = image.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        final Bitmap externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

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
}
