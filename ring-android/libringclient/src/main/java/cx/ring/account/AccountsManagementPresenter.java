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

package cx.ring.account;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;
import io.reactivex.observers.DisposableObserver;

public class AccountsManagementPresenter extends RootPresenter<AccountsManagementView> {
    private AccountService mAccountService;
    private DisposableObserver<List<Account>> accountsObserver = null;
    private DisposableObserver<Account> accountObserver = null;
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
        accountsObserver = mAccountService.getObservableAccountList()
                .observeOn(mUiScheduler)
                .subscribeWith(new DisposableObserver<List<Account>>() {
            @Override
            public void onNext(List<Account> accounts) {
                getView().refresh(accounts);
            }
            @Override
            public void onError(Throwable e) {}
            @Override
            public void onComplete() {}
        });
        accountObserver = mAccountService.getObservableAccounts()
                .observeOn(mUiScheduler)
                .subscribeWith(new DisposableObserver<Account>() {
            @Override
            public void onNext(Account account) {
                getView().refreshAccount(account);
            }
            @Override
            public void onError(Throwable e) {}
            @Override
            public void onComplete() {}
        });
    }

    @Override
    public void unbindView() {
        super.unbindView();
        if (accountsObserver != null) {
            accountsObserver.dispose();
            accountsObserver = null;
        }
        if (accountObserver != null) {
            accountObserver.dispose();
            accountObserver = null;
        }
    }

    public void clickAccount(Account account) {
        if (account.needsMigration()) {
            getView().launchAccountMigrationActivity(account);
        } else {
            getView().launchAccountEditActivity(account);
        }
    }

    public void refresh() {}

    public void addClicked() {
        getView().launchWizardActivity();
    }

    public void itemClicked(String accountId, Map<String, String> details) {
        mAccountService.setAccountDetails(accountId, details);
    }
}
