/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.contacts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;

import cx.ring.R;

public class ContactPictureFactory {

    private ContactPictureFactory() {
    }

    public static Drawable getContactPicture(Context context, byte[] photo, String username) {
        if (context == null || username == null) {
            throw new IllegalArgumentException();
        }
        Drawable result;
        if (photo != null) {
            result = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(photo, 0, photo.length));
        } else {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_account_box);
            result = createMarkerIcon(context, drawable, getCharacter(username), 128, 128);
        }
        return result;
    }

    private static String getCharacter(String name) {
        String cleanedName = name.replaceFirst("[^\\p{L}\\p{Nd}\\p{P}\\p{S}]+", "");

        if (cleanedName.isEmpty()) {
            return "#";
        } else {
            return String.valueOf(cleanedName.charAt(0));
        }
    }

    private static Drawable createMarkerIcon(Context context, Drawable backgroundImage, String text, int width, int height) {
        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Create a canvas, that will draw on to canvasBitmap.
        Canvas imageCanvas = new Canvas(canvasBitmap);

        // Set up the paint for use with our Canvas
        Paint imagePaint = new Paint();
        imagePaint.setTextAlign(Paint.Align.CENTER);
        imagePaint.setTextSize(16f);

        // Draw the image to our canvas
        backgroundImage.draw(imageCanvas);

        // Draw the text on top of our image
        imageCanvas.drawText(text, width / 2, height / 2, imagePaint);

        // Combine background and text to a LayerDrawable
        return new LayerDrawable(new Drawable[]{backgroundImage, new BitmapDrawable(context.getResources(), canvasBitmap)});
    }
}
