/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
package cx.ring.search;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;


public class RingSearchPresenter extends RootPresenter<RingSearchView> implements Observer<ServiceEvent> {

    private static final String TAG = RingSearchPresenter.class.getSimpleName();

    AccountService mAccountService;

    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLastBlockchainQuery = null;
    private CallContact mCallContact;

    @Inject
    public RingSearchPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    @Override
    public void bindView(RingSearchView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                if (mLastBlockchainQuery != null
                        && (mLastBlockchainQuery.equals("") || !mLastBlockchainQuery.equals(name))) {
                    return;
                }
                String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                parseEventState(name, address, state);
                break;
        }
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

                mLastBlockchainQuery = query;
                mNameLookupInputHandler.enqueueNextLookup(query);
            }

        }
    }

    private void parseEventState(String name, String address, int state) {
        switch (state) {
            case 0:
                // on found
                if (mLastBlockchainQuery != null && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildRingContact(new Uri(address), name);
                    getView().displayContact(mCallContact);
                    mLastBlockchainQuery = null;
                }
                break;
            case 1:
                // invalid name
                Uri uriName = new Uri(name);
                if (uriName.isRingId()
                        && mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)) {
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
                        && mLastBlockchainQuery != null
                        && mLastBlockchainQuery.equals(name)) {
                    mCallContact = CallContact.buildUnknown(name, address);
                    getView().displayContact(mCallContact);
                } else {
                    getView().clearSearch();
                }
                break;
        }
    }
}