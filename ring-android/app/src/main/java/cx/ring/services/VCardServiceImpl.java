/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class VCardServiceImpl extends VCardService {

    private Context mContext;

    public VCardServiceImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public VCard loadSmallVCard(String accountId, int maxSize) {
        VCard vcard = VCardUtils.loadLocalProfileFromDisk(mContext.getFilesDir(), accountId);
        if (vcard != null && !vcard.getPhotos().isEmpty()) {
            // Reduce photo size to fit in one DHT packet
            Bitmap photo = BitmapUtils.bytesToBitmap(vcard.getPhotos().get(0).getData());
            photo = BitmapUtils.reduceBitmap(photo, maxSize);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 92, stream);
            vcard.removeProperties(Photo.class);
            vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.JPEG));
            vcard.removeProperties(RawProperty.class);
        }
        return vcard;
    }
}
