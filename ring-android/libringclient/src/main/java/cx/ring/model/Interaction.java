package cx.ring.model;

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
    protected boolean mIsIncoming;

    @DatabaseField(generatedId = true, columnName = COLUMN_ID, index = true)
    Integer mId;
    @DatabaseField(columnName = COLUMN_AUTHOR, index = true)
    String mAuthor;
    @DatabaseField(columnName = COLUMN_CONVERSATION, foreignColumnName = ConversationHistory.COLUMN_CONVERSATION_ID, foreign = true)
    ConversationHistory mConversation;
    @DatabaseField(columnName = COLUMN_TIMESTAMP, index = true)
    Long mTimestamp;
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

    /* Needed by ORMLite */
    public Interaction() {
    }

    public Interaction(String id, String author, ConversationHistory conversation, String timestamp, String body, String type, String status, String daemonId, String isRead, String extraFlag) {
        mId = Integer.parseInt(id);
        mAuthor = author;
        mConversation = conversation;
        mTimestamp = Long.parseLong(timestamp);
        mBody = body;
        mType = type;
        mStatus = status;
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
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

    public Integer getId() {
        return mId;
    }

    public void setId(Integer id) {
        mId = id;
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
        mStatus = status.toString();

    }

    JsonObject getExtraFlag() {
        return toJson(mExtraFlag);
    }

    JsonObject toJson(String value) {
        return new JsonParser().parse(value).getAsJsonObject();
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

    public void setDaemonId(Long daemonId) {
        mDaemonId = daemonId;
    }

    public void setDaemonId(String daemonId) {
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
    }

    public boolean isIncoming() {
        return mIsIncoming;
    }

    public boolean isRead() {
        return mIsRead == 1;


    }

    public enum InteractionStatus {
        UNKNOWN, SENDING, SUCCEEDED, INVALID, FAILED,

        TRANSFER_CREATED,
        TRANSFER_ACCEPTED,
        TRANSFER_CANCELED,
        TRANSFER_ERROR,
        TRANSFER_UNJOINABLE_PEER,
        TRANSFER_ONGOING,
        TRANSFER_AWAITING_PEER,
        TRANSFER_AWAITING_HOST,
        TRANSFER_TIMEOUT_EXPIRED,
        TRANSFER_FINISHED;

        static InteractionStatus fromString(String str) {
            for (InteractionStatus s : values()) {
                if (s.name().equals(str)) {
                    return s;
                }
            }
            return INVALID;
        }

        static InteractionStatus fromIntTextMessage(int n) {
            try {
                if (n == 3) {
                    return SUCCEEDED;
                } else
                    return values()[n];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        // todo cleanup
        public static InteractionStatus fromIntFile(int n) {
            switch (n) {
                case 0:
                    return UNKNOWN;
                case 1:
                    return TRANSFER_CREATED;
                case 2:
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
                    return TRANSFER_UNJOINABLE_PEER;
                case 8:
                    return TRANSFER_UNJOINABLE_PEER;
                case 9:
                    return TRANSFER_ERROR;
                case 10:
                    return TRANSFER_UNJOINABLE_PEER;
                case 11:
                    return TRANSFER_TIMEOUT_EXPIRED;
            }
            return UNKNOWN;
        }

        public boolean isError() {
            return this == TRANSFER_ERROR || this == TRANSFER_UNJOINABLE_PEER || this == TRANSFER_CANCELED || this == TRANSFER_TIMEOUT_EXPIRED;
        }

        public boolean isOver() {
            return isError() || this == TRANSFER_FINISHED;
        }

    }

    public enum InteractionType {
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
            return null;
        }

    }

}
