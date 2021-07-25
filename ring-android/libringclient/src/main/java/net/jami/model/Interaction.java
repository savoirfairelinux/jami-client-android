/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = Interaction.TABLE_NAME)
public class Interaction {

    public static final String TABLE_NAME = "interactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_CONVERSATION = "conversation";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DAEMON_ID = "daemon_id";
    public static final String COLUMN_IS_READ = "is_read";
    public static final String COLUMN_EXTRA_FLAG = "extra_data";
    protected String mAccount;
    boolean mIsIncoming;
    Contact mContact = null;

    @DatabaseField(generatedId = true, columnName = COLUMN_ID, index = true)
    int mId;
    @DatabaseField(columnName = COLUMN_AUTHOR, index = true)
    String mAuthor;
    @DatabaseField(columnName = COLUMN_CONVERSATION, foreignColumnName = ConversationHistory.COLUMN_CONVERSATION_ID, foreign = true)
    ConversationHistory mConversation;
    @DatabaseField(columnName = COLUMN_TIMESTAMP, index = true)
    long mTimestamp;
    @DatabaseField(columnName = COLUMN_BODY)
    String mBody;
    @DatabaseField(columnName = COLUMN_TYPE)
    String mType;
    @DatabaseField(columnName = COLUMN_STATUS)
    String mStatus = InteractionStatus.UNKNOWN.toString();
    @DatabaseField(columnName = COLUMN_DAEMON_ID)
    Long mDaemonId = null;
    @DatabaseField(columnName = COLUMN_IS_READ)
    int mIsRead = 0;
    @DatabaseField(columnName = COLUMN_EXTRA_FLAG)
    String mExtraFlag = new JsonObject().toString();

    // Swarm
    private String mConversationId = null;
    private String mMessageId = null;
    private String mParentId = null;

    /* Needed by ORMLite */
    public Interaction() {
    }
    public Interaction(String accountId) {
        mAccount = accountId;
        setType(InteractionType.INVALID);
    }

    public Interaction(Conversation conversation, InteractionType type) {
        mConversation = conversation;
        mAccount = conversation.getAccountId();
        mType = type.toString();
    }

    public Interaction(String id, String author, ConversationHistory conversation, String timestamp, String body, String type, String status, String daemonId, String isRead, String extraFlag) {
        mId = Integer.parseInt(id);
        mAuthor = author;
        mConversation = conversation;
        mTimestamp = Long.parseLong(timestamp);
        mBody = body;
        mType = type;
        mStatus = status;
        try {
            mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
        }
        catch (NumberFormatException e) {
            mDaemonId = 0L;
        }
        mIsRead = Integer.parseInt(isRead);
        mExtraFlag = extraFlag;
    }

    static int compare(Interaction a, Interaction b) {
        if (a == null)
            return b == null ? 0 : -1;
        if (b == null) return 1;
        return Long.compare(a.getTimestamp(), b.getTimestamp());
    }

    public String getAccount() {
        return mAccount;
    }

    public void setAccount(String account) {
        mAccount = account;
    }

    public int getId() {
        return mId;
    }

    public void read() {
        mIsRead = 1;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public ConversationHistory getConversation() {
        return mConversation;
    }

    public void setConversation(ConversationHistory conversation) {
        mConversation = conversation;
    }

    public Long getTimestamp() {
        return mTimestamp;
    }

    public String getBody() {
        return mBody;
    }

    public InteractionType getType() {
        return InteractionType.fromString(mType);
    }

    public void setType(InteractionType type) {
        mType = type.toString();
    }

    public InteractionStatus getStatus() {
        return InteractionStatus.fromString(mStatus);
    }

    public void setStatus(InteractionStatus status) {
        if (status == InteractionStatus.DISPLAYED)
            mIsRead = 1;
        mStatus = status.toString();
    }

    JsonObject getExtraFlag() {
        return toJson(mExtraFlag);
    }

    JsonObject toJson(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    String fromJson(JsonObject json) {
        return json.toString();
    }

    public Long getDaemonId() {
        return mDaemonId;
    }

    public String getDaemonIdString() {
        return mDaemonId == null ? null : Long.toString(mDaemonId);
    }

    public void setDaemonId(long daemonId) {
        mDaemonId = daemonId;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public String getConversationId() {
        return mConversationId;
    }

    public String getParentId() {
        return mParentId;
    }

    public boolean isIncoming() {
        return mIsIncoming;
    }

    public boolean isRead() {
        return mIsRead == 1;
    }

    public Contact getContact() {
        return mContact;
    }

    public void setContact(Contact contact) {
        mContact = contact;
    }

    public void setSwarmInfo(String conversationId) {
        mConversationId = conversationId;
        mMessageId = null;
        mParentId = null;
    }
    public void setSwarmInfo(String conversationId, String messageId, String parent) {
        mConversationId = conversationId;
        mMessageId = messageId;
        mParentId = parent;
    }

    public enum InteractionStatus {
        UNKNOWN, SENDING, SUCCESS, DISPLAYED, INVALID, FAILURE,

        TRANSFER_CREATED,
        TRANSFER_ACCEPTED,
        TRANSFER_CANCELED,
        TRANSFER_ERROR,
        TRANSFER_UNJOINABLE_PEER,
        TRANSFER_ONGOING,
        TRANSFER_AWAITING_PEER,
        TRANSFER_AWAITING_HOST,
        TRANSFER_TIMEOUT_EXPIRED,
        TRANSFER_FINISHED,
        FILE_AVAILABLE;

        static InteractionStatus fromString(String str) {
            for (InteractionStatus s : values()) {
                if (s.name().equals(str)) {
                    return s;
                }
            }
            return INVALID;
        }

        public static InteractionStatus fromIntTextMessage(int n) {
            try {
                return values()[n];
            } catch (ArrayIndexOutOfBoundsException e) {
                return INVALID;
            }
        }

        public static InteractionStatus fromIntFile(int n) {
            switch (n) {
                case 0:
                    return INVALID;
                case 1:
                    return TRANSFER_CREATED;
                case 2:
                case 9:
                    return TRANSFER_ERROR;
                case 3:
                    return TRANSFER_AWAITING_PEER;
                case 4:
                    return TRANSFER_AWAITING_HOST;
                case 5:
                    return TRANSFER_ONGOING;
                case 6:
                    return TRANSFER_FINISHED;
                case 7:
                case 8:
                case 10:
                    return TRANSFER_UNJOINABLE_PEER;
                case 11:
                    return TRANSFER_TIMEOUT_EXPIRED;
                default:
                    return UNKNOWN;
            }
        }

        public boolean isError() {
            return this == TRANSFER_ERROR || this == TRANSFER_UNJOINABLE_PEER || this == TRANSFER_CANCELED || this == TRANSFER_TIMEOUT_EXPIRED || this == FAILURE;
        }

        public boolean isOver() {
            return isError() || this == TRANSFER_FINISHED;
        }

    }

    public enum InteractionType {
        INVALID,
        TEXT,
        CALL,
        CONTACT,
        DATA_TRANSFER;

        static InteractionType fromString(String str) {
            for (InteractionType type : values()) {
                if (type.name().equals(str)) {
                    return type;
                }
            }
            return INVALID;
        }

    }
}
