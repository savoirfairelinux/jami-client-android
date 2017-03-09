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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.services.ContactService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.TrustRequestUtils;
import cx.ring.utils.Tuple;
import ezvcard.VCard;

public class PendingContactRequestsPresenter extends RootPresenter<PendingContactRequestsView> implements Observer<ServiceEvent> {

    static private final String TAG = PendingContactRequestsPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private NotificationService mNotificationService;
    private ContactService mContactService;
    private String mAccountID;
    private ArrayList<PendingContactRequestsViewModel> mContactRequestsViewModels;

    @Inject
    public PendingContactRequestsPresenter(AccountService accountService,
                                           NotificationService notificationService,
                                           ContactService contactService) {
        mAccountService = accountService;
        mNotificationService = notificationService;
        mContactService = contactService;
    }

    final private List<TrustRequest> mTrustRequests = new ArrayList<>();
    final private List<TrustRequest> mTrustRequestsTmp = new ArrayList<>();

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(PendingContactRequestsView view) {
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
        boolean hasPane = !currentAccount.equals(mAccountService.getCurrentAccount());


        if (clear) {
            mTrustRequests.clear();
            mTrustRequestsTmp.clear();
            ArrayList<Map<String, String>> list = mAccountService.getTrustRequests(currentAccount.getAccountID()).toNative();
            String accountId = currentAccount.getAccountID();

            for (Map<String, String> request : list) {
                String contactId = request.get("from");
                String timestamp = request.get("received");
                String payload = request.get("payload");
                TrustRequest trustRequest = new TrustRequest(accountId, contactId);
                trustRequest.setTimestamp(timestamp);
                Tuple<VCard, String> tuple = TrustRequestUtils.parsePayload(payload);
                trustRequest.setVCard(tuple.first);
                trustRequest.setMessage(tuple.second);
                mTrustRequestsTmp.add(trustRequest);
                mAccountService.lookupAddress("", "", contactId);
            }
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

    public void acceptTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        mAccountService.acceptTrustRequest(accountId, viewModel.getContactId());
        updateList(true);
    }

    public void refuseTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        mAccountService.discardTrustRequest(accountId, viewModel.getContactId());
        updateList(true);
    }

    public void blockTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        String contactId = viewModel.getContactId();
        mAccountService.discardTrustRequest(accountId, contactId);
        mContactService.removeContact(accountId, contactId);
        updateList(true);
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
                Log.d(TAG, "update, accountId: " + mAccountService.getCurrentAccount().getAccountID());
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
