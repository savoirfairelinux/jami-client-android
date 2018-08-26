/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import java.io.File;
import java.util.List;

import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransfer;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseView;

public interface ConversationView extends BaseView {

    void refreshView(List<ConversationElement> conversation);

    void scrollToEnd();

    void displayContact(CallContact contact);

    void displayOnGoingCallPane(boolean display);

    void displayNumberSpinner(Conversation conversation, Uri number);

    void displayDeleteDialog(Conversation conversation);

    void displayCopyToClipboard(CallContact callContact);

    void displayErrorToast(int error);

    void displayCompletedDownload(DataTransfer transfer, File destination);

    void hideNumberSpinner();

    void clearMsgEdit();

    void goToHome();

    void goToAddContact(CallContact callContact);

    void goToCallActivity(String conferenceId);

    void goToCallActivityWithResult(String accountId, String contactRingId, boolean audioOnly);

    void switchToUnknownView(String name);

    void switchToIncomingTrustRequestView(String message);

    void switchToConversationView();

    void askWriteExternalStoragePermission();

    void openFilePicker();

    void shareFile(File path);

    void addElement(ConversationElement e);
    void updateElement(ConversationElement e);
    void removeElement(ConversationElement e);
}
