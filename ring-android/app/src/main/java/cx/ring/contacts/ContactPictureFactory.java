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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import cx.ring.R;
import cx.ring.utils.HashUtils;

public class ContactPictureFactory {

    private static final int[] contactColors = {
            R.color.red_500, R.color.pink_500,
            R.color.purple_500, R.color.deep_purple_500,
            R.color.indigo_500, R.color.blue_500,
            R.color.cyan_500, R.color.teal_500,
            R.color.green_500, R.color.light_green_500,
            R.color.grey_500, R.color.lime_500,
            R.color.amber_500, R.color.deep_orange_500,
            R.color.brown_500, R.color.blue_grey_500
    };

    private ContactPictureFactory() {
    }

    public static Drawable getContactPicture(Context context, byte[] photo, String username, String uuid) {
        return getContactPicture(context, photo, username, uuid, Float.valueOf(context.getResources().getDisplayMetrics().density * 128).intValue());
    }

    public static Drawable getContactPicture(Context context, byte[] photo, String username, String uuid, int pictureSize) {
        if (context == null || uuid == null) {
            throw new IllegalArgumentException();
        }

        Drawable result;
        if (photo != null && photo.length > 0) {
            result = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(photo, 0, photo.length));
        } else if (username != null) {
            // Create a default picture with a letter
            result = createLetterAvatar(context, username, pictureSize, pictureSize);
        } else {
            result = ContextCompat.getDrawable(context, R.drawable.ic_contact_picture);
        }
        return result;
    }

    private static Drawable createLetterAvatar(Context context, String text, int width, int height) {
        if (context == null || text == null || text.isEmpty() || width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }

        Drawable backgroundDrawable = new ColorDrawable(Color.TRANSPARENT);
        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas backgroundCanvas = new Canvas(canvasBitmap);
        backgroundDrawable.draw(backgroundCanvas);
        int conversationColorLight = context.getResources().getColor(generateColor(text));
        backgroundCanvas.drawColor(conversationColorLight);

        Drawable letterDrawable = new ColorDrawable(Color.TRANSPARENT);
        Canvas letterCanvas = new Canvas(canvasBitmap);
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(height / 2);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        letterDrawable.draw(letterCanvas);
        String character = getFirstCharacter(text);
        letterCanvas.drawText(character, width * 0.51f, height * 0.7f, paint);

        return new BitmapDrawable(context.getResources(), canvasBitmap);
    }

    public static int generateColor(String name) {
        if (name == null) {
            return R.color.grey_500;
        }

        String md5 = HashUtils.md5(name);
        if (md5 == null) {
            return R.color.grey_500;
        }
        int colorIndex = Integer.parseInt(md5.charAt(0) + "", 16);
        return contactColors[colorIndex % contactColors.length];
    }

    private static String getFirstCharacter(String name) {
        if (name == null || name.isEmpty()) {
            return "#";
        } else {
            return String.valueOf(name.charAt(0)).toUpperCase();
        }
    }
}
