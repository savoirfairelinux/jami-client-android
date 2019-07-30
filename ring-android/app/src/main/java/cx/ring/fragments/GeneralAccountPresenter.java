/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;

public class GeneralAccountPresenter extends RootPresenter<GeneralAccountView> {

    private static final String TAG = GeneralAccountPresenter.class.getSimpleName();

    protected AccountService mAccountService;

    private Account mAccount;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public GeneralAccountPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    // Init with current account
    public void init() {
        Account currentAccount = mAccountService.getCurrentAccount();
        if (currentAccount == null) {
            Log.e(TAG, "init: No currentAccount available");
            return;
        }
        init(currentAccount.getAccountID());
    }

    void init(String accountId) {
        mAccount = mAccountService.getAccount(accountId);
        if (mAccount != null) {
            if (mAccount.isRing()) {
                getView().addRingPreferences();
            } else {
                getView().addSIPPreferences();
            }
            mCompositeDisposable.clear();
            mCompositeDisposable.add(mAccountService.getObservableAccount(accountId)
                .observeOn(mUiScheduler)
                .subscribe(account -> getView().accountChanged(account)));
        }
    }

    void setEnabled(Object newValue) {
        boolean enabled = (Boolean) newValue;
        mAccount.setEnabled(enabled);
        mAccountService.setAccountEnabled(mAccount.getAccountID(), enabled);
    }

    public void twoStatePreferenceChanged(ConfigKey configKey, Object newValue) {
        if (configKey == ConfigKey.ACCOUNT_ENABLE) {
            setEnabled(newValue);
        } else {
            mAccount.setDetail(configKey, newValue.toString());
            updateAccount();
        }
    }

    void passwordPreferenceChanged(ConfigKey configKey, Object newValue) {
        if (mAccount.isSip()) {
            mAccount.getCredentials().get(0).setDetail(configKey, newValue.toString());
        }
        updateAccount();
    }

    void userNameChanged(ConfigKey configKey, Object newValue) {
        if (mAccount.isSip()) {
            mAccount.setDetail(configKey, newValue.toString());
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
}
