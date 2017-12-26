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
package cx.ring.smartlist;

import java.util.ArrayList;

import cx.ring.model.CallContact;
import cx.ring.mvp.BaseView;

public interface SmartListView extends BaseView {

    void displayNetworkErrorPanel();

    void displayMobileDataPanel();

    void displayNewContactRowWithName(String name);

    void displayChooseNumberDialog(CharSequence numbers[]);

    void displayNoConversationMessage();

    void displayConversationDialog(SmartListViewModel smartListViewModel);

    void displayDeleteDialog(CallContact callContact);

    void copyNumber(CallContact callContact);

    void setLoading(boolean display);

    void displayMenuItem();

    void hideSearchRow();

    void hideErrorPanel();

    void hideList();

    void hideNoConversationMessage();

    void updateList(ArrayList<SmartListViewModel> smartListViewModels);

    void goToConversation(String accountId, String contactId);

    void goToCallActivity(String accountId, String contactId);

    void goToQRActivity();

    void goToContact(CallContact callContact);

    void scrollToTop();
}
