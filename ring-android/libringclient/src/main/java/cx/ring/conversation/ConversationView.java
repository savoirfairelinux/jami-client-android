/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.conversation;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Uri;
import cx.ring.mvp.GenericView;
import cx.ring.utils.Tuple;

public interface ConversationView {

    void refreshView(Conversation conversation, Uri number);

    void updateView(String address, String name, int state);

    void displayContactName(String contactName);

    void displayOnGoingCallPane(boolean display);

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
