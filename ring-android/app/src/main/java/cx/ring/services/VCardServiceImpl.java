/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import androidx.annotation.NonNull;
import cx.ring.model.Account;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class VCardServiceImpl extends VCardService {

    private Context mContext;

    public VCardServiceImpl(Context context) {
        this.mContext = context;
    }

    public static Single<Tuple<String, Object>> loadProfile(@NonNull Account account) {
        synchronized (account) {
            Single<Tuple<String, Object>> ret = account.getLoadedProfile();
            if (ret == null) {
                ret = Single.fromCallable(() -> readData(account.getProfile()))
                        .subscribeOn(Schedulers.computation())
                        .cache();
                account.setLoadedProfile(ret);
            }
            return ret;
        }
    }

    @Override
    public Single<VCard> loadSmallVCard(String accountId, int maxSize) {
        return VCardUtils
                .loadLocalProfileFromDisk(mContext.getFilesDir(), accountId)
                .map(vcard -> {
                    if (!vcard.getPhotos().isEmpty()) {
                        // Reduce photo size to fit in one DHT packet
                        Bitmap photo = BitmapUtils.bytesToBitmap(vcard.getPhotos().get(0).getData(), maxSize);
                        //photo = BitmapUtils.reduceBitmap(photo, maxSize);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 88, stream);
                        vcard.removeProperties(Photo.class);
                        vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.JPEG));
                        vcard.removeProperties(RawProperty.class);
                    }
                    return vcard;
                });
    }

    @Override
    public Single<Tuple<String, Object>> loadVCardProfile(VCard vcard) {
        return Single.fromCallable(() -> readData(vcard));
    }

    public static Tuple<String, Object> readData(VCard vcard) {
        return readData(VCardUtils.readData(vcard));
    }
    public static Tuple<String, Object> readData(Tuple<String, byte[]> profile) {
        return new Tuple<>(profile.first, BitmapUtils.bytesToBitmap(profile.second));
    }

}
