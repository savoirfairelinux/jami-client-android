/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import cx.ring.R;
import cx.ring.utils.HashUtils;

import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.text.TextUtils;
import android.util.Log;

public class AvatarDrawable extends Drawable {
    private static final int SIZE_AB = 36;
    private static final float DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f;
    private static final int PLACEHOLDER_ICON = R.drawable.baseline_account_circle_24;

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

    private Bitmap workspace;
    private final Paint clipPaint = new Paint();
    private final int size;

    private final Bitmap bitmap;
    private final Rect bitmapBounds;
    private VectorDrawableCompat placeholder;
    private final Paint textPaint = new Paint();
    private final RectF backgroundBounds = new RectF();
    private final String avatarText;
    private float textStartXPoint;
    private float textStartYPoint;
    private int color;

    private boolean update =true;

    public AvatarDrawable(Context context, byte[] photo, String name, String id) {
        Resources res = context.getResources();
        size = (int) (SIZE_AB * res.getDisplayMetrics().density);
        clipPaint.setAntiAlias(true);
        if (photo != null) {
            avatarText = null;
            bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeByteArray(photo, 0, photo.length), 420, 420);
            bitmapBounds = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        } else {
            bitmap = null;
            bitmapBounds = null;
            avatarText = convertNameToAvatarText(name);
            color = res.getColor(generateAvatarColor(id));
            if (avatarText == null) {
                placeholder = VectorDrawableCompat.create(context.getResources(), PLACEHOLDER_ICON, context.getTheme());
            } else {
                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.SANS_SERIF);
            }
        }
        textPaint.setAntiAlias(true);
    }

    @Override
    public void draw(@NonNull Canvas finalCanvas) {
        if (workspace == null)
            return;
        //Log.w("AvatarDrawable", this + "draw " + getBounds().width() + " " + getBounds().height());
        if (update) {
            Canvas canvas = new Canvas(workspace);
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, bitmapBounds, backgroundBounds, textPaint);
            } else if (placeholder == null) {
                canvas.drawColor(color);
                canvas.drawText(avatarText, textStartXPoint, textStartYPoint, textPaint);
            } else {
                canvas.drawColor(0xffffffff);
                canvas.save();
                canvas.scale(1.2f, 1.2f, getBounds().centerX(), getBounds().centerY());
                placeholder.setTint(color);
                placeholder.draw(canvas);
                canvas.restore();
            }
            update = false;
        }
        int d = Math.min(getBounds().width(), getBounds().height());
        int r = d/2;
        finalCanvas.drawCircle(getBounds().centerX(), getBounds().centerY(), r, clipPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Log.w("AvatarDrawable", this + "onBoundsChange " + bounds.width() + " " + bounds.height());
        setAvatarTextValues();
        if (placeholder != null)
            placeholder.setBounds(bounds);
        int d = Math.min(bounds.width(), bounds.height());
        int r = d/2;
        int cx = bounds.centerX();
        int cy = bounds.centerY();
        backgroundBounds.set(cx - r, cy  - r, cx + r, cy  + r);
        if (d > 0) {
            workspace = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            clipPaint.setShader(new BitmapShader(workspace, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        } else {
            clipPaint.setShader(null);
            workspace.recycle();
            workspace = null;
        }
        update = true;
    }

    @Override
    public void setAlpha(int alpha) {
        if (placeholder != null) {
            placeholder.setAlpha(alpha);
        } else {
            textPaint.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (placeholder != null) {
            placeholder.setColorFilter(colorFilter);
        } else {
            textPaint.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getMinimumWidth() {
        return size;
    }

    @Override
    public int getMinimumHeight() {
        return size;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private void setAvatarTextValues() {
        if (avatarText != null) {
            textPaint.setTextSize(getBounds().height() * DEFAULT_TEXT_SIZE_PERCENTAGE);
            textStartXPoint = calculateTextStartXPoint();
            textStartYPoint = calculateTextStartYPoint();
        }
    }

    private float calculateTextStartXPoint() {
        float stringWidth = textPaint.measureText(avatarText);
        return (getBounds().width() / 2f) - (stringWidth / 2f);
    }

    private float calculateTextStartYPoint() {
        return (getBounds().height() / 2f) - ((textPaint.ascent() + textPaint.descent()) / 2f);
    }

    private String convertNameToAvatarText(String name) {
        return TextUtils.isEmpty(name) ? null : name.substring(0, 1).toUpperCase();
    }

    private static int generateAvatarColor(String id) {
        if (id == null) {
            return R.color.grey_500;
        }

        String md5 = HashUtils.md5(id);
        if (md5 == null) {
            return R.color.grey_500;
        }
        int colorIndex = Integer.parseInt(md5.charAt(0) + "", 16);
        return contactColors[colorIndex % contactColors.length];
    }
}