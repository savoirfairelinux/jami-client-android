/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Scheduler;

public class RingSearchPresenter extends RootPresenter<RingSearchView> {

    private AccountService mAccountService;
    private HardwareService mHardwareService;
    private VCardService mVCardService;

    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLastQuery = null;
    private CallContact mCallContact;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public RingSearchPresenter(AccountService accountService,
                               HardwareService hardwareService,
                               VCardService vCardService) {
        this.mAccountService = accountService;
        this.mHardwareService = hardwareService;
        this.mVCardService = vCardService;
    }

    @Override
    public void bindView(RingSearchView view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService.getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> parseEventState(r.name, r.address, r.state)));
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
                mCallContact = CallContact.buildUnknown(uri);
                getView().displayContact(mCallContact);
            } else {
                getView().clearSearch();

                // Ring search
                if (mNameLookupInputHandler == null) {
                    mNameLookupInputHandler = new NameLookupInputHandler(mAccountService, currentAccount.getAccountID());
                }

                mLastQuery = query;
                mNameLookupInputHandler.enqueueNextLookup(query);
            }

        }
    }

    private void parseEventState(String name, String address, int state) {
        if (mLastQuery != null
                && (mLastQuery.equals("") || !mLastQuery.equals(name))) {
            return;
        }
        switch (state) {
            case 0:
                // on found
                if (mLastQuery != null && mLastQuery.equals(name)) {
                    mCallContact = CallContact.buildRingContact(new Uri(address), name);
                    getView().displayContact(mCallContact);
                    mLastQuery = null;
                }
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()
                        && mLastQuery != null
                        && mLastQuery.equals(name)) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
                } else {
                    getView().clearSearch();
                }
                break;
            default:
                // on error
                Uri uriAddress = new Uri(address);
                if (uriAddress.isRingId()
                        && mLastQuery != null
                        && mLastQuery.equals(name)) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
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