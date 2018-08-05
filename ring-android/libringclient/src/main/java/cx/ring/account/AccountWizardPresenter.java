/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
package cx.ring.account;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import io.reactivex.observers.DisposableObserver;

public class AccountWizardPresenter extends RootPresenter<AccountWizardView> {

    public static final String TAG = AccountWizardPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final DeviceRuntimeService mDeviceRuntimeService;

    private boolean mCreationError = false;
    private boolean mCreatingAccount = false;
    private String mAccountType;
    private RingAccountViewModel mRingAccountViewModel;

    @Inject
    public AccountWizardPresenter(AccountService accountService, DeviceRuntimeService deviceRuntimeService) {
        this.mAccountService = accountService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    public void init(String accountType) {
        mAccountType = accountType;
        if (AccountConfig.ACCOUNT_TYPE_SIP.equals(mAccountType)) {
            getView().goToSipCreation();
        } else {
            getView().goToHomeCreation();
        }
    }

    public void initRingAccountCreation(RingAccountViewModel ringAccountViewModel, String defaultAccountName) {
        mRingAccountViewModel = ringAccountViewModel;
        HashMap<String, String> accountDetails = initRingAccountDetails(defaultAccountName);
        if (accountDetails != null) {
            if (!ringAccountViewModel.getUsername().isEmpty()) {
                accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), ringAccountViewModel.getUsername());
            }
            if (!ringAccountViewModel.getPassword().isEmpty()) {
                accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), ringAccountViewModel.getPassword());
            }
            createNewAccount(accountDetails);
        }
    }

    public void initRingAccountLink(RingAccountViewModel ringAccountViewModel, String defaultAccountName) {
        mRingAccountViewModel = ringAccountViewModel;
        HashMap<String, String> accountDetails = initRingAccountDetails(defaultAccountName);
        if (accountDetails != null) {
            if (!ringAccountViewModel.getPassword().isEmpty()) {
                accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), ringAccountViewModel.getPassword());
            }
            if (!ringAccountViewModel.getPin().isEmpty()) {
                accountDetails.put(ConfigKey.ARCHIVE_PIN.key(), ringAccountViewModel.getPin());
            }
            createNewAccount(accountDetails);
        }
    }

    public void successDialogClosed() {
        AccountWizardView view = getView();
        if (view != null) {
            getView().finish(true);
        }
    }

    private HashMap<String, String> initRingAccountDetails(String defaultAccountName) {
        HashMap<String, String> accountDetails = initAccountDetails();
        if (accountDetails != null) {
            accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), mAccountService.getNewAccountName(defaultAccountName));
            accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), "bootstrap.ring.cx");
            accountDetails.put(ConfigKey.ACCOUNT_UPNP_ENABLE.key(), AccountConfig.TRUE_STR);
        }
        return accountDetails;
    }

    private HashMap<String, String> initAccountDetails() {
        try {
            HashMap<String, String> accountDetails = (HashMap<String, String>) mAccountService.getAccountTemplate(mAccountType);
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }

            boolean hasCameraPermission = mDeviceRuntimeService.hasVideoPermission();
            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(hasCameraPermission));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), "sipinfo");
            return accountDetails;
        } catch (Exception e) {
            getView().displayCreationError();
            Log.e(TAG, "Error creating account", e);
            return null;
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        if (mCreatingAccount) {
            return;
        }

        mCreatingAccount = true;
        mCreationError = false;

        getView().blockOrientation();
        getView().displayProgress(true);

            mCompositeDisposable.add(mAccountService.addAccount(accountDetails)
                    .subscribeWith(new DisposableObserver<Account>() {
                        @Override
                        public void onNext(Account account) {
                            if (!handleCreationState(account)) {
                                dispose();
                            }
                        }
                        @Override
                        public void onError(Throwable e) {
                            handleCreationState(null);
                            dispose();
                        }
                        @Override
                        public void onComplete() {}
                    }));
    }

    private boolean handleCreationState(final Account account) {
        AccountWizardView view = getView();
        if (account == null) {
            view.displayProgress(false);
            view.displayCannotBeFoundError();
            return false;
        }
        String newState = account.getRegistrationState();
        if (account.isRing() && (newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))) {
            return true;
        }
        view.displayProgress(false);

        if (!mCreationError) {
            switch (newState) {
                case AccountConfig.STATE_ERROR_GENERIC:
                    view.displayGenericError();
                    mCreationError = true;
                    break;
                case AccountConfig.STATE_UNREGISTERED:
                    view.displaySuccessDialog();
                    view.saveProfile(account.getAccountID(), mRingAccountViewModel);
                    mCreationError = false;
                    break;
                case AccountConfig.STATE_ERROR_NETWORK:
                    view.displayNetworkError();
                    mCreationError = true;
                    break;
                default:
                    view.displaySuccessDialog();
                    view.saveProfile(account.getAccountID(), mRingAccountViewModel);
                    mCreationError = false;
                    break;
            }

            if (account.isRing() && mRingAccountViewModel.getUsername() != null && !mRingAccountViewModel.getUsername().contentEquals("")) {
                Log.i(TAG, "Account created, registering " + mRingAccountViewModel.getUsername());
                mAccountService.registerName(account, "", mRingAccountViewModel.getUsername());
            }
            mAccountService.setCurrentAccount(account);
        }
        return false;
    }
}
