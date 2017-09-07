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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

public class ContactRequestsPresenter extends RootPresenter<ContactRequestsView> implements Observer<ServiceEvent> {

    static private final String TAG = ContactRequestsPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private NotificationService mNotificationService;
    private DeviceRuntimeService mDeviceRuntimeService;
    private PreferencesService mPreferencesService;

    private String mAccountID;
    private ArrayList<PendingContactRequestsViewModel> mContactRequestsViewModels;

    @Inject
    public ContactRequestsPresenter(AccountService accountService,
                                    NotificationService notificationService,
                                    DeviceRuntimeService deviceRuntimeService,
                                    PreferencesService sharedPreferencesService) {
        mAccountService = accountService;
        mNotificationService = notificationService;
        mDeviceRuntimeService = deviceRuntimeService;
        mPreferencesService = sharedPreferencesService;
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

    public void acceptTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        String contactId = viewModel.getContactId();
        mAccountService.acceptTrustRequest(accountId, contactId);

        for (Iterator<TrustRequest> it = mTrustRequests.iterator(); it.hasNext(); ) {
            TrustRequest request = it.next();
            if (accountId.equals(request.getAccountId()) && contactId.equals(request.getContactId())) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, contactId + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
                it.remove();
            }
        }

        mPreferencesService.removeRequestPreferences(accountId, contactId);
        updateList(false);
    }

    public void refuseTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        String contactId = viewModel.getContactId();
        mAccountService.discardTrustRequest(accountId, contactId);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        updateList(true);
    }

    public void blockTrustRequest(PendingContactRequestsViewModel viewModel) {
        String accountId = mAccountID == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountID;
        String contactId = viewModel.getContactId();
        mAccountService.discardTrustRequest(accountId, contactId);
        mAccountService.removeContact(accountId, contactId, true);
        mPreferencesService.removeRequestPreferences(accountId, contactId);
        updateList(true);
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
