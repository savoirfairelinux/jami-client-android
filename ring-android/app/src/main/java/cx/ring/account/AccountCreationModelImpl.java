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
package cx.ring.account;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;

import cx.ring.mvp.AccountCreationModel;
import cx.ring.utils.BitmapUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.Single;

public class AccountCreationModelImpl extends AccountCreationModel implements Parcelable {

    public static final Creator<AccountCreationModelImpl> CREATOR = new Creator<AccountCreationModelImpl>() {
        @Override
        public AccountCreationModelImpl createFromParcel(Parcel source) {
            return new AccountCreationModelImpl(source);
        }

        @Override
        public AccountCreationModelImpl[] newArray(int size) {
            return new AccountCreationModelImpl[size];
        }
    };
    private Bitmap photo;

    public AccountCreationModelImpl() {
    }

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

    protected AccountCreationModelImpl(Parcel in) {
        this.photo = in.readParcelable(Bitmap.class.getClassLoader());
        this.mFullName = in.readString();
        this.mUsername = in.readString();
        this.mPassword = in.readString();
        this.mPin = in.readString();
        this.link = in.readByte() != 0;
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
        profile.onNext(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.photo, flags);
        dest.writeString(this.mFullName);
        dest.writeString(this.mUsername);
        dest.writeString(this.mPassword);
        dest.writeString(this.mPin);
        dest.writeByte(this.link ? (byte) 1 : (byte) 0);
    }
}
