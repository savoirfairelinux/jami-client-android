/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import net.jami.model.Account;
import net.jami.model.ConfigKey;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.HardwareService;
import net.jami.services.PreferencesService;

import io.reactivex.rxjava3.core.Scheduler;

public class GeneralAccountPresenter extends RootPresenter<GeneralAccountView> {

    private static final String TAG = GeneralAccountPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final HardwareService mHardwareService;
    private final PreferencesService mPreferenceService;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private Account mAccount;

    @Inject
    GeneralAccountPresenter(AccountService accountService, HardwareService hardwareService, PreferencesService preferencesService) {
        this.mAccountService = accountService;
        this.mHardwareService = hardwareService;
        this.mPreferenceService = preferencesService;
    }

    // Init with current account
    public void init() {
        init(mAccountService.getCurrentAccount());
    }

    public void init(String accountId) {
        init(mAccountService.getAccount(accountId));
    }

    private void init(Account account) {
        mCompositeDisposable.clear();
        mAccount = account;

        if (account != null) {
            if (account.isJami()) {
                getView().addJamiPreferences(account.getAccountID());
            } else {
                getView().addSipPreferences();
            }
            getView().accountChanged(account);
            mCompositeDisposable.add(mAccountService.getObservableAccount(account.getAccountID())
                    .observeOn(mUiScheduler)
                    .subscribe(acc -> getView().accountChanged(acc)));

            mCompositeDisposable.add(mHardwareService.getMaxResolutions()
                    .observeOn(mUiScheduler)
                    .subscribe(res -> {
                        if (res.first == null) {
                            getView().updateResolutions(null, mPreferenceService.getResolution());
                        } else {
                            getView().updateResolutions(res, mPreferenceService.getResolution());
                        }
                    },
                    e -> getView().updateResolutions(null, mPreferenceService.getResolution())));
        } else {
            Log.e(TAG, "init: No currentAccount available");
            getView().finish();
        }
    }

    void setEnabled(boolean enabled) {
        mAccount.setEnabled(enabled);
        mAccountService.setAccountEnabled(mAccount.getAccountID(), enabled);
    }

    public void twoStatePreferenceChanged(ConfigKey configKey, Object newValue) {
        if (configKey == ConfigKey.ACCOUNT_ENABLE) {
            setEnabled((Boolean) newValue);
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

    public void removeAccount() {
        mAccountService.removeAccount(mAccount.getAccountID());
    }

}
