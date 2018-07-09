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
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.util.LruCache;

import com.bumptech.glide.request.RequestOptions;

import cx.ring.R;
import cx.ring.model.Uri;
import cx.ring.utils.CircleTransform;
import cx.ring.utils.HashUtils;
import ezvcard.VCard;

public class AvatarFactory {

    private static final String TAG = AvatarFactory.class.getSimpleName();

    // ordered to have the same colors on all clients
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
    private static final int DEFAULT_AVATAR_SIZE = 128;

    private static final RequestOptions GLIDE_OPTIONS = new RequestOptions()
            .centerCrop()
            .error(R.drawable.ic_contact_picture_fallback);

    private static final RequestOptions GLIDE_OPTIONS_CIRCLE = new RequestOptions()
            .centerCrop()
            .error(R.drawable.ic_contact_picture_fallback)
            .transform(new CircleTransform());

    private static final Paint AVATAR_TEXT_PAINT = new Paint();
    static {
        AVATAR_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
        AVATAR_TEXT_PAINT.setColor(Color.WHITE);
        AVATAR_TEXT_PAINT.setAntiAlias(true);
    }

    private static final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    private static final LruCache<String, BitmapDrawable> mMemoryCache = new LruCache<String, BitmapDrawable>(maxMemory / 8) {
        @Override
        protected int sizeOf(String key, BitmapDrawable bitmap) {
            return bitmap.getBitmap() == null ? 0 : bitmap.getBitmap().getByteCount() / 1024;
        }
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

        return getAvatar(context, contactPhoto, username, ringId, pictureSize, false);
    }

    public static BitmapDrawable getAvatar(Context context, byte[] photo, String username, String ringId, boolean noCache) {
        return getAvatar(context, photo, username, ringId, Float.valueOf(context.getResources().getDisplayMetrics().density * DEFAULT_AVATAR_SIZE).intValue(), noCache);
    }

    public static BitmapDrawable getAvatar(Context context, byte[] photo, String username, String ringId) {
        return getAvatar(context, photo, username, ringId, Float.valueOf(context.getResources().getDisplayMetrics().density * DEFAULT_AVATAR_SIZE).intValue(), false);
    }

    public static BitmapDrawable getAvatar(Context context, byte[] photo, String username, String ringId, int pictureSize, boolean noCache) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }
        String key = ringId + pictureSize + username;
        BitmapDrawable bmp = noCache ? null : mMemoryCache.get(key);
        if (bmp != null)
            return bmp;

        Log.d(TAG, "getAvatar: username=" + username + ", ringid=" + ringId + ", pictureSize=" + pictureSize);

        if (photo != null && photo.length > 0) {
            bmp = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(photo, 0, photo.length));
            mMemoryCache.put(key, bmp);
            return bmp;
        }

        Uri uriUsername = new Uri(username);
        Uri uri = new Uri(ringId);
        Character firstCharacter = getFirstCharacter(uriUsername.getRawRingId());
        if (uri.isEmpty() || uriUsername.isRingId() || firstCharacter == null) {
            bmp = createDefaultAvatar(context, generateAvatarColor(uri.getRawUriString()), pictureSize);
            mMemoryCache.put(key, bmp);
            return bmp;
        }

        bmp = createLetterAvatar(context, firstCharacter, generateAvatarColor(uri.getRawUriString()), pictureSize);
        mMemoryCache.put(key, bmp);
        return bmp;
    }

    private static BitmapDrawable createDefaultAvatar(Context context, int backgroundColor, int pictureSize) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }

        Bitmap canvasBitmap = Bitmap.createBitmap(pictureSize, pictureSize, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(context.getResources().getColor(backgroundColor));

        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_contact_picture_box_default);
        if (drawable == null) {
            Log.e(TAG, "Not able to get default drawable");
        } else {
            drawable.setBounds(0, 0, pictureSize, pictureSize);
            drawable.draw(canvas);
        }

        return new BitmapDrawable(context.getResources(), canvasBitmap);
    }

    private static BitmapDrawable createLetterAvatar(Context context, char firstCharacter, int backgroundColor, int pictureSize) {
        if (context == null || pictureSize <= 0) {
            throw new IllegalArgumentException();
        }

        Bitmap canvasBitmap = Bitmap.createBitmap(pictureSize, pictureSize, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(context.getResources().getColor(backgroundColor));

        AVATAR_TEXT_PAINT.setTextSize(pictureSize / 2);
        canvas.drawText(Character.toString(firstCharacter), pictureSize * 0.51f, pictureSize * 0.7f, AVATAR_TEXT_PAINT);

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

    private static Character getFirstCharacter(String name) {
        if (name == null) {
            return null;
        }
        String filteredName = name.replaceAll("\\W+", "");
        if (filteredName.isEmpty()) {
            return null;
        }
        return Character.toUpperCase(name.charAt(0));
    }

    public static RequestOptions getGlideOptions(boolean circle, boolean withPlaceholder) {
        return circle ? GLIDE_OPTIONS_CIRCLE : GLIDE_OPTIONS;
    }

    public static void clearCache() {
        mMemoryCache.evictAll();
    }
}
