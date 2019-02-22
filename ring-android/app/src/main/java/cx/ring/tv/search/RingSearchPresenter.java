/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import cx.ring.daemon.Blob;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.RingError;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.HardwareService;
import cx.ring.services.VCardService;
import cx.ring.utils.VCardUtils;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;

public class RingSearchPresenter extends RootPresenter<RingSearchView> {

    private final AccountService mAccountService;
    private final HardwareService mHardwareService;
    private final VCardService mVCardService;

    private CallContact mCallContact;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private final PublishSubject<String> contactQuery = PublishSubject.create();

    @Inject
    public RingSearchPresenter(AccountService accountService,
                               HardwareService hardwareService,
                               VCardService vCardService) {
        mAccountService = accountService;
        mHardwareService = hardwareService;
        mVCardService = vCardService;
    }

    @Override
    public void bindView(RingSearchView view) {
        super.bindView(view);
        mCompositeDisposable.add(contactQuery
                .debounce(350, TimeUnit.MILLISECONDS)
                .switchMapSingle(q -> mAccountService.findRegistrationByName(mAccountService.getCurrentAccount().getAccountID(), "", q))
                .observeOn(mUiScheduler)
                .subscribe(q -> parseEventState(mAccountService.getAccount(q.accountId), q.name, q.address, q.state)));
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

            Uri uri = new Uri(query);
            if (uri.isRingId()) {
                mCallContact = currentAccount.getContactFromCache(uri);
                getView().displayContact(currentAccount.getAccountID(), mCallContact);
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
                mCallContact = account.getContactFromCache(address);
                mCallContact.setUsername(name);
                getView().displayContact(account.getAccountID(), mCallContact);
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()) {
                    mCallContact = account.getContactFromCache(uriName);
                    getView().displayContact(account.getAccountID(), mCallContact);
                } else {
                    getView().clearSearch();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()) {
                    mCallContact = account.getContactFromCache(uriAddress);
                    getView().displayContact(account.getAccountID(), mCallContact);
                } else {
                    getView().clearSearch();
                }
                break;
        }
    }

    public void contactClicked(CallContact contact) {
        if (!mHardwareService.isVideoAvailable() && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(RingError.NO_INPUT);
            return;
        }

        String accountId = mAccountService.getCurrentAccount().getAccountID();

        if (contact.getStatus() == CallContact.Status.NO_REQUEST) {
            mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_REQUEST)
                    .subscribe(vCard -> {
                        mAccountService.sendTrustRequest(accountId,
                                contact.getPrimaryUri().getRawRingId(),
                                Blob.fromString(VCardUtils.vcardToString(vCard)));

                    });
        }

        getView().startCall(accountId, contact.getPrimaryNumber());
    }
}