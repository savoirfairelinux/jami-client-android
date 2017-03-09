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

import cx.ring.daemon.Blob;
import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.TrustRequestUtils;
import cx.ring.utils.Tuple;
import ezvcard.VCard;

public class PendingTrustRequestsPresenter extends RootPresenter<GenericView<PendingTrustRequestsViewModel>> implements Observer<ServiceEvent> {

    static final String TAG = PendingTrustRequestsPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private NotificationService mNotificationService;
    private DeviceRuntimeService mDeviceRuntimeService;

    private String mAccountID;

    @Inject
    public PendingTrustRequestsPresenter(AccountService accountService,
                                         NotificationService notificationService,
                                         DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mDeviceRuntimeService = deviceRuntimeService;
        mNotificationService = notificationService;
    }

    final private List<TrustRequest> mTrustRequests = new ArrayList<>();
    final private List<TrustRequest> mTrustRequestsTmp = new ArrayList<>();

    @Override
    public void afterInjection() {

    }

    @Override
    public void bindView(GenericView<PendingTrustRequestsViewModel> view) {
        mAccountService.addObserver(this);
        super.bindView(view);
        updateList(true);
    }

    public void updateAccount(String accountId) {
        mAccountID = accountId;
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
            String accountId = currentAccount.getAccountID();
            HashMap<String, String> map = mAccountService.getTrustRequests(accountId).toNative();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String contactId = entry.getKey();
                String timestamp = entry.getValue();
                TrustRequest trustRequest = TrustRequestUtils.loadFromDisk(accountId, contactId, mDeviceRuntimeService.provideFilesDir());
                if (trustRequest == null) {
                    trustRequest = new TrustRequest(accountId, contactId);
                }
                Log.d(TAG, "trust request: " + accountId + ", " + contactId);
                mTrustRequestsTmp.add(trustRequest);
                mAccountService.lookupAddress("", "", contactId);
            }
        }

        getView().showViewModel(new PendingTrustRequestsViewModel(currentAccount, mTrustRequests));
        if (clear) {
            mNotificationService.cancelTrustRequestNotification(currentAccount.getAccountID());
        }
        mAccountID = null;
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
        Log.d(TAG, "update " + event.getEventType());

        switch (event.getEventType()) {
            case INCOMING_TRUST_REQUEST:
                final String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                final String from = event.getEventInput(ServiceEvent.EventInput.FROM, String.class);
                final String payload = event.getEventInput(ServiceEvent.EventInput.MESSAGE, Blob.class).toJavaString();
                TrustRequest request = TrustRequestUtils.loadFromDisk(accountId, from, mDeviceRuntimeService.provideFilesDir());
                if (request == null) {
                    request = new TrustRequest(accountId, from);
                    Tuple<VCard, String> tuple = TrustRequestUtils.parsePayload(payload);
                    request.setVCard(tuple.first);
                    request.setMessage(tuple.second);
                }
                if (!mTrustRequestsTmp.contains(request) && !mTrustRequests.contains(request)) {
                    mTrustRequestsTmp.add(request);
                    mAccountService.lookupAddress("", "", from);
                    updateList(false);
                }
                break;
            case ACCOUNTS_CHANGED:
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
