/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class GeneralAccountPresenter extends RootPresenter<GeneralAccountView> implements Observer<ServiceEvent> {

    protected AccountService mAccountService;

    private Account mAccount;

    @Inject
    public GeneralAccountPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    @Override
    public void bindView(GeneralAccountView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    void init(String accountId) {
        mAccount = mAccountService.getAccount(accountId);
        if (mAccount != null) {
            if (mAccount.isRing()) {
                getView().addRingPreferences();
            } else {
                getView().addSIPPreferences();
            }
            getView().accountChanged(mAccount);
        }
    }

    void accountChanged(Object newValue) {
        mAccount.setEnabled((Boolean) newValue);
        updateAccount();
    }

    void twoStatePreferenceChanged(ConfigKey configKey, Object newValue) {
        mAccount.setDetail(configKey, newValue.toString());
        updateAccount();
    }

    void passwordPreferenceChanged(ConfigKey configKey, Object newValue) {
        if (mAccount.isSip()) {
            mAccount.getCredentials().get(0).setDetail(configKey, newValue.toString());
        }
        updateAccount();
    }

    void userNameChanged(ConfigKey configKey, Object newValue) {
        if (mAccount.isSip()) {
            mAccount.getCredentials().get(0).setDetail(configKey, newValue.toString());
        }
        updateAccount();
    }

    void preferenceChanged(ConfigKey configKey, Object newValue) {
        mAccount.setDetail(configKey, newValue.toString());
        updateAccount();
    }

    private void updateAccount() {
        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || getView() == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                getView().accountChanged(mAccount);
                break;
            default:
                break;
        }
    }
}
