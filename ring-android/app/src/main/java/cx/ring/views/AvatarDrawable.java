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
import cx.ring.utils.Log;
import io.reactivex.Single;

import android.graphics.drawable.VectorDrawable;
import android.text.TextUtils;
import android.util.TypedValue;

public class AvatarDrawable extends Drawable {
    private static final int SIZE_AB = 36;
    private static final float DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f;
    private static final int PLACEHOLDER_ICON = R.drawable.baseline_account_circle_24;
    private static final int PRESENCE_COLOR = R.color.green_A700;

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

    private boolean update = true;
    private int inSize = -1;

    private final int minSize;
    private Bitmap workspace;
    private Bitmap bitmap;
    private VectorDrawable placeholder;
    private final RectF backgroundBounds = new RectF();
    private String avatarText;
    private float textStartXPoint;
    private float textStartYPoint;
    private int color;
    private int presenceColor;
    private int backgroundColor;

    private final Paint clipPaint = new Paint();
    private final Paint textPaint = new Paint();
    private static final Paint drawPaint = new Paint();
    static {
        drawPaint.setAntiAlias(true);
        drawPaint.setFilterBitmap(true);
    }

    static int numAvatars = 0;

    private final boolean cropCircle;
    private boolean isOnline;

    public static class Builder {

        private Bitmap photo = null;
        private String name = null;
        private String id = null;
        private boolean crop = false;
        private boolean isOnline = false;

        public Builder() {}

        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        public Builder withPhoto(Bitmap photo){
            this.photo = photo;
            return this;
        }
        public Builder withName(String name){
            this.name = name;
            return this;
        }
        public Builder doCrop(boolean crop){
            this.crop = crop;
            return this;
        }
        public Builder withOnlineState(boolean isOnline){
            this.isOnline = isOnline;
            return this;
        }

        public Builder withNames(String profileName, String username){
            withName(TextUtils.isEmpty(profileName) ? username : profileName);
            return this;
        }
        public Builder withContact(CallContact contact){
            withPhoto((Bitmap)contact.getPhoto());
            withId(contact.getPrimaryNumber());
            withOnlineState(contact.isOnline());
            withNames(contact.getProfileName(), contact.getUsername());
            return this;
        }

        public AvatarDrawable build(Context context){
            AvatarDrawable avatarDrawable = new AvatarDrawable(
                    context, photo, name, id, crop);
            avatarDrawable.setOnline(isOnline);
            return avatarDrawable;
        }
    }

    public static Single<AvatarDrawable> load(Context context, Account account, boolean crop) {
        return VCardServiceImpl.loadProfile(account)
                .map(data -> {
                    return new AvatarDrawable.Builder()
                            .withPhoto((Bitmap)data.second)
                            .withNames(data.first, account.getRegisteredName())
                            .withId(account.getUri())
                            .doCrop(crop)
                            .build(context);
                });
    }
    public static Single<AvatarDrawable> load(Context context, Account account) {
        return load(context, account, true);
    }

    public void setName(String name) {
        avatarText = convertNameToAvatarText(name);
        update = true;
    }
    public void setPhoto(Bitmap photo) {
        bitmap = photo;
        update = true;
    }
    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    private AvatarDrawable(Context context, Bitmap photo, String name, String id, boolean crop) {
        cropCircle = crop;
        Resources res = context.getResources();
        minSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, SIZE_AB, res.getDisplayMetrics());
        clipPaint.setAntiAlias(true);
        placeholder = (VectorDrawable) context.getDrawable(PLACEHOLDER_ICON);
        color = ContextCompat.getColor(context, getAvatarColor(id));
        avatarText = convertNameToAvatarText(name);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        bitmap = photo;
        presenceColor = ContextCompat.getColor(context, PRESENCE_COLOR);
        backgroundColor = ContextCompat.getColor(context, R.color.background);

        numAvatars++;
        Log.d("AvatarDrawable", "numAvatars++: " + numAvatars);
    }

    protected void finalize () throws Throwable {
        numAvatars--;
        Log.d("AvatarDrawable", "numAvatars--: " + numAvatars);
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
            if (isOnline) {
                drawPresence(finalCanvas);
            }
        } else {
            finalCanvas.drawBitmap(workspace, null, getBounds(), drawPaint);
        }
    }

    private void drawPresence(@NonNull Canvas canvas) {
        int oldColor = drawPaint.getColor();
        Paint.Style oldStyle = drawPaint.getStyle();

        Rect avatarBounds = getBounds();
        int radius = (int) (0.29289321881 * (double) (avatarBounds.width()) * 0.5);
        int cx = avatarBounds.right - radius;
        int cy = avatarBounds.bottom - radius;
        int presenceStrokeWidth = radius / 3;
        radius -= presenceStrokeWidth * 0.5;

        drawPaint.setColor(presenceColor);
        drawPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius - 1, drawPaint);

        drawPaint.setColor(backgroundColor);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(presenceStrokeWidth);
        canvas.drawCircle(cx, cy, radius, drawPaint);

        drawPaint.setColor(oldColor);
        drawPaint.setStyle(oldStyle);

        Log.d("AvatarDrawable", "draw bounds: " + getBounds() + " r: " + radius + " sw: " + presenceStrokeWidth);
    }

    private void drawActual(@NonNull Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, backgroundBounds, drawPaint);
        } else {
            canvas.drawColor(color);
            if (avatarText != null) {
                canvas.drawText(avatarText, textStartXPoint, textStartYPoint, textPaint);
            } else {
                placeholder.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                placeholder.draw(canvas);
            }
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        setAvatarTextValues();
        int d = Math.min(bounds.width(), bounds.height());
        if (avatarText == null) {
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
        if (avatarText == null) {
            placeholder.setAlpha(alpha);
        } else {
            textPaint.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (avatarText == null) {
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
            Log.d("AvatarDrawable", "setAvatarTextValues");
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
        if (TextUtils.isEmpty(name)) {
            return null;
        } else {
            return new String(Character.toChars(name.codePointAt(0))).toUpperCase();
        }
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