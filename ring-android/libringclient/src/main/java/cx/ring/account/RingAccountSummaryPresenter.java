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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.account;

import javax.inject.Inject;

import cx.ring.daemon.StringMap;
import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class RingAccountSummaryPresenter extends RootPresenter<RingAccountSummaryView> implements Observer<ServiceEvent> {

    private static final String TAG = RingAccountSummaryPresenter.class.getSimpleName();

    private static final int PIN_GENERATION_SUCCESS = 0;
    private static final int PIN_GENERATION_WRONG_PASSWORD = 1;
    private static final int PIN_GENERATION_NETWORK_ERROR = 2;

    @Inject
    AccountService mAccountService;

    private String mAccountID;

    @Override
    public void bindView(RingAccountSummaryView view) {
        mAccountService.addObserver(this);
        super.bindView(view);
    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
    }

    @Override
    public void afterInjection() {

    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || getView() == null) {
            return;
        }

        switch (event.getEventType()) {
            case KNOWN_DEVICES_CHANGED:
                handleKnownDevices(event);
                break;
            case REGISTRATION_STATE_CHANGED:
            case NAME_REGISTRATION_ENDED:
                getView().accountChanged(mAccountService.getAccount(mAccountID));
                break;
            case EXPORT_ON_RING_ENDED:
                handleExportEnded(event);
                break;
            default:
                Log.d(TAG, "This event " + event.getEventType() + " is not handled here");
                break;
        }
    }

    private void handleExportEnded(ServiceEvent event) {
        String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
        Account currentAccount = mAccountService.getAccount(mAccountID);
        if (currentAccount == null || !mAccountID.equals(accountId) || getView() == null) {
            return;
        }

        final int code = event.getEventInput(ServiceEvent.EventInput.CODE, Integer.class);
        final String pin = event.getEventInput(ServiceEvent.EventInput.PIN, String.class);

        switch (code) {
            case PIN_GENERATION_SUCCESS:
                getView().showPIN(pin);
                return;
            case PIN_GENERATION_WRONG_PASSWORD:
                getView().showPasswordError();
                return;
            case PIN_GENERATION_NETWORK_ERROR:
                getView().showNetworkError();
                break;
            default:
                getView().showGenericError();
                break;
        }
    }

    private void handleKnownDevices(ServiceEvent event) {
        String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
        Account currentAccount = mAccountService.getAccount(mAccountID);
        if (currentAccount == null || !mAccountID.equals(accountId) || getView() == null) {
            return;
        }
        final StringMap devices = event.getEventInput(ServiceEvent.EventInput.DEVICES, StringMap.class);
        getView().updateDeviceList(devices.toNative());
    }

    public void registerName(String name, String password) {
        final Account account = mAccountService.getAccount(mAccountID);
        if (account == null || getView() == null) {
            return;
        }
        mAccountService.registerName(account, password, name);
        getView().accountChanged(account);
    }

    public void startAccountExport(String password) {
        if (getView() == null) {
            return;
        }

        getView().showExportingProgressDialog();
        // TODO: this may need to be in the Application Executor
        mAccountService.exportOnRing(mAccountID, password);
    }

    public void setAccountId(String accountID) {
        if (getView() == null) {
            return;
        }
        mAccountID = accountID;
        getView().accountChanged(mAccountService.getAccount(mAccountID));
    }

    public void enableAccount(boolean newValue) {
        Account account = mAccountService.getAccount(mAccountID);
        if (account == null) {
            Log.w(TAG, "account not found!");
            return;
        }

        account.setEnabled(newValue);
        mAccountService.setCredentials(account.getAccountID(), account.getCredentialsHashMapList());
        mAccountService.setAccountDetails(account.getAccountID(), account.getDetails());
    }

}
