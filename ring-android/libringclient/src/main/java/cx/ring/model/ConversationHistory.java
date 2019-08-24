/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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

@DatabaseTable(tableName = ConversationHistory.TABLE_NAME)
public class ConversationHistory {

    public static final String TABLE_NAME = "conversations";
    public static final String COLUMN_CONVERSATION_ID = "id";
    public static final String COLUMN_PARTICIPANT = "participant";
    public static final String COLUMN_EXTRA_DATA = "extra_data";

    @DatabaseField(generatedId = true , columnName = COLUMN_CONVERSATION_ID, canBeNull = false)
    Integer mId;
    @DatabaseField(columnName = COLUMN_PARTICIPANT, index = true)
    String mParticipant;
    @DatabaseField(columnName = COLUMN_EXTRA_DATA)
    String mExtraData;

    String account;


    /* Needed by ORMLite */
    public ConversationHistory() {
    }

    public ConversationHistory(Conversation conversation) {
        mId = conversation.getId();
        mParticipant = conversation.getParticipant();
    }

    public ConversationHistory(Integer id, String participant) {
        mId = id;
        mParticipant = participant;
    }

    public ConversationHistory(String participant) {
        mParticipant = participant;
    }

    public Integer getId() {
        return mId;
    }

    public void setId(Integer id) {
        mId = id;
    }

    public String getParticipant() {
        return mParticipant;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

}
