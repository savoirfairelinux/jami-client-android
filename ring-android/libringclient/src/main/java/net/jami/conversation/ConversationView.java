/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.conversation;

import java.io.File;
import java.util.List;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.Error;
import net.jami.model.Interaction;
import net.jami.model.DataTransfer;
import net.jami.model.Uri;
import net.jami.mvp.BaseView;

public interface ConversationView extends BaseView {

    void refreshView(List<Interaction> conversation);

    void scrollToEnd();

    void updateContact(Contact contact);

    void displayContact(Conversation conversation);

    void displayOnGoingCallPane(boolean display);

    void displayNumberSpinner(Conversation conversation, Uri number);

    void displayErrorToast(Error error);

    void hideNumberSpinner();

    void clearMsgEdit();

    void goToHome();

    void goToAddContact(Contact contact);

    void goToCallActivity(String conferenceId);

    void goToCallActivityWithResult(String accountId, Uri conversationUri, Uri contactUri, boolean audioOnly);

    void goToContactActivity(String accountId, Uri uri);

    void switchToUnknownView(String name);

    void switchToIncomingTrustRequestView(String message);

    void switchToConversationView();

    void switchToSyncingView();
    void switchToEndedView();

    void askWriteExternalStoragePermission();

    void openFilePicker();

    void acceptFile(String accountId, Uri conversationUri, DataTransfer transfer);
    void refuseFile(String accountId, Uri conversationUri, DataTransfer transfer);
    void shareFile(File path, String displayName);
    void openFile(File path, String displayName);

    void addElement(Interaction e);
    void updateElement(Interaction e);
    void removeElement(Interaction e);
    void setComposingStatus(Account.ComposingStatus composingStatus);
    void setLastDisplayed(Interaction interaction);

    void setConversationColor(int integer);
    void setConversationSymbol(CharSequence symbol);

    void startSaveFile(DataTransfer currentFile, String fileAbsolutePath);

    void startShareLocation(String accountId, String contactId);

    void showMap(String accountId, String contactId, boolean open);
    void hideMap();

    void showPluginListHandlers(String accountId, String peerId);

    void hideErrorPanel();

    void displayNetworkErrorPanel();

    void displayAccountOfflineErrorPanel();

    void setReadIndicatorStatus(boolean show);

    void updateLastRead(String last);
}
