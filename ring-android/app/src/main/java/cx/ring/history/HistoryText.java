/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.history;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import cx.ring.model.CallContact;
import cx.ring.model.TextMessage;

public class HistoryText implements Parcelable {

    @DatabaseField(index = true, columnName="id")
    public String id;
    @DatabaseField(index = true, columnName="TIMESTAMP")
    public long time;
    @DatabaseField
    public String number;
    @DatabaseField
    public int direction;
    @DatabaseField
    String accountID;
    @DatabaseField
    long contactID;
    @DatabaseField
    String contactKey;
    @DatabaseField
    String callID;
    @DatabaseField
    String message;
    @DatabaseField
    boolean read;

    public HistoryText(TextMessage txt) {
        id = txt.getId();
        time = txt.getTimestamp();
        accountID = txt.getAccount();
        number = txt.getNumber();
        direction = txt.getCallType();
        message = txt.getMessage();
        callID = txt.getCallId();
        if (txt.getContact() != null) {
            contactID = txt.getContact().getId();
            contactKey = txt.getContact().getKey();
        }
        read = txt.isRead();
    }

    public String getAccountID() {
        return accountID;
    }

    public long getContactID() {
        return contactID;
    }

    /*
    public HistoryText(String account, String from, String msg, CallContact contact, boolean incoming) {
        time = System.currentTimeMillis();
        accountID = account;
        number = from;
        direction = incoming ? TextMessage.direction.INCOMING : TextMessage.direction.OUTGOING;
        if (contact != null) {
            contactID = contact.getId();
            contactKey = contact.getKey();
        }
        //callID = call.getCallId();
        message = msg;
    }*/

    /* Needed by ORMLite */
    public HistoryText() {
    }

    public String getDirection() {
        switch (direction) {
            case TextMessage.direction.INCOMING:
                return "INCOMING";
            case TextMessage.direction.OUTGOING:
                return "OUTGOING";
            default:
                return "CALL_TYPE_UNDETERMINED";
        }
    }

    public Date getDate() {
        return new Date(time);
    }

    /*
    public String getTimeString(String format) {
        Timestamp stamp = new Timestamp(time); // in milliseconds
        Date date = new Date(stamp.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }*/

    public String getNumber() {
        return number;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(time);
        dest.writeString(accountID);
        dest.writeString(number);
        dest.writeInt(direction);
        dest.writeLong(contactID);
        dest.writeString(callID);
        dest.writeString(message);
        dest.writeByte(read ? (byte) 1 : (byte) 0);
    }

    public static final Creator<HistoryText> CREATOR = new Creator<HistoryText>() {
        public HistoryText createFromParcel(Parcel in) {
            return new HistoryText(in);
        }

        public HistoryText[] newArray(int size) {
            return new HistoryText[size];
        }
    };

    public HistoryText(Parcel in) {
        id = in.readString();
        time = in.readLong();
        accountID = in.readString();
        number = in.readString();
        direction = in.readInt();
        contactID = in.readLong();
        callID = in.readString();
        message = in.readString();
        read = in.readByte() != 0;
    }

    public boolean isIncoming() {
        return direction == TextMessage.direction.INCOMING;
    }

    public String getCallId() {
        return callID;
    }

    public boolean isRead() {
        return read;
    }
}
