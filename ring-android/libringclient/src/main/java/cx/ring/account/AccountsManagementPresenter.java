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

package cx.ring.account;

import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class AccountsManagementPresenter extends RootPresenter<AccountsManagementView> implements Observer<ServiceEvent> {

    private static final String TAG = AccountsManagementPresenter.class.getSimpleName();

    private AccountService mAccountService;

    @Inject
    public AccountsManagementPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(AccountsManagementView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        view.refresh(mAccountService.getAccounts());
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    public void clickAccount(Account account) {
        if (account.needsMigration()) {
            getView().launchAccountMigrationActivity(account);
        } else {
            getView().launchAccountEditActivity(account);
        }
    }

    public void refresh() {
        getView().refresh(mAccountService.getAccounts());
    }

    public void addClicked() {
        getView().launchWizardActivity();
    }

    public void itemClicked(String accountId, Map<String, String> details) {
        mAccountService.setAccountDetails(accountId, details);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                getView().refresh(mAccountService.getAccounts());
                break;
            default:
                Log.d(TAG, "This event is not handled here");
                break;
        }
    }
}
