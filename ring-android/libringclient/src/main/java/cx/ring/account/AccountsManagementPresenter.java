/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

package cx.ring.account;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;

public class AccountsManagementPresenter extends RootPresenter<AccountsManagementView> {
    private AccountService mAccountService;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public AccountsManagementPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(AccountsManagementView view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService.getObservableAccountList()
                .observeOn(mUiScheduler)
                .subscribe(accounts -> getView().refresh(accounts)));
        mCompositeDisposable.add(mAccountService.getObservableAccounts()
                .observeOn(mUiScheduler)
                .subscribe(account -> getView().refreshAccount(account)));
    }

    public void clickAccount(Account account) {
        if (account.needsMigration()) {
            getView().launchAccountMigrationActivity(account);
        } else {
            getView().launchAccountEditActivity(account);
        }
    }

    public void addClicked() {
        getView().launchWizardActivity();
    }

    public void accountEnabled(String accountId, boolean enabled) {
        mAccountService.setAccountEnabled(accountId, enabled);
    }
}
