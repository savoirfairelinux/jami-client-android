/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import cx.ring.mvp.RingAccountViewModel;

public class RingAccountViewModelImpl extends RingAccountViewModel implements Parcelable {

    private Bitmap photo;

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
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

    public RingAccountViewModelImpl() {
    }

    protected RingAccountViewModelImpl(Parcel in) {
        this.photo = in.readParcelable(Bitmap.class.getClassLoader());
        this.mFullName = in.readString();
        this.mUsername = in.readString();
        this.mPassword = in.readString();
        this.mPin = in.readString();
        this.link = in.readByte() != 0;
    }

    public static final Creator<RingAccountViewModelImpl> CREATOR = new Creator<RingAccountViewModelImpl>() {
        @Override
        public RingAccountViewModelImpl createFromParcel(Parcel source) {
            return new RingAccountViewModelImpl(source);
        }

        @Override
        public RingAccountViewModelImpl[] newArray(int size) {
            return new RingAccountViewModelImpl[size];
        }
    };
}
