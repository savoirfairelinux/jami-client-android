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


package cx.ring.history;

import android.os.Parcel;
import android.os.Parcelable;
import com.j256.ormlite.field.DatabaseField;
import cx.ring.model.SipCall;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryCall implements Parcelable {

    @DatabaseField(index = true, columnName="TIMESTAMP_START")
    long call_start;
    @DatabaseField
    long call_end;
    @DatabaseField
    String number;
    @DatabaseField
    boolean missed;
    @DatabaseField
    int direction;
    @DatabaseField
    String recordPath;
    @DatabaseField
    String accountID;
    @DatabaseField
    long contactID;
    @DatabaseField
    String callID;

    public String getAccountID() {
        return accountID;
    }

    public long getContactID() {
        return contactID;
    }

    public HistoryCall(SipCall call) {
        call_start = call.getTimestampStart_();
        call_end = call.getTimestampEnd_();
        accountID = call.getAccount().getAccountID();
        number = call.getmContact().getPhones().get(0).getNumber();
        missed = call.isRinging() && call.isIncoming();
        direction = call.getCallType();
        recordPath = call.getRecordPath();
        contactID = call.getmContact().getId();
        callID = call.getCallId();
    }

    /* Needed by ORMLite */
    public HistoryCall() {
    }

    public String getDirection() {
        switch (direction) {
            case SipCall.direction.CALL_TYPE_INCOMING:
                return "CALL_TYPE_INCOMING";
            case SipCall.direction.CALL_TYPE_OUTGOING:
                return "CALL_TYPE_OUTGOING";
            default:
                return "CALL_TYPE_UNDETERMINED";
        }
    }

    public String getDate() {
        return HistoryTimeModel.timeToHistoryConst(call_start);
    }
    public Date getStartDate() {
        return new Date(call_start);
    }
    public Date getEndDate() {
        return new Date(call_end * 1000);
    }

    public String getStartString(String format) {
        Timestamp stamp = new Timestamp(call_start * 1000); // in milliseconds
        Date date = new Date(stamp.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);

    }

    public String getDurationString() {

        long duration = call_end - call_start;
        if (duration < 60)
            return String.format(Locale.getDefault(), "%02d secs", duration);

        if (duration < 3600)
            return String.format(Locale.getDefault(), "%02d mins %02d secs", (duration % 3600) / 60, (duration % 60));

        return String.format(Locale.getDefault(), "%d h %02d mins %02d secs", duration / 3600, (duration % 3600) / 60, (duration % 60));

    }

    public long getDuration() {
        return call_end - call_start;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(call_start);
        dest.writeLong(call_end);
        dest.writeString(accountID);
        dest.writeString(number);
        dest.writeByte((byte) (missed ? 1 : 0));
        dest.writeInt(direction);
        dest.writeString(recordPath);
        dest.writeLong(contactID);
        dest.writeString(callID);
    }

    public static final Parcelable.Creator<HistoryCall> CREATOR = new Parcelable.Creator<HistoryCall>() {
        public HistoryCall createFromParcel(Parcel in) {
            return new HistoryCall(in);
        }

        public HistoryCall[] newArray(int size) {
            return new HistoryCall[size];
        }
    };

    private HistoryCall(Parcel in) {
        call_start = in.readLong();
        call_end = in.readLong();
        accountID = in.readString();
        number = in.readString();
        missed = in.readByte() == 1;
        direction = in.readInt();
        recordPath = in.readString();
        contactID = in.readLong();
        callID = in.readString();
    }

    public boolean hasRecord() {
        return recordPath.length() > 0;
    }

    public boolean isIncoming() {
        return direction == SipCall.direction.CALL_TYPE_INCOMING;
    }

    public boolean isMissed() {
        return missed;
    }

}
