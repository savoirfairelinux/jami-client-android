/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.contactrequests;

import cx.ring.model.CallContact;
import ezvcard.VCard;

public class ContactRequestsViewModel {
    private String accountId;
    private CallContact mContact;
    private String mFullname;
    private String mUsername;
    private String mAccountUsername;
    private boolean hasPane;
    private String mContactId;
    long added;

    private final String mUuid;

    public ContactRequestsViewModel(String account, CallContact contact) {
        accountId = account;
        mContact = contact;
        mFullname = contact.getDisplayName();
        mUsername = contact.getRingUsername();
        hasPane = false;
        mAccountUsername = contact.getDisplayName();
        mContactId = contact.getPrimaryNumber();
        mUuid = contact.getPrimaryNumber();
    }

    public String getAccountUsername() {
        return mAccountUsername;
    }

    public VCard getVCard() {
        return mContact.vcard;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getFullname() {
        return mFullname;
    }

    public boolean hasPane() {
        return hasPane;
    }

    public String getContactId() {
        return mContactId;
    }

    public String getUuid() {
        return mUuid + "";
    }

    public String getAccountId() {
        return accountId;
    }

    public CallContact getContact() {
        return mContact;
    }
}
