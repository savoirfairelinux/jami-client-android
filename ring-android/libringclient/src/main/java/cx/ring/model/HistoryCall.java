/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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


package cx.ring.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

@DatabaseTable(tableName = HistoryCall.TABLE_NAME)
public class HistoryCall implements ConversationElement, Serializable {

    public static final String TABLE_NAME = "historycall";
    public static final String COLUMN_TIMESTAMP_START_NAME = "TIMESTAMP_START";
    public static final String COLUMN_TIMESTAMP_END_NAME = "call_end";
    public static final String COLUMN_NUMBER_NAME = "number";
    public static final String COLUMN_MISSED_NAME = "missed";
    public static final String COLUMN_DIRECTION_NAME = "direction";
    public static final String COLUMN_RECORD_PATH_NAME = "recordPath";
    public static final String COLUMN_ACCOUNT_ID_NAME = "accountID";
    public static final String COLUMN_CONTACT_ID_NAME = "contactID";
    public static final String COLUMN_CONTACT_KEY_NAME = "contactKey";
    public static final String COLUMN_CALL_ID_NAME = "callID";

    @DatabaseField(index = true, columnName = COLUMN_TIMESTAMP_START_NAME)
    public long call_start;
    @DatabaseField(columnName = COLUMN_TIMESTAMP_END_NAME)
    public long call_end;
    @DatabaseField(index = true, columnName = COLUMN_NUMBER_NAME)
    public String number;
    @DatabaseField(columnName = COLUMN_MISSED_NAME)
    boolean missed;
    @DatabaseField(columnName = COLUMN_DIRECTION_NAME)
    int direction;
    @DatabaseField(columnName = COLUMN_RECORD_PATH_NAME)
    String recordPath;
    @DatabaseField(index = true, columnName = COLUMN_ACCOUNT_ID_NAME)
    String accountID;
    @DatabaseField(columnName = COLUMN_CONTACT_ID_NAME)
    long contactID;
    @DatabaseField(columnName = COLUMN_CONTACT_KEY_NAME)
    String contactKey;
    @DatabaseField(uniqueIndex = true, columnName = COLUMN_CALL_ID_NAME)
    String callID;

    /* Needed by ORMLite */
    public HistoryCall() {
    }

    public HistoryCall(SipCall call) {
        call_start = call.getTimestampStart();
        call_end = call.getTimestampEnd();
        accountID = call.getAccount();
        number = call.getNumber();
        missed = call.isMissed();
        direction = call.getCallType().getValue();
        recordPath = call.getRecordPath();
        contactID = call.getContact().getId();
        contactKey = call.getContact().getKey();
        callID = call.getCallId();
    }

    public String getAccountID() {
        return accountID;
    }

    public long getContactID() {
        return contactID;
    }

    public String getContactKey() {
        return contactKey;
    }

    public Date getStartDate() {
        return new Date(call_start);
    }

    public Date getEndDate() {
        return new Date(call_end);
    }

    public String getDurationString() {
        long duration = (call_end - call_start) / 1000;
        if (duration < 60) {
            return String.format(Locale.getDefault(), "%02d secs", duration);
        }

        if (duration < 3600) {
            return String.format(Locale.getDefault(), "%02d mins %02d secs", (duration % 3600) / 60, (duration % 60));
        }

        return String.format(Locale.getDefault(), "%d h %02d mins %02d secs", duration / 3600, (duration % 3600) / 60, (duration % 60));
    }

    public long getDuration() {
        return call_end - call_start;
    }

    public String getNumber() {
        return number;
    }

    public Uri getContactNumber() {
        return new Uri(number);
    }

    @Override
    public boolean isRead() {
        return true;
    }

    public boolean isIncoming() {
        return direction == SipCall.Direction.INCOMING.getValue();
    }

    public boolean isMissed() {
        return missed;
    }

    public CharSequence getCallId() {
        return callID;
    }

    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HistoryCall that = (HistoryCall) o;

        if (call_start != that.call_start || call_end != that.call_end || contactID != that.contactID) {
            return false;
        }
        return callID != null ? callID.equals(that.callID) : that.callID == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (call_start ^ (call_start >>> 32));
        result = 31 * result + (int) (call_end ^ (call_end >>> 32));
        result = 31 * result + (int) (contactID ^ (contactID >>> 32));
        result = 31 * result + (callID != null ? callID.hashCode() : 0);
        return result;
    }*/

    private long code() {
        long result = call_start ^ (call_start >>> 32);
        result = 31 * result + (call_end ^ (call_end >>> 32));
        result = 31 * result + (contactID ^ (contactID >>> 32));
        result = 31 * result + (callID != null ? callID.hashCode() : 0);
        return result;
    }

    @Override
    public CEType getType() {
        return CEType.CALL;
    }

    @Override
    public long getDate() {
        return call_start;
    }

    @Override
    public long getId() {
        return code();
    }
}
