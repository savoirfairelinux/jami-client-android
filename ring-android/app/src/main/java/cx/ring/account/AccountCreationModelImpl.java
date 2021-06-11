/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.account;

import android.graphics.Bitmap;

import java.io.Serializable;

import net.jami.mvp.AccountCreationModel;
import cx.ring.utils.BitmapUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.rxjava3.core.Single;

public class AccountCreationModelImpl extends AccountCreationModel implements Serializable {

    @Override
    public Single<VCard> toVCard() {
        return Single
                .fromCallable(() -> {
                    VCard vcard = new VCard();
                    vcard.setFormattedName(new FormattedName(getFullName()));
                    vcard.setUid(new Uid(getUsername()));
                    Bitmap bmp = getPhoto();
                    if (bmp != null) {
                        vcard.removeProperties(Photo.class);
                        vcard.addPhoto(BitmapUtils.bitmapToPhoto(bmp));
                    }
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                });
    }

    @Override
    public Bitmap getPhoto() {
        return (Bitmap) super.getPhoto();
    }

    public void setPhoto(Bitmap photo) {
        super.setPhoto(photo);
    }
}
