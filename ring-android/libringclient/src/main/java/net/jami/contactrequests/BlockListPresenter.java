/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package net.jami.contactrequests;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.utils.Log;
import io.reactivex.Scheduler;

public class BlockListPresenter extends RootPresenter<BlockListView> {
    static private final String TAG = BlockListPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private String mAccountID;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public BlockListPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(BlockListView view) {
        super.bindView(view);
        if (mAccountID != null) {
            setAccountId(mAccountID);
        }
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    private void updateList(Collection<Contact> list) {
        if (getView() == null) {
            return;
        }
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
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mAccountService
                .getAccountSingle(accountID)
                .flatMapObservable(Account::getBannedContactsUpdates)
                .observeOn(mUiScheduler)
                .subscribe(this::updateList, e -> Log.e(TAG, "Error showing blacklist", e)));
        mAccountID = accountID;
    }

    public void unblockClicked(Contact contact) {
        String contactId = contact.getUri().getRawRingId();
        mAccountService.addContact(mAccountID, contactId);
    }
}
