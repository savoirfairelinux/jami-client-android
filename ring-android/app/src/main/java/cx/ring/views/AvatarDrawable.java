/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.services.VCardServiceImpl;
import cx.ring.utils.HashUtils;
import cx.ring.utils.Tuple;
import io.reactivex.Single;

import android.graphics.drawable.VectorDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

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

    private final boolean cropCircle;
    private boolean update = true;
    private int inSize = -1;

    private final int minSize;
    private Bitmap workspace;
    private final Bitmap bitmap;
    private VectorDrawable placeholder;
    private final RectF backgroundBounds = new RectF();
    private final String avatarText;
    private float textStartXPoint;
    private float textStartYPoint;
    private int color;

    private final Paint clipPaint = new Paint();
    private final Paint textPaint = new Paint();
    private static final Paint drawPaint = new Paint();
    static {
        drawPaint.setAntiAlias(true);
        drawPaint.setFilterBitmap(true);
    }

    public AvatarDrawable(Context context, CallContact contact) {
        this(context, (Bitmap)contact.getPhoto(), contact.getProfileName(), contact.getUsername(), contact.getPrimaryNumber(), true);
    }
    public AvatarDrawable(Context context, CallContact contact, boolean crop) {
        this(context, (Bitmap)contact.getPhoto(), contact.getProfileName(), contact.getUsername(), contact.getPrimaryNumber(), crop);
    }
    public AvatarDrawable(Context context, Tuple<String, Object> data, String registeredName, String uri, boolean crop) {
        this(context, (Bitmap)data.second, data.first, registeredName, uri, crop);
    }
    public AvatarDrawable(Context context, Tuple<String, Object> data, String registeredName, String uri) {
        this(context, (Bitmap)data.second, data.first, registeredName, uri, true);
    }
    public AvatarDrawable(Context context, Bitmap photo, String profileName, String username, String id, boolean crop) {
        this(context, photo, TextUtils.isEmpty(profileName) ? username : profileName, id, crop);
    }

    public static Single<AvatarDrawable> load(Context context, Account account, boolean crop) {
        return VCardServiceImpl.loadProfile(account)
                .map(data -> new AvatarDrawable(context, data, account.getRegisteredName(), account.getUri(), crop));
    }
    public static Single<AvatarDrawable> load(Context context, Account account) {
        return load(context, account, true);
    }

    public AvatarDrawable(Context context, Bitmap photo, String name, String id, boolean crop) {
        //Log.w("AvatarDrawable", photo + " " + name + " " + id);
        cropCircle = crop;
        Resources res = context.getResources();
        minSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SIZE_AB, res.getDisplayMetrics());
        clipPaint.setAntiAlias(true);
        if (photo != null) {
            avatarText = null;
            bitmap = photo;
        } else {
            bitmap = null;
            avatarText = convertNameToAvatarText(name);
            color = ContextCompat.getColor(context, getAvatarColor(id));
            if (avatarText == null) {
                placeholder = (VectorDrawable) context.getDrawable(PLACEHOLDER_ICON);
                placeholder.setColorFilter(context.getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
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
        if (update) {
            drawActual(new Canvas(workspace));
            update = false;
        }
        if (cropCircle) {
            int d = Math.min(getBounds().width(), getBounds().height());
            int r = d / 2;
            finalCanvas.drawCircle(getBounds().centerX(), getBounds().centerY(), r, clipPaint);
        } else {
            finalCanvas.drawBitmap(workspace, null, getBounds(), drawPaint);
        }
    }

    private void drawActual(@NonNull Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, backgroundBounds, drawPaint);
        } else if (placeholder == null) {
            canvas.drawColor(color);
            canvas.drawText(avatarText, textStartXPoint, textStartYPoint, textPaint);
        } else {
            canvas.drawColor(color);
            placeholder.setTint(Color.WHITE);
            placeholder.draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        //Log.w("AvatarDrawable", this + "onBoundsChange " + bounds.width() + " " + bounds.height());
        setAvatarTextValues();
        int d = Math.min(bounds.width(), bounds.height());
        if (placeholder != null) {
            int cx = (bounds.width()-d)/2;
            int cy = (bounds.height()-d)/2;
            placeholder.setBounds(cx, cy, cx + d, cy + d);
        }
        if (bitmap != null) {
            int iw = cropCircle ? d : bounds.width();
            int ih = cropCircle ? d : bounds.height();
            int a = bitmap.getWidth() * ih;
            int b = bitmap.getHeight() * iw;
            int w;
            int h;
            if (a < b) {
                w = iw;
                h  = (iw * bitmap.getHeight())/bitmap.getWidth();
            } else {
                w  = (ih * bitmap.getWidth())/bitmap.getHeight();
                h = ih;
            }
            int cx = (iw - w)/2;
            int cy = (ih - h)/2;
            backgroundBounds.set(cx, cy, cx + w, h + cy);
        }
        if (cropCircle) {
            if (d > 0) {
                workspace = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
                clipPaint.setShader(new BitmapShader(workspace, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            } else {
                clipPaint.setShader(null);
                if (workspace != null) {
                    workspace.recycle();
                    workspace = null;
                }
            }
        } else {
            workspace = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
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
        return minSize;
    }

    @Override
    public int getMinimumHeight() {
        return minSize;
    }

    public void setInSize(int s) {
        inSize = s;
    }

    @Override
    public int getIntrinsicWidth() {
        return inSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return inSize;
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

    private static int getAvatarColor(String id) {
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