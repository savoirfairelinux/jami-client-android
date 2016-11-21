/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Uri;

public class StateService extends Observable {

    private Account mCurrentAccount;
    private Map<String, CallContact> mContacts;

    public StateService() {
        mContacts = new HashMap<>();
    }

    public Account getCurrentAccount() {
        return mCurrentAccount;
    }

    public void setCurrentAccount(Account currentAccount) {
        this.mCurrentAccount = currentAccount;
        setChanged();
        notifyObservers();
    }

    public void addContact(CallContact contact) {
        if (contact == null
                || contact.getPhones().isEmpty()
                || contact.getPhones().get(0) == null
                || contact.getPhones().get(0).getNumber() == null) {
            return;
        }
        mContacts.put(contact.getPhones().get(0).getNumber().toString(), contact);
    }

    public CallContact getContact(Uri uri) {
        return mContacts.get(uri.toString());
    }

}
