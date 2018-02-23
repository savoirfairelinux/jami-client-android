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
import android.util.Log;

import cx.ring.R;
import cx.ring.model.Uri;
import cx.ring.utils.HashUtils;
import ezvcard.VCard;

public class AvatarFactory {

    private static final String TAG = AvatarFactory.class.getSimpleName();

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

    private AvatarFactory() {
    }

    public static Drawable getAvatar(Context context, VCard vcard, String username, String ringId) {
        return getAvatar(context, vcard, username, ringId, Float.valueOf(context.getResources().getDisplayMetrics().density * 128).intValue());
    }

    public static Drawable getAvatar(Context context, VCard vcard, String username, String ringId, int pictureSize) {
        if (vcard == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] contactPhoto = null;
        if (vcard.getPhotos() != null && !vcard.getPhotos().isEmpty()) {
            contactPhoto = vcard.getPhotos().get(0).getData();
        }

        return getAvatar(context, contactPhoto, username, ringId, pictureSize);
    }

    public static Drawable getAvatar(Context context, byte[] photo, String username, String ringId) {
        return getAvatar(context, photo, username, ringId, Float.valueOf(context.getResources().getDisplayMetrics().density * 128).intValue());
    }

    public static Drawable getAvatar(Context context, byte[] photo, String username, String ringId, int pictureSize) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }
        Log.d(TAG, "getAvatar: username=" + username + ", ringid=" + ringId + ", pictureSize=" + pictureSize);

        if (photo != null && photo.length > 0) {
            return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(photo, 0, photo.length));
        }

        Uri uriUsername = new Uri(username);
        Uri uri = new Uri(ringId);
        if (uri.isEmpty() || uriUsername.isRingId()) {
            return createDefaultAvatar(context, generateAvatarColor(uri.getRawUriString()), pictureSize);
        }

        return createLetterAvatar(context, getFirstCharacter(uriUsername.getRawRingId()), generateAvatarColor(uri.getRawUriString()), pictureSize);
    }

    private static Drawable createDefaultAvatar(Context context, int backgroundColor, int pictureSize) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }

        Drawable backgroundDrawable = new ColorDrawable(Color.TRANSPARENT);
        Bitmap canvasBitmap = Bitmap.createBitmap(pictureSize, pictureSize, Bitmap.Config.ARGB_8888);

        Canvas backgroundCanvas = new Canvas(canvasBitmap);
        backgroundDrawable.draw(backgroundCanvas);
        int conversationColorLight = context.getResources().getColor(backgroundColor);
        backgroundCanvas.drawColor(conversationColorLight);

        Canvas avatarCanvas = new Canvas(canvasBitmap);
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_contact_picture_box_default);
        if (drawable == null) {
            Log.e(TAG, "Not able to get default drawable");
        } else {
            drawable.setBounds(0, 0, pictureSize, pictureSize);
            drawable.draw(avatarCanvas);
        }

        return new BitmapDrawable(context.getResources(), canvasBitmap);
    }

    private static Drawable createLetterAvatar(Context context, char firstCharacter, int backgroundColor, int pictureSize) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }

        Drawable backgroundDrawable = new ColorDrawable(Color.TRANSPARENT);
        Bitmap canvasBitmap = Bitmap.createBitmap(pictureSize, pictureSize, Bitmap.Config.ARGB_8888);

        Canvas backgroundCanvas = new Canvas(canvasBitmap);
        backgroundDrawable.draw(backgroundCanvas);
        int conversationColorLight = context.getResources().getColor(backgroundColor);
        backgroundCanvas.drawColor(conversationColorLight);

        Canvas letterCanvas = new Canvas(canvasBitmap);
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(pictureSize / 2);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        letterCanvas.drawText(Character.toString(firstCharacter), pictureSize * 0.51f, pictureSize * 0.7f, paint);

        return new BitmapDrawable(context.getResources(), canvasBitmap);
    }

    private static int generateAvatarColor(String ringId) {
        if (ringId == null) {
            return R.color.grey_500;
        }

        String md5 = HashUtils.md5(ringId);
        if (md5 == null) {
            return R.color.grey_500;
        }
        int colorIndex = Integer.parseInt(md5.charAt(0) + "", 16);
        Log.d(TAG, "generateAvatarColor: ringid=" + ringId + ", index=" + colorIndex + ", md5=" + md5);
        return contactColors[colorIndex % contactColors.length];
    }

    private static char getFirstCharacter(String name) {
        if (name == null || name.isEmpty()) {
            return '#';
        } else {
            return Character.toUpperCase(name.charAt(0));
        }
    }
}
