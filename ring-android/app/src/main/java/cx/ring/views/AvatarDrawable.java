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
import cx.ring.model.Conversation;
import cx.ring.services.VCardServiceImpl;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.HashUtils;
import io.reactivex.Single;

import android.graphics.drawable.VectorDrawable;
import android.text.TextUtils;
import android.util.TypedValue;

import java.util.List;

public class AvatarDrawable extends Drawable {
    private static final int SIZE_AB = 36;
    private static final float DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f;
    private static final int PLACEHOLDER_ICON = R.drawable.baseline_account_crop_24;
    private static final int CHECKED_ICON = R.drawable.baseline_check_circle_24;
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

    private static class PresenceIndicatorInfo { int cx, cy, radius; };
    private final PresenceIndicatorInfo presence = new PresenceIndicatorInfo();

    private boolean update = true;
    private int inSize = -1;

    private final int minSize;
    private Bitmap workspace;
    private Bitmap bitmap;
    private VectorDrawable placeholder;
    private VectorDrawable checkedIcon;
    private final RectF backgroundBounds = new RectF();
    private String avatarText;
    private float textStartXPoint;
    private float textStartYPoint;
    private int color;

    private final Paint clipPaint = new Paint();
    private final Paint textPaint = new Paint();
    private static final Paint drawPaint = new Paint();
    private final Paint presenceFillPaint;
    private final Paint presenceStrokePaint;
    private final Paint checkedPaint;
    static {
        drawPaint.setAntiAlias(true);
        drawPaint.setFilterBitmap(true);
    }

    private final boolean cropCircle;
    private boolean isOnline;
    private boolean isChecked;
    private boolean showPresence;

    public static class Builder {

        private Bitmap photo = null;
        private String name = null;
        private String id = null;
        private boolean circleCrop = false;
        private boolean isOnline = false;
        private boolean showPresence = true;
        private boolean isChecked = false;

        public Builder() {}

        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        public Builder withPhoto(Bitmap photo) {
            this.photo = photo;
            return this;
        }
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        public Builder withCircleCrop(boolean crop) {
            this.circleCrop = crop;
            return this;
        }
        public Builder withOnlineState(boolean isOnline) {
            this.isOnline = isOnline;
            return this;
        }
        public Builder withPresence(boolean showPresence) {
            this.showPresence = showPresence;
            return this;
        }
        public Builder withCheck(boolean checked) {
            this.isChecked = checked;
            return this;
        }

        public Builder withNameData(String profileName, String username) {
            return withName(TextUtils.isEmpty(profileName) ? username : profileName);
        }
        public Builder withContact(CallContact contact){
            return withPhoto((Bitmap)contact.getPhoto())
                    .withId(contact.getPrimaryNumber())
                    .withOnlineState(contact.isOnline())
                    .withNameData(contact.getProfileName(), contact.getUsername());
        }

        public Builder withContacts(List<CallContact> contacts) {
            if (contacts.size() == 1) {
                return withContact(contacts.get(0));
            } else {
                return withName("G");
            }
        }
        public Builder withConversation(Conversation conversation) {
            return withContacts(conversation.getContacts());
        }

        public AvatarDrawable build(Context context) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(
                    context, photo, name, id, circleCrop);
            avatarDrawable.setOnline(isOnline);
            avatarDrawable.setChecked(isChecked);
            avatarDrawable.showPresence = this.showPresence;
            return avatarDrawable;
        }

        public Single<AvatarDrawable> buildAsync(Context context) {
            return Single.fromCallable(() -> build(context));
        }
    }

    public static Single<AvatarDrawable> load(Context context, Account account, boolean crop) {
        return VCardServiceImpl.loadProfile(account)
                .map(data -> new Builder()
                        .withPhoto((Bitmap)data.second)
                        .withNameData(data.first, account.getRegisteredName())
                        .withId(account.getUri())
                        .withCircleCrop(crop)
                        .build(context));
    }
    public static Single<AvatarDrawable> load(Context context, Account account) {
        return load(context, account, true);
    }

    public void update(CallContact contact) {
        String profileName = contact.getProfileName();
        String username = contact.getUsername();
        avatarText = convertNameToAvatarText(
                TextUtils.isEmpty(profileName) ? username : profileName);
        bitmap = (Bitmap)contact.getPhoto();
        isOnline = contact.isOnline();
        update = true;
    }
    public void setName(String name) {
        avatarText = convertNameToAvatarText(name);
        update = true;
    }
    public void setPhoto(Bitmap photo) {
        bitmap = photo;
        update = true;
    }
    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    private AvatarDrawable(Context context, Bitmap photo, String name, String id, boolean crop) {
        cropCircle = crop;
        minSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, SIZE_AB, context.getResources().getDisplayMetrics());
        if (photo != null) {
            avatarText = null;
            bitmap = photo;
        } else {
            bitmap = null;
            avatarText = convertNameToAvatarText(name);
            color = ContextCompat.getColor(context, getAvatarColor(id));
            if (avatarText == null) {
                placeholder = (VectorDrawable) context.getDrawable(PLACEHOLDER_ICON);
            } else {
                textPaint.setColor(Color.WHITE);
                textPaint.setTypeface(Typeface.SANS_SERIF);
            }
        }
        presenceFillPaint = new Paint();
        presenceFillPaint.setColor(ContextCompat.getColor(context, PRESENCE_COLOR));
        presenceFillPaint.setStyle(Paint.Style.FILL);
        presenceFillPaint.setAntiAlias(true);

        presenceFillPaint.setColor(ContextCompat.getColor(context, PRESENCE_COLOR));
        presenceFillPaint.setStyle(Paint.Style.FILL);
        presenceFillPaint.setAntiAlias(true);

        presenceStrokePaint = new Paint();
        presenceStrokePaint.setColor(ContextCompat.getColor(context, DeviceUtils.isTv(context) ? R.color.grey_900 : R.color.background));
        presenceStrokePaint.setStyle(Paint.Style.STROKE);
        presenceStrokePaint.setAntiAlias(true);

        checkedIcon = (VectorDrawable) context.getDrawable(CHECKED_ICON);
        checkedIcon.setTint(ContextCompat.getColor(context, R.color.colorPrimary));
        checkedPaint = new Paint();
        checkedPaint.setColor(ContextCompat.getColor(context, R.color.background));
        checkedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        checkedPaint.setAntiAlias(true);

        clipPaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
    }

    public AvatarDrawable(AvatarDrawable other) {
        cropCircle = other.cropCircle;
        minSize = other.minSize;
        bitmap = other.bitmap;
        color = other.color;
        placeholder = other.placeholder;
        avatarText = other.avatarText;

        isOnline = other.isOnline;
        isChecked = other.isChecked;
        showPresence = other.showPresence;
        presenceFillPaint = other.presenceFillPaint;
        presenceStrokePaint = other.presenceStrokePaint;
        checkedPaint = other.checkedPaint;

        clipPaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
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
        if (showPresence && isOnline) {
            drawPresence(finalCanvas);
        }
        if (isChecked) {
            drawChecked(finalCanvas);
        }
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

    private void setupPresenceIndicator(Rect bounds) {
        presence.radius = (int) (0.29289321881 * (double) (bounds.width()) * 0.5);
        presence.cx = bounds.right - presence.radius;
        presence.cy = bounds.bottom - presence.radius;
        int presenceStrokeWidth = presence.radius / 3;
        presenceStrokePaint.setStrokeWidth(presenceStrokeWidth);
        checkedPaint.setStrokeWidth(presenceStrokeWidth);
        presence.radius -= presenceStrokeWidth * 0.5;

        if (checkedIcon != null)
            checkedIcon.setBounds(presence.cx - presence.radius, presence.cy - presence.radius, presence.cx + presence.radius,  presence.cy + presence.radius);
    }

    private void drawPresence(@NonNull Canvas canvas) {
        canvas.drawCircle(presence.cx, presence.cy, presence.radius - 1, presenceFillPaint);
        canvas.drawCircle(presence.cx, presence.cy, presence.radius, presenceStrokePaint);
    }
    private void drawChecked(@NonNull Canvas canvas) {
        if (checkedIcon != null) {
            canvas.drawCircle(presence.cx, presence.cy, presence.radius, checkedPaint);
            checkedIcon.draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        //if (showPresence)
        setupPresenceIndicator(bounds);
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
        } else {
            setAvatarTextValues(bounds);
        }
        if (cropCircle) {
            if (d > 0) {
                workspace = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
                clipPaint.setShader(new BitmapShader(workspace, BitmapShader.TileMode.CLAMP,
                                    BitmapShader.TileMode.CLAMP));
            } else {
                clipPaint.setShader(null);
                if (workspace != null) {
                    workspace.recycle();
                    workspace = null;
                }
            }
        } else {
            workspace = Bitmap.createBitmap(bounds.width(), bounds.height(),
                                            Bitmap.Config.ARGB_8888);
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

    private void setAvatarTextValues(Rect bounds) {
        if (avatarText != null) {
            textPaint.setTextSize(bounds.height() * DEFAULT_TEXT_SIZE_PERCENTAGE);
            float stringWidth = textPaint.measureText(avatarText);
            textStartXPoint = (bounds.width() / 2f) - (stringWidth / 2f);
            textStartYPoint = (bounds.height() / 2f) - ((textPaint.ascent() + textPaint.descent()) / 2f);
        }
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