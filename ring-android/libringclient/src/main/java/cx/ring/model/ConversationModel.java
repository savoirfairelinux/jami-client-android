package cx.ring.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = ConversationModel.TABLE_NAME)
public class ConversationModel implements Serializable {

    public static final String TABLE_NAME = "conversation";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ACCOUNT_ID_NAME = "accountID";
    public static final String COLUMN_CONTACT_ID_NAME = "contactID";

    @DatabaseField(index = true, columnName = COLUMN_ID_NAME, generatedId = true)
    private long id;

    @DatabaseField(columnName = COLUMN_ACCOUNT_ID_NAME)
    private String accountId;

    @DatabaseField(columnName = COLUMN_CONTACT_ID_NAME)
    private String contactId;

    /* Needed by ORMLite */
    public ConversationModel() {
    }

    public ConversationModel(Conversation conversation) {
        accountId = conversation.getLastAccountUsed();
        contactId = conversation.getContact().getDisplayName();
    }

    public ConversationModel(String accountId, String contactId) {
        this.accountId = accountId;
        this.contactId = contactId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getContactId() {
        return contactId;
    }

    public long getId() {
        return id;
    }
}
