package cx.ring.smartlist;

import java.util.Collection;

import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.GenericView;

/**
 * Created by hdsousa on 17-03-15.
 */

public interface SmartListView extends GenericView {

    void displayNewContactRowWithName(String name, String address);

    void hideSearchRow();

    void updateView(final Collection<Conversation> list, String query);

    void goToConversation(CallContact callContact);
}
