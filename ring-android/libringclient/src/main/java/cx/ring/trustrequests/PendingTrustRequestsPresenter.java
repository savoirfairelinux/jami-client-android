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

package cx.ring.trustrequests;

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
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class PendingTrustRequestsPresenter extends RootPresenter<GenericView<PendingTrustRequestsViewModel>> implements Observer<ServiceEvent> {

    static final String TAG = PendingTrustRequestsPresenter.class.getSimpleName();

    private String mAccountID;

    @Inject
    AccountService mAccountService;

    @Override
    public void afterInjection() {
        mAccountService.addObserver(this);
    }

    @Override
    public void bindView(GenericView<PendingTrustRequestsViewModel> view) {
        mAccountService.addObserver(this);
        super.bindView(view);
        updateList();
    }

    public void updateAccount(String accountId) {
        mAccountID = accountId;
    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
    }

    public void updateList() {
        if (getView() == null) {
            Log.d(TAG, "updateList. GetView() is null");
            return;
        }

        Log.d(TAG, "updateList");
        Account currentAccount = ((mAccountID == null) ? mAccountService.getCurrentAccount() : mAccountService.getAccount(mAccountID));
        HashMap<String, String> map = mAccountService.getTrustRequests(currentAccount.getAccountID()).toNative();
        List<TrustRequest> trustRequests = new ArrayList<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Log.d(TAG, "trust request: " + value + ", " + key);
            trustRequests.add(new TrustRequest(value, key));
        }


        getView().showViewModel(new PendingTrustRequestsViewModel(currentAccount, trustRequests));
        mAccountID = null;
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        Log.d(TAG, "update " + event.getEventType());
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case INCOMING_TRUST_REQUEST:
                updateList();
                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
