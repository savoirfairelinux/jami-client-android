/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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

import cx.ring.model.CallContact;

public class TVListViewModel implements Parcelable {
    private String mAccountId;
    private CallContact mCallContact;
    private boolean isOnline;

    public TVListViewModel(String accountId, CallContact callContact) {
        mAccountId = accountId;
        mCallContact = callContact;
        isOnline = callContact.isOnline();
    }

    protected TVListViewModel(Parcel in) {
        mAccountId = in.readString();
        isOnline = in.readByte() != 0;
    }

    public static final Creator<TVListViewModel> CREATOR = new Creator<TVListViewModel>() {
        @Override
        public TVListViewModel createFromParcel(Parcel in) {
            return new TVListViewModel(in);
        }

        @Override
        public TVListViewModel[] newArray(int size) {
            return new TVListViewModel[size];
        }
    };

    public CallContact getContact() {
        return mCallContact;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean pOnline) {
        isOnline = pOnline;
    }

    @Override
    public String toString() {
        return mCallContact.toString() + " " + isOnline;
    }

    public String getAccountId() {
        return mAccountId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAccountId);
        dest.writeByte((byte) (isOnline ? 1 : 0));
    }

}
