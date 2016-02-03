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
import java.util.Date;
import cx.ring.model.TextMessage;

public class HistoryText
{

    @DatabaseField(index = true, columnName="id", generatedId = true)
    public long id;
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
