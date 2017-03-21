package cx.ring.smartlist;

import java.util.ArrayList;
import java.util.Collection;

import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.GenericView;

/**
 * Created by hdsousa on 17-03-15.
 */

public interface SmartListView extends GenericView {

    void displayNetworkErrorPanel();

    void displayMobileDataPanel();

    void displayNewContactRowWithName(String name);

    void displayChooseNumberDialog(CharSequence numbers[]);

    void displayNoConversationMessage();

    void displayConversationDialog(Conversation conversation);

    void displayMenuItem();

    void hideSearchRow();

    void hideErrorPanel();

    void hideNoConversationMessage();

    void updateView(ArrayList<SmartListViewModel> list);

    void goToConversation(CallContact callContact);

    void goToCallActivity(String rawUriNumber);

    void goToQRActivity();

    void goToContact(CallContact callContact);
}
