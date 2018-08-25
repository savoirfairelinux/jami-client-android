/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
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

package cx.ring.contacts;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.utils.CircleTransform;
import cx.ring.views.AvatarDrawable;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class AvatarFactory {

    public static final int SIZE_AB = 36;
    public static final int SIZE_NOTIF = 48;
    
    private static final RequestOptions GLIDE_OPTIONS = new RequestOptions()
            .centerCrop()
            .error(R.drawable.ic_contact_picture_fallback);

    private static final RequestOptions GLIDE_OPTIONS_CIRCLE = new RequestOptions()
            .centerCrop()
            .error(R.drawable.ic_contact_picture_fallback)
            .transform(new CircleTransform());

    private AvatarFactory() {}

    private static Drawable getDrawable(Context context, byte[] photo, String profileName, String username, String id) {
        return new AvatarDrawable(context, photo, TextUtils.isEmpty(profileName) ? username : profileName, id, true);
    }

    private static <T> RequestBuilder<T> getGlideRequest(Context context, RequestBuilder<T> request, byte[] photo, String profileName, String username, String id) {
        return request.load(getDrawable(context, photo, profileName, username, id));
    }

    public static RequestBuilder<Drawable> getGlideAvatar(Context context, RequestManager manager, CallContact contact) {
        return getGlideRequest(context, manager.asDrawable(), contact.getPhoto(), contact.getProfileName(), contact.getUsername(), contact.getPrimaryNumber());
    }

    public static Single<Drawable> getAvatar(Context context, CallContact contact) {
        return Single.fromCallable(() -> new AvatarDrawable(context, contact));
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : size;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : size;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Single<Bitmap> getBitmapAvatar(Context context, CallContact contact, int size) {
        return getAvatar(context, contact)
                .map(d -> drawableToBitmap(d, size))
                .subscribeOn(Schedulers.computation());
    }

    public static RequestBuilder<Drawable> getGlideAvatar(Context context, RequestManager manager, VCard vcard, String username, String ringId) {
        byte[] photo = null;
        String profile = null;
        if (vcard != null) {
            if (vcard.getPhotos() != null && !vcard.getPhotos().isEmpty()) {
                photo = vcard.getPhotos().get(0).getData();
            }
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String n = name.getValue();
                if (!TextUtils.isEmpty(n))
                    profile = n;
            }
        }

        return getGlideRequest(context, manager.asDrawable(), photo, profile, username, ringId)
                .transition(DrawableTransitionOptions.withCrossFade(100));
    }

    public static RequestBuilder<Drawable> getGlideAvatar(Fragment fragment, CallContact contact) {
        return getGlideAvatar(fragment.getActivity(), Glide.with(fragment), contact);
    }

    public static RequestBuilder<Drawable> getGlideAvatar(Context context, CallContact contact) {
        return getGlideAvatar(context, Glide.with(context), contact);
    }

    public static void loadGlideAvatar(ImageView view, CallContact contact) {
        getGlideAvatar(view.getContext(), contact).into(view);
    }

    public static RequestBuilder<Bitmap> getBitmapGlideAvatar(Context context, CallContact contact) {
        return getGlideRequest(context, Glide.with(context).asBitmap(), contact.getPhoto(), contact.getProfileName(), contact.getUsername(), contact.getPrimaryNumber());
    }

    public static RequestOptions getGlideOptions(boolean circle) {
        return circle ? GLIDE_OPTIONS_CIRCLE : GLIDE_OPTIONS;
    }

    public static void clearCache() {
    }
}
