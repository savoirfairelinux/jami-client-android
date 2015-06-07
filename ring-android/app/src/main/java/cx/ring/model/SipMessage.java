/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package cx.ring.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SipMessage implements Parcelable {
    public boolean left;
    public String comment;

    public SipMessage(boolean left, String comment) {
        super();
        this.left = left;
        this.comment = comment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte) (left ? 1 : 0));
        out.writeString(comment);
    }

    public static final Parcelable.Creator<SipMessage> CREATOR = new Parcelable.Creator<SipMessage>() {
        public SipMessage createFromParcel(Parcel in) {
            return new SipMessage(in);
        }

        public SipMessage[] newArray(int size) {
            return new SipMessage[size];
        }
    };

    private SipMessage(Parcel in) {
        left = (in.readByte() == 1);
        comment = in.readString();
    }

}