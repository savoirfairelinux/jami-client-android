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

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class AccountEditionPresenter extends RootPresenter<AccountEditionView> implements Observer<ServiceEvent> {

    private static final String TAG = RingAccountSummaryPresenter.class.getSimpleName();

    protected AccountService mAccountService;

    private Account mAccount;

    @Inject
    public AccountEditionPresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void update(Observable o, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTRATION_STATE_CHANGED:
            case ACCOUNTS_CHANGED:
                // refresh the selected account
                getView().displayAccountName(mAccount.getAlias());

                break;
            default:
                Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    @Override
    public void bindView(AccountEditionView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    public void init(String accountId) {

        if (accountId == null && getView() != null) {
            getView().exit();
            return;
        }
        mAccount = mAccountService.getAccount(accountId);
        getView().displayAccountName(mAccount.getAlias());

        if (mAccount.isRing()) {
            getView().displaySummary(mAccount.getAccountID());
        }
        getView().initViewPager(mAccount.getAccountID(), mAccount.isRing());
    }

    public void goToBlackList() {
        getView().goToBlackList(mAccount.getAccountID());
    }

    public void removeAccount() {
        mAccountService.removeAccount(mAccount.getAccountID());
        getView().exit();
    }

    public void prepareOptionsMenu() {
        if (getView() != null) {
            getView().showAdvancedOption(mAccount.isRing());
            getView().showBlacklistOption(mAccount.isRing());
        }
    }
}
