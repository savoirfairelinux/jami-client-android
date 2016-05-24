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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.Random;

import cx.ring.model.TextMessage;

@DatabaseTable(tableName = HistoryText.TABLE_NAME)
public class HistoryText
{
    public static final String TABLE_NAME = "historytext";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TIMESTAMP_NAME = "TIMESTAMP";
    public static final String COLUMN_NUMBER_NAME = "number";
    public static final String COLUMN_DIRECTION_NAME = "direction";
    public static final String COLUMN_ACCOUNT_ID_NAME = "accountID";
    public static final String COLUMN_CONTACT_ID_NAME = "contactID";
    public static final String COLUMN_CONTACT_KEY_NAME = "contactKey";
    public static final String COLUMN_CALL_ID_NAME = "callID";
    public static final String COLUMN_MESSAGE_NAME = "message";
    public static final String COLUMN_READ_NAME = "read";

    @DatabaseField(index = true, columnName=COLUMN_ID_NAME, id = true)
    public long id;
    @DatabaseField(index = true, columnName=COLUMN_TIMESTAMP_NAME)
    public long time;
    @DatabaseField(columnName=COLUMN_NUMBER_NAME)
    public String number;
    @DatabaseField(columnName=COLUMN_DIRECTION_NAME)
    public int direction;
    @DatabaseField(columnName=COLUMN_ACCOUNT_ID_NAME)
    String accountID;
    @DatabaseField(columnName=COLUMN_CONTACT_ID_NAME)
    long contactID;
    @DatabaseField(columnName=COLUMN_CONTACT_KEY_NAME)
    String contactKey;
    @DatabaseField(columnName=COLUMN_CALL_ID_NAME)
    String callID;
    @DatabaseField(columnName=COLUMN_MESSAGE_NAME)
    String message;
    @DatabaseField(columnName=COLUMN_READ_NAME)
    boolean read;

    static private final Random R = new Random();

    public HistoryText(TextMessage txt) {
        id = txt.getId();
        if (id == 0)
            id = R.nextLong();
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

    public String getContactKey() {
        return contactKey;
    }

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

    public String getNumber() {
        return number;
    }

    public String getMessage() {
        return message;
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
