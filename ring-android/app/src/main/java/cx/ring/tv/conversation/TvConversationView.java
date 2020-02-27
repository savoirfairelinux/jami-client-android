/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.tv.conversation;

import java.io.File;
import java.util.List;

import cx.ring.model.CallContact;
import cx.ring.model.DataTransfer;
import cx.ring.model.Error;
import cx.ring.model.Interaction;
import cx.ring.mvp.BaseView;

public interface TvConversationView extends BaseView {

    void refreshView(List<Interaction> conversation);

    void scrollToTop();

    void displayContact(CallContact contact);

    void displayErrorToast(Error error);

    void switchToUnknownView(String name);

    void switchToIncomingTrustRequestView(String message);

    void switchToConversationView();

    void addElement(Interaction e);

    void updateElement(Interaction e);

    void removeElement(Interaction e);

    void shareFile(File path);

    void openFile(File path);

    void startSaveFile(DataTransfer currentFile, String fileAbsolutePath);
}
