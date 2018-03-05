/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> implements Observer<ServiceEvent> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private NotificationService mNotificationService;

    private String mAccountID;
    private ArrayList<PendingContactRequestsViewModel> mContactRequestsViewModels;

    @Inject
    public ContactRequestsPresenter(AccountService accountService,
                                    NotificationService notificationService) {
        mAccountService = accountService;
        mNotificationService = notificationService;
    }

    final private List<TrustRequest> mTrustRequests = new ArrayList<>();

    @Override
    public void bindView(ContactRequestsView view) {
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

    public void updateList(boolean clear) {
        if (getView() == null) {
            Log.d(TAG, "updateList: not able to get view");
            return;
        }

        Log.d(TAG, "updateList");
        Account currentAccount = ((mAccountID == null) ? mAccountService.getCurrentAccount() : mAccountService.getAccount(mAccountID));
        if (currentAccount == null) {
            return;
        }
        boolean hasPane = !currentAccount.equals(mAccountService.getCurrentAccount());

        if (clear) {
            mTrustRequests.clear();
            mTrustRequests.addAll(currentAccount.getRequests());
        }

        if (mContactRequestsViewModels == null) {
            mContactRequestsViewModels = new ArrayList<>();
        } else {
            mContactRequestsViewModels.clear();
        }
        for (TrustRequest request : mTrustRequests) {
            mContactRequestsViewModels.add(new PendingContactRequestsViewModel(currentAccount, request, hasPane));
        }

        getView().updateView(mContactRequestsViewModels);
        mNotificationService.cancelTrustRequestNotification(currentAccount.getAccountID());
    }

    public void contactRequestClicked(String contactId) {
        String rawUriString = new Uri(contactId).getRawUriString();
        getView().goToConversation(mAccountService.getCurrentAccount().getAccountID(), rawUriString);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
        Log.d(TAG, "update " + event.getEventType());

        switch (event.getEventType()) {
            case INCOMING_TRUST_REQUEST:
                updateList(false);
                break;
            case INCOMING_CALL:
            case INCOMING_MESSAGE:
            case ACCOUNTS_CHANGED:
                updateList(true);
                break;
            case REGISTERED_NAME_FOUND:
                updateList(false);
                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
