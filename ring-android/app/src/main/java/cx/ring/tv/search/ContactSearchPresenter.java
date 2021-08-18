/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.search;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.HardwareService;
import net.jami.services.VCardService;
import net.jami.smartlist.SmartListViewModel;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class ContactSearchPresenter extends RootPresenter<ContactSearchView> {

    private final AccountService mAccountService;

    private Contact mContact;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private final PublishSubject<String> contactQuery = PublishSubject.create();

    @Inject
    public ContactSearchPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(ContactSearchView view) {
        super.bindView(view);
        mCompositeDisposable.add(contactQuery
                .debounce(350, TimeUnit.MILLISECONDS)
                .switchMapSingle(q -> mAccountService.findRegistrationByName(mAccountService.getCurrentAccount().getAccountID(), "", q))
                .observeOn(mUiScheduler)
                .subscribe(q -> parseEventState(mAccountService.getAccount(q.getAccountId()), q.getName(), q.getAddress(), q.getState())));
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    public void queryTextChanged(String query) {
        if (query.equals("")) {
            getView().clearSearch();
        } else {
            Account currentAccount = mAccountService.getCurrentAccount();
            if (currentAccount == null) {
                return;
            }

            Uri uri = Uri.fromString(query);
            if (uri.isHexId()) {
                mContact = currentAccount.getContactFromCache(uri);
                getView().displayContact(currentAccount.getAccountID(), mContact);
            } else {
                getView().clearSearch();
                contactQuery.onNext(query);
            }
        }
    }

    private void parseEventState(Account account, String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                mContact = account.getContactFromCache(address);
                mContact.setUsername(name);
                getView().displayContact(account.getAccountID(), mContact);
                break;
            case 1:
                // invalid name
                Uri uriName = Uri.fromString(name);
                if (uriName.isHexId()) {
                    mContact = account.getContactFromCache(uriName);
                    getView().displayContact(account.getAccountID(), mContact);
                } else {
                    getView().clearSearch();
                }
                break;
            default:
                // on error
                Uri uriAddress = Uri.fromString(address);
                if (uriAddress.isHexId()) {
                    mContact = account.getContactFromCache(uriAddress);
                    getView().displayContact(account.getAccountID(), mContact);
                } else {
                    getView().clearSearch();
                }
                break;
        }
    }

    public void contactClicked(SmartListViewModel model) {
        getView().displayContactDetails(model);
    }
}