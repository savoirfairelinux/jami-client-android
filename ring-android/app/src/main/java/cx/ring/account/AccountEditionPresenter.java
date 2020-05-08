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

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;

public class AccountEditionPresenter extends RootPresenter<AccountEditionView> {
    private final AccountService mAccountService;
    private final Scheduler mUiScheduler;

    private Account mAccount;

    @Inject
    public AccountEditionPresenter(AccountService accountService, @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mUiScheduler = uiScheduler;
    }

    public void init(String accountId) {
        init(mAccountService.getAccount(accountId));
        mCompositeDisposable.add(mAccountService
                .getCurrentAccountSubject()
                .observeOn(mUiScheduler)
                .subscribe(a -> {
                    if (mAccount != a) {
                        init(a);
                    }
                }));
    }

    public void init(Account account) {
        final AccountEditionView view = getView();
        if (account == null) {
            if (view != null)
                view.exit();
            return;
        }
        mAccount = account;
        if (account.isJami()) {
            view.displaySummary(account.getAccountID());
        } else {
            view.displaySIPView(account.getAccountID());
        }
        view.initViewPager(account.getAccountID(), account.isJami());
    }

    public void goToBlackList() {
        getView().goToBlackList(mAccount.getAccountID());
    }

    public void removeAccount() {
        mAccountService.removeAccount(mAccount.getAccountID());
    }

    public void prepareOptionsMenu() {
        if (mAccount != null)
            prepareOptionsMenu(mAccount.isJami());
    }

    public void prepareOptionsMenu(boolean isJami) {
        AccountEditionView view = getView();
        if (view != null) {
            view.showAdvancedOption(isJami);
            view.showBlacklistOption(isJami);
        }
    }
}
