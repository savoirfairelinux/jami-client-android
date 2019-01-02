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
        final AccountEditionView view = getView();
        mAccount = accountId == null ? null : mAccountService.getAccount(accountId);
        if (mAccount == null) {
            if (view != null)
                view.exit();
            return;
        }
        view.displayAccountName(mAccount.getAlias());
        if (mAccount.isRing()) {
            view.displaySummary(mAccount.getAccountID());
        }
        view.initViewPager(mAccount.getAccountID(), mAccount.isRing());
        mCompositeDisposable.add(mAccountService.getObservableAccount(accountId)
                .observeOn(mUiScheduler)
                .subscribe(account -> getView().displayAccountName(account.getAlias())));
    }

    public void goToBlackList() {
        getView().goToBlackList(mAccount.getAccountID());
    }

    public void removeAccount() {
        mAccountService.removeAccount(mAccount.getAccountID());

        if (mAccountService.getAccountList().size() == 0) {
            getView().goToWizardActivity();
        } else {
            getView().exit();
        }
    }

    public void prepareOptionsMenu() {
        if (getView() != null) {
            getView().showAdvancedOption(mAccount.isRing());
            getView().showBlacklistOption(mAccount.isRing());
        }
    }
}
