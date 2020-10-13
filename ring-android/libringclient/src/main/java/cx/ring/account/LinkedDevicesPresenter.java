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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.account;

import java.net.SocketException;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;
import io.reactivex.Scheduler;

public class LinkedDevicesPresenter extends RootPresenter<LinkedDevicesView> {

    private static final String TAG = LinkedDevicesPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private String mAccountID;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public LinkedDevicesPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    public void setAccountId(String accountID) {
        mCompositeDisposable.clear();
        mAccountID = accountID;
        LinkedDevicesView v = getView();
        Account account = mAccountService.getAccount(mAccountID);
        if (v != null && account != null)
            v.accountChanged(account);
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(mAccountID)
                .observeOn(mUiScheduler)
                .subscribe(a -> {
                    LinkedDevicesView view = getView();
                    if (view != null)
                        view.accountChanged(a);
                }));
    }

    public void revokeDevice(final String deviceId, String password) {
        if (getView() != null) {
            getView().showRevokingProgressDialog();
        }
        mCompositeDisposable.add(mAccountService
                .revokeDevice(mAccountID, password, deviceId)
                .observeOn(mUiScheduler)
                .subscribe(result -> {
                    getView().deviceRevocationEnded(deviceId, result);
                    getView().updateDeviceList(mAccountService.getAccount(mAccountID).getDevices(), mAccountID);
                }));
    }

    public void renameDevice(String newName) {
        mAccountService.renameDevice(mAccountID, newName);
    }

    public String getDeviceName() {
        Account account = mAccountService.getAccount(mAccountID);
        if (account == null) {
            Log.w(TAG, "account not found!");
            return null;
        }
        return account.getDeviceName();
    }

}
