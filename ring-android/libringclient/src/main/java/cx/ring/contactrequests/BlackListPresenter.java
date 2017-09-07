/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class BlackListPresenter extends RootPresenter<BlackListView> implements Observer<ServiceEvent> {
    static private final String TAG = BlackListPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private String mAccountID;

    @Inject
    public BlackListPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(BlackListView view) {
        mAccountService.addObserver(this);
        super.bindView(view);
        updateList();
    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
    }

    public void updateList() {
        if (getView() == null) {
            return;
        }

        Account account = mAccountService.getAccount(mAccountID);
        if (account == null) {
            return;
        }

        List<CallContact> list = account.getBannedContacts();
        if(list.isEmpty()) {
            getView().hideListView();
            getView().displayEmptyListMessage(true);
        } else {
            getView().updateView(list);
            getView().displayEmptyListMessage(false);
        }
    }

    public void setAccountId(String accountID) {
        if (getView() == null) {
            return;
        }
        mAccountID = accountID;
        updateList();
    }

    public void unblockClicked(CallContact contact) {
        String contactId = contact.getPhones().get(0).getNumber().getRawRingId();
        mAccountService.addContact(mAccountID, contactId);
        updateList();
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
        Log.d(TAG, "update " + event.getEventType());

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case CONTACT_ADDED:
            case CONTACT_REMOVED:
                updateList();
                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
