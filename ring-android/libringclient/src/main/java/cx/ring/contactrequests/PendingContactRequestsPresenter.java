/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class PendingContactRequestsPresenter extends RootPresenter<GenericView<PendingContactRequestsViewModel>> implements Observer<ServiceEvent> {

    static private final String TAG = PendingContactRequestsPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private String mAccountID;
    private NotificationService mNotificationService;

    @Inject
    public PendingContactRequestsPresenter(AccountService mAccountService, NotificationService mNotificationService) {
        this.mAccountService = mAccountService;
        this.mNotificationService = mNotificationService;
    }

    final private List<TrustRequest> mTrustRequests = new ArrayList<>();
    final private List<TrustRequest> mTrustRequestsTmp = new ArrayList<>();

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(GenericView<PendingContactRequestsViewModel> view) {
        mAccountService.addObserver(this);
        super.bindView(view);
        updateList(true);
    }

    public void updateAccount(String accountId, boolean shouldUpdateList) {
        mAccountID = accountId;
        if (shouldUpdateList) {
            updateList(true);
        }

    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
    }

    public void updateList(Boolean clear) {
        if (getView() == null) {
            return;
        }

        Log.d(TAG, "updateList");
        Account currentAccount = ((mAccountID == null) ? mAccountService.getCurrentAccount() : mAccountService.getAccount(mAccountID));
        if (currentAccount == null) {
            return;
        }

        if (clear) {
            mTrustRequests.clear();
            mTrustRequestsTmp.clear();
            HashMap<String, String> map = mAccountService.getTrustRequests(currentAccount.getAccountID()).toNative();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Log.d(TAG, "trust request: " + value + ", " + key);
                mTrustRequestsTmp.add(new TrustRequest(value, key));
                mAccountService.lookupAddress("", "", key);
            }
        }

        boolean hasPane = mAccountID != null;
        getView().showViewModel(new PendingContactRequestsViewModel(currentAccount, mTrustRequests, hasPane));
        mNotificationService.cancelTrustRequestNotification(currentAccount.getAccountID());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
        Log.d(TAG, "update " + event.getEventType());

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case INCOMING_TRUST_REQUEST:
                updateList(true);
                break;
            case REGISTERED_NAME_FOUND:
                Log.d(TAG, "update, accountID: " + mAccountService.getCurrentAccount().getAccountID());
                final String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                switch (state) {
                    case 0: //name found
                        for (TrustRequest trustRequest : mTrustRequestsTmp) {
                            if (trustRequest.getContactId().equals(address)) {
                                trustRequest.setUsername(name);
                                mTrustRequestsTmp.remove(trustRequest);
                                mTrustRequests.add(trustRequest);
                                updateList(false);
                                break;
                            }
                        }
                        break;
                    default:
                        for (TrustRequest trustRequest : mTrustRequestsTmp) {
                            if (trustRequest.getContactId().equals(address)) {
                                mTrustRequestsTmp.remove(trustRequest);
                                mTrustRequests.add(trustRequest);
                                updateList(false);
                                break;
                            }
                        }
                        break;
                }
                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
