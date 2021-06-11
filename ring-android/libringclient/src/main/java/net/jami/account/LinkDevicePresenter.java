/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package net.jami.account;

import net.jami.services.AccountService;

import java.net.SocketException;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.model.Account;
import net.jami.mvp.RootPresenter;

import io.reactivex.rxjava3.core.Scheduler;

public class LinkDevicePresenter extends RootPresenter<LinkDeviceView> {

    private static final String TAG = LinkDevicePresenter.class.getSimpleName();

    private net.jami.services.AccountService mAccountService;
    private String mAccountID;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public LinkDevicePresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    public void startAccountExport(String password) {
        if (getView() == null) {
            return;
        }
        getView().showExportingProgress();
        mCompositeDisposable.add(mAccountService
                .exportOnRing(mAccountID, password)
                .observeOn(mUiScheduler)
                .subscribe(pin -> getView().showPIN(pin),
                           error -> {
                    getView().dismissExportingProgress();
                    if (error instanceof IllegalArgumentException) {
                        getView().showPasswordError();
                    } else if (error instanceof SocketException) {
                        getView().showNetworkError();
                    } else {
                        getView().showGenericError();
                    }
                }));
    }

    public void setAccountId(String accountID) {
        mCompositeDisposable.clear();
        mAccountID = accountID;
        LinkDeviceView v = getView();
        Account account = mAccountService.getAccount(mAccountID);
        if (v != null && account != null)
            v.accountChanged(account);
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(mAccountID)
                .observeOn(mUiScheduler)
                .subscribe(a -> {
                    LinkDeviceView view = getView();
                    if (view != null)
                        view.accountChanged(a);
                }));
    }

}
