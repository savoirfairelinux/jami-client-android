package cx.ring.conversation;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Uri;
import cx.ring.mvp.GenericView;
import cx.ring.utils.Tuple;

/**
 * Created by hdsousa on 17-03-21.
 */

public interface ConversationView extends GenericView {

    void refreshView(Conversation conversation, Uri number);

    void updateView(String address, String name, int state);

    void displayContactName(String contactName);

    void displayOnGoingCallPane(boolean display);

    void displayContactPhoto(byte[] photo);

    void displayNumberSpinner(Conversation conversation, Uri number);

    void displayAddContact(boolean display);

    void displayDeleteDialog(Conversation conversation);

    void displayCopyToClipboard(CallContact callContact);

    void displaySendTrustRequest(String accountId, String contactId);

    void hideNumberSpinner();

    void clearMsgEdit();

    void goToHome();

    void goToAddContact(CallContact callContact);

    void goToCallActivity(String conferenceId);

    void goToCallActivityWithResult(Tuple<Account, Uri> guess, boolean hasVideo);
}
