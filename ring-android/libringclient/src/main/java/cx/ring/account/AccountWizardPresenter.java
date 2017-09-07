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
package cx.ring.account;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class AccountWizardPresenter extends RootPresenter<AccountWizardView> implements Observer<ServiceEvent> {

    public static final String TAG = AccountWizardPresenter.class.getSimpleName();

    protected AccountService mAccountService;
    protected DeviceRuntimeService mDeviceRuntimeService;

    private boolean mLinkAccount = false;
    private boolean mCreationError = false;
    private boolean mCreatingAccount = false;

    private String mAccountType;
    private Account mAccount;
    private String mCreatedAccountId;

    private RingAccountViewModel mRingAccountViewModel;

    @Inject
    public AccountWizardPresenter(AccountService accountService, DeviceRuntimeService deviceRuntimeService) {
        this.mAccountService = accountService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void bindView(AccountWizardView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
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
        getView().finish(true);
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

        if (mAccountType.equals(AccountConfig.ACCOUNT_TYPE_RING) || mAccount == null) {
            mAccount = mAccountService.addAccount(accountDetails);
        } else {
            mAccount.setDetail(ConfigKey.ACCOUNT_ALIAS, accountDetails.get(ConfigKey.ACCOUNT_ALIAS.key()));
            if (accountDetails.containsKey(ConfigKey.ACCOUNT_HOSTNAME.key())) {
                mAccount.setDetail(ConfigKey.ACCOUNT_HOSTNAME, accountDetails.get(ConfigKey.ACCOUNT_HOSTNAME.key()));
                mAccount.setDetail(ConfigKey.ACCOUNT_USERNAME, accountDetails.get(ConfigKey.ACCOUNT_USERNAME.key()));
                mAccount.setDetail(ConfigKey.ACCOUNT_PASSWORD, accountDetails.get(ConfigKey.ACCOUNT_PASSWORD.key()));
            }

            mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
        }

        mCreatingAccount = false;
    }


    private void handleCreationState(final ServiceEvent event) {

        String stateAccountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);

        if (!stateAccountId.isEmpty() && stateAccountId.equals(mCreatedAccountId)) {
            String newState = event.getEventInput(ServiceEvent.EventInput.STATE, String.class);

            mAccount = mAccountService.getAccount(mCreatedAccountId);

            if (mAccount != null && mAccount.isRing() && (newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))) {
                return;
            }

            getView().displayProgress(false);

            if (!mCreationError) {
                switch (newState) {
                    case AccountConfig.STATE_ERROR_GENERIC:
                        getView().displayGenericError();
                        mCreationError = true;
                        break;
                    case AccountConfig.STATE_UNREGISTERED:
                        if (mLinkAccount) {
                            getView().displayCannotBeFoundError();
                            mCreationError = true;
                        } else {
                            getView().displaySuccessDialog();
                            getView().saveProfile(mAccount.getAccountID(), mRingAccountViewModel);
                            mCreationError = false;
                            mAccountService.setCurrentAccount(mAccount);
                            break;
                        }
                        break;
                    case AccountConfig.STATE_ERROR_NETWORK:
                        getView().displayNetworkError();
                        mCreationError = true;
                        break;
                    default:
                        getView().displaySuccessDialog();
                        getView().saveProfile(mAccount.getAccountID(), mRingAccountViewModel);
                        mCreationError = false;
                        mAccountService.setCurrentAccount(mAccount);
                        break;
                }

                if (mRingAccountViewModel.getUsername() != null && !mRingAccountViewModel.getUsername().contentEquals("")) {
                    Log.i(TAG, "Account created, registering " + mRingAccountViewModel.getUsername());
                    mAccountService.registerName(mAccount, "", mRingAccountViewModel.getUsername());
                }
            }
        }

    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || (AccountConfig.ACCOUNT_TYPE_SIP.equals(mAccountType))) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNT_ADDED:
                mCreatedAccountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                handleCreationState(event);
                break;
            case REGISTRATION_STATE_CHANGED:
                handleCreationState(event);
                break;
            default:
                cx.ring.utils.Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }
}
