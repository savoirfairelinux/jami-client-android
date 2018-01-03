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
package cx.ring.tv.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TVContactRequestViewModel implements Parcelable {
    private String contactId;
    private String displayName;
    private String userName;
    private byte[] photo = null;
    private String message;

    protected TVContactRequestViewModel(Parcel in) {
        this.contactId = in.readString();
        this.displayName = in.readString();
        this.userName = in.readString();
        this.message = in.readString();
        this.photo = new byte[in.readInt()];
        in.readByteArray(photo);
    }

    public TVContactRequestViewModel(String contactId, String displayName, String userName, byte[] photo, String message) {
        this.contactId = contactId;
        this.displayName = displayName;
        this.userName = userName;
        this.photo = photo;
        this.message = message;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static final Creator<TVContactRequestViewModel> CREATOR = new Creator<TVContactRequestViewModel>() {
        @Override
        public TVContactRequestViewModel createFromParcel(Parcel in) {
            return new TVContactRequestViewModel(in);
        }

        @Override
        public TVContactRequestViewModel[] newArray(int size) {
            return new TVContactRequestViewModel[size];
        }
    };

    @Override
    public boolean equals(Object m) {
        return m instanceof TVContactRequestViewModel && contactId.equals(((TVContactRequestViewModel) m).contactId);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(contactId);
        dest.writeString(displayName);
        dest.writeString(userName);
        dest.writeString(message);
        if(photo != null) {
            dest.writeInt(photo.length);
            dest.writeByteArray(photo);
        }
    }
}
