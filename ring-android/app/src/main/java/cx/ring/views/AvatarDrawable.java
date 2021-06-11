/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.smartlist.SmartListViewModel;
import net.jami.utils.HashUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cx.ring.R;
import cx.ring.services.VCardServiceImpl;
import cx.ring.utils.DeviceUtils;
import io.reactivex.rxjava3.core.Single;

public class AvatarDrawable extends Drawable {
    private static final String TAG = AvatarDrawable.class.getSimpleName();

    private static final int SIZE_AB = 36;
    private static final float SIZE_BORDER = 2f;

    private static final float DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f;
    private static final int PLACEHOLDER_ICON = R.drawable.baseline_account_crop_24;
    private static final int PLACEHOLDER_ICON_GROUP = R.drawable.baseline_group_24;
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

    private static class PresenceIndicatorInfo {
        int cx, cy, radius;
    }

    ;
    private final PresenceIndicatorInfo presence = new PresenceIndicatorInfo();

    private boolean update = true;
    private final boolean isGroup;
    private int inSize = -1;

    private final int minSize;
    private final List<Bitmap> workspace;
    private final List<Bitmap> bitmaps;
    private VectorDrawable placeholder;
    private VectorDrawable checkedIcon;
    private final List<RectF> backgroundBounds;
    private final List<Rect> inBounds;
    private String avatarText;
    private float textStartXPoint;
    private float textStartYPoint;
    private int color;

    private final List<Paint> clipPaint;
    private final Paint textPaint = new Paint();
    private final Paint presenceFillPaint;
    private final Paint presenceStrokePaint;
    private final Paint checkedPaint;
    private static final Paint drawPaint = new Paint();

    static {
        drawPaint.setAntiAlias(true);
        drawPaint.setFilterBitmap(true);
    }

    private final boolean cropCircle;
    private boolean isOnline;
    private boolean isChecked;
    private boolean showPresence;

    public static class Builder {

        private List<Bitmap> photos = null;
        private String name = null;
        private String id = null;
        private boolean circleCrop = false;
        private boolean isOnline = false;
        private boolean showPresence = true;
        private boolean isChecked = false;
        private boolean isGroup = false;

        public Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withPhoto(Bitmap photo) {
            this.photos = photo == null ? null : Arrays.asList(photo); // list elements must be mutable
            return this;
        }

        public Builder withPhotos(List<Bitmap> photos) {
            this.photos = photos.isEmpty() ? null : photos;
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

        public Builder withContact(Contact contact) {
            if (contact == null)
                return this;
            return withPhoto((Bitmap) contact.getPhoto())
                    .withId(contact.getPrimaryNumber())
                    .withOnlineState(contact.isOnline())
                    .withNameData(contact.getProfileName(), contact.getUsername());
        }

        public Builder withContacts(List<Contact> contacts) {
            List<Bitmap> bitmaps = new ArrayList<>(contacts.size());
            int notTheUser = 0;
            for (Contact contact : contacts) {
                if (contact.isUser())
                    continue;
                notTheUser++;
                Bitmap bitmap = (Bitmap) contact.getPhoto();
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
                if (bitmaps.size() == 4)
                    break;
            }
            if (notTheUser == 1) {
                for (Contact contact : contacts) {
                    if (!contact.isUser())
                        return withContact(contact);
                }
            }
            if (bitmaps.isEmpty()) {
                // Fallback to the user avatar
                for (Contact contact : contacts)
                    return withContact(contact);
            } else {
                return withPhotos(bitmaps);
            }
            return this;
        }

        public Builder withConversation(Conversation conversation) {
            return conversation.isSwarm()
                    ? withContacts(conversation.getContacts()).setGroup()
                    : withContact(conversation.getContact());
        }

        private Builder setGroup() {
            isGroup = true;
            return this;
        }

        public Builder withViewModel(SmartListViewModel vm) {
            boolean isSwarm = vm.getUri().isSwarm();
            return (isSwarm
                    ? withContacts(vm.getContacts()).setGroup()
                    : withContact(vm.getContacts().isEmpty() ? null : vm.getContacts().get(vm.getContacts().size() - 1)))
                    .withPresence(vm.showPresence())
                    .withOnlineState(vm.isOnline())
                    .withCheck(vm.isChecked());
        }

        public AvatarDrawable build(Context context) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(
                    context, photos, name, id, circleCrop, isGroup);
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
        return VCardServiceImpl.loadProfile(context, account)
                .map(data -> new Builder()
                        .withPhoto((Bitmap) data.second)
                        .withNameData(data.first, account.getRegisteredName())
                        .withId(account.getUri())
                        .withCircleCrop(crop)
                        .build(context));
    }

    public static Single<AvatarDrawable> load(Context context, Account account) {
        return load(context, account, true);
    }

    public void update(Contact contact) {
        String profileName = contact.getProfileName();
        String username = contact.getUsername();
        avatarText = convertNameToAvatarText(
                TextUtils.isEmpty(profileName) ? username : profileName);
        if (bitmaps != null) {
            bitmaps.set(0, (Bitmap) contact.getPhoto());
        }
        isOnline = contact.isOnline();
        update = true;
    }

    public void setName(String name) {
        avatarText = convertNameToAvatarText(name);
        update = true;
    }

    public void setPhoto(Bitmap photo) {
        bitmaps.set(0, photo);
        update = true;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    private AvatarDrawable(Context context, List<Bitmap> photos, String name, String id, boolean crop, boolean group) {
        //Log.w("AvatarDrawable", "AvatarDrawable " + (photos == null ? null : photos.size()) + " " + name);
        cropCircle = crop;
        isGroup = group;
        minSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, SIZE_AB, context.getResources().getDisplayMetrics());
        float borderSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, SIZE_BORDER, context.getResources().getDisplayMetrics());
        if (cropCircle) {
            inSize = minSize;
        }
        if (photos != null && photos.size() > 0) {
            avatarText = null;
            bitmaps = photos;
            if (photos.size() == 1) {
                backgroundBounds = Collections.singletonList(new RectF());
                inBounds = Collections.singletonList(null);
                clipPaint = cropCircle ? Collections.singletonList(new Paint()) : null;
                workspace = Arrays.asList((Bitmap) null);
            } else {
                backgroundBounds = new ArrayList<>(bitmaps.size());
                inBounds = new ArrayList<>(bitmaps.size());
                clipPaint = cropCircle ? new ArrayList<>(bitmaps.size()) : null;
                workspace = cropCircle ? new ArrayList<>(bitmaps.size()) : Arrays.asList((Bitmap) null);
                for (Bitmap ignored : bitmaps) {
                    backgroundBounds.add(new RectF());
                    inBounds.add(cropCircle ? null : new Rect());
                    if (cropCircle) {
                        Paint p = new Paint();
                        p.setStrokeWidth(borderSize);
                        p.setColor(Color.WHITE);
                        p.setStyle(Paint.Style.FILL);
                        clipPaint.add(p);
                        workspace.add(null);
                    }
                }
            }
        } else {
            workspace = Arrays.asList((Bitmap) null);
            bitmaps = null;
            backgroundBounds = null;
            inBounds = null;
            avatarText = convertNameToAvatarText(name);
            color = ContextCompat.getColor(context, getAvatarColor(id));
            clipPaint = cropCircle ? Collections.singletonList(new Paint()) : null;
            if (avatarText == null) {
                placeholder = (VectorDrawable) context.getDrawable(isGroup ? PLACEHOLDER_ICON_GROUP : PLACEHOLDER_ICON);
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

        if (clipPaint != null)
            for (Paint p : clipPaint)
                p.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
    }

    public AvatarDrawable(AvatarDrawable other) {
        //Log.w("AvatarDrawable", "AvatarDrawable copy");
        cropCircle = other.cropCircle;
        isGroup = other.isGroup;
        minSize = other.minSize;
        bitmaps = other.bitmaps;
        backgroundBounds = other.backgroundBounds == null ? null : new ArrayList<>(other.backgroundBounds.size());
        if (backgroundBounds != null) {
            for (int i = 0, n = other.backgroundBounds.size(); i < n; i++) {
                backgroundBounds.add(new RectF());
            }
        }

        inBounds = other.inBounds;
        color = other.color;
        placeholder = other.placeholder;
        avatarText = other.avatarText;
        workspace = new ArrayList<>(other.workspace.size());
        for (int i = 0, n = other.workspace.size(); i < n; i++) {
            workspace.add(null);
        }
        clipPaint = other.clipPaint == null ? null : new ArrayList<>(other.clipPaint.size());
        if (clipPaint != null) {
            for (int i = 0, n = other.clipPaint.size(); i < n; i++) {
                clipPaint.add(new Paint(other.clipPaint.get(i)));
                clipPaint.get(i).setShader(null);
            }
        }

        isOnline = other.isOnline;
        isChecked = other.isChecked;
        showPresence = other.showPresence;
        presenceFillPaint = other.presenceFillPaint;
        presenceStrokePaint = other.presenceStrokePaint;
        checkedPaint = other.checkedPaint;

        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
    }

    @Override
    public void draw(@NonNull Canvas finalCanvas) {
        if (workspace.get(0) == null)
            return;
        if (update) {
            for (int i = 0, s = workspace.size(); i < s; i++)
                drawActual(i, new Canvas(workspace.get(i)));
            update = false;
        }
        if (cropCircle) {
            finalCanvas.save();
            finalCanvas.translate((getBounds().width() - workspace.get(0).getWidth())  / 2.f, (getBounds().height() - workspace.get(0).getHeight())  / 2.f);
            float r = Math.min(workspace.get(0).getWidth(), workspace.get(0).getHeight()) / 2;
            int cx =  workspace.get(0).getWidth()/2;//getBounds().centerX();
            float cy = workspace.get(0).getHeight()/2;//getBounds().height() / 2;
            int i = 0;
            final float ratio = 1.333333f;
            for (Paint paint : clipPaint) {
                finalCanvas.drawCircle(cx, workspace.get(0).getHeight() - cy, r, paint);
                if (i != 0) {
                    Shader s = paint.getShader();
                    paint.setShader(null);
                    paint.setStyle(Paint.Style.STROKE);
                    finalCanvas.drawCircle(cx, workspace.get(0).getHeight() - cy, r, paint);
                    paint.setShader(s);
                    paint.setStyle(Paint.Style.FILL);
                }
                i++;
                r /= ratio;
                cy /= ratio;
            }

            finalCanvas.restore();
        } else {
            finalCanvas.drawBitmap(workspace.get(0), null, getBounds(), drawPaint);
        }
        if (showPresence && isOnline) {
            drawPresence(finalCanvas);
        }
        if (isChecked) {
            drawChecked(finalCanvas);
        }
    }

    private void drawActual(int i, @NonNull Canvas canvas) {
        if (bitmaps != null) {
            if (cropCircle) {
                canvas.drawBitmap(bitmaps.get(i), inBounds.get(i), backgroundBounds.get(i), drawPaint);
            } else {
                if (backgroundBounds.size() == bitmaps.size())
                    for (int n = 0, s = bitmaps.size(); n < s; n++) {
                        canvas.drawBitmap(bitmaps.get(n), inBounds.get(n), backgroundBounds.get(n), drawPaint);
                    }
            }
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
            checkedIcon.setBounds(presence.cx - presence.radius, presence.cy - presence.radius, presence.cx + presence.radius, presence.cy + presence.radius);
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

    private static Rect getSubBounds(@NonNull Rect bounds, int total, int i) {
        if (total == 1)
            return bounds;

        if (total == 2 || (total == 3 && i == 0)) {
            //Rect zone = getSubZone(bounds, 2, 1);
            int w = bounds.width() / 2;
            return (i == 0)
                    ? new Rect(bounds.left, bounds.top, bounds.left + w, bounds.bottom)
                    : new Rect(bounds.left + w, bounds.top, bounds.right, bounds.bottom);
        }
        if (total == 3 || (total == 4 && (i == 1 || i == 2))) {
            int w = bounds.width() / 2;
            int h = bounds.height() / 2;
            return (i == 1)
                    ? new Rect(bounds.left + w, bounds.top, bounds.right, bounds.top + h)
                    : new Rect(bounds.left + w, bounds.top + h, bounds.right, bounds.bottom);
        }
        if (total == 4) {
            int w = bounds.width() / 2;
            int h = bounds.height() / 2;
            return (i == 0)
                    ? new Rect(bounds.left, bounds.top, bounds.left + w, bounds.top + h)
                    : new Rect(bounds.left, bounds.top + h, bounds.left + w, bounds.bottom);
        }
        return null;
    }

    private static <T> void fit(int iw, int ih, int bw, int bh, boolean outfit, T ret) {
        int a = bw * ih;
        int b = bh * iw;
        int w;
        int h;
        if (outfit == (a < b)) {
            w = iw;
            h = (iw * bh) / bw;
        } else {
            w = (ih * bw) / bh;
            h = ih;
        }
        int x = (iw - w) / 2;
        int y = (ih - h) / 2;
        if (ret instanceof Rect)
            ((Rect) ret).set(x, y, x + w, y + h);
        else if (ret instanceof RectF)
            ((RectF) ret).set(x, y, x + w, y + h);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        //if (showPresence)
        setupPresenceIndicator(bounds);
        int d = Math.min(bounds.width(), bounds.height());
        if (placeholder != null) {
            int cx = (bounds.width() - d) / 2;
            int cy = (bounds.height() - d) / 2;
            placeholder.setBounds(cx, cy, cx + d, cy + d);
        }
        int iw = cropCircle ? d : bounds.width();
        int ih = cropCircle ? d : bounds.height();
        for (int i = 0, n = workspace.size(); i < n; i++) {
            if (workspace.get(i) != null) {
                workspace.get(i).recycle();
                workspace.set(i, null);
                clipPaint.get(i).setShader(null);
            }
        }
        if (iw <= 0 || ih <= 0) {
            return;
        }
        if (cropCircle) {
            for (int i = 0, s = workspace.size(); i < s; i++) {
                Bitmap workspacei = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
                workspace.set(i, workspacei);
                clipPaint.get(i).setShader(new BitmapShader(workspacei, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            }
        } else {
            workspace.set(0, Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888));
        }

        if (bitmaps != null) {
            if (bitmaps.size() == 1 || cropCircle) {
                for (int i = 0; i < bitmaps.size(); i++) {
                    Bitmap bitmap = bitmaps.get(i);
                    fit(iw, ih, bitmap.getWidth(), bitmap.getHeight(), true, backgroundBounds.get(i));
                }
            } else {
                Rect realBounds = cropCircle ? new Rect(0, 0, iw, ih) : bounds;
                for (int i = 0; i < bitmaps.size(); i++) {
                    Bitmap bitmap = bitmaps.get(i);
                    Rect subBounds = getSubBounds(realBounds, bitmaps.size(), i);
                    if (subBounds != null) {
                        fit(bitmap.getWidth(), bitmap.getHeight(), subBounds.width(), subBounds.height(), false, inBounds.get(i));
                        backgroundBounds.get(i).set(subBounds);
                    }
                }
            }
        } else {
            setAvatarTextValues(bounds);
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