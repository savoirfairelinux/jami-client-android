package cx.ring.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = ConversationHistory.TABLE_NAME)
public class ConversationHistory {

    public static final String TABLE_NAME = "conversations";
    public static final String COLUMN_CONVERSATION_ID = "id";
    public static final String COLUMN_PARTICIPANT = "participant";
    public static final String COLUMN_EXTRA_FLAG = "extra_flag";

    @DatabaseField(generatedId = true , columnName = COLUMN_CONVERSATION_ID, canBeNull = false)
    Integer mId;
    @DatabaseField(columnName = COLUMN_PARTICIPANT, index = true)
    String mParticipant;
    @DatabaseField(columnName = COLUMN_EXTRA_FLAG)
    String mExtraFlag;

    String account;


    /* Needed by ORMLite */
    public ConversationHistory() {
    }

    public ConversationHistory(Conversation conversation) {
        mId = conversation.getId();
        mParticipant = conversation.getParticipant();
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
