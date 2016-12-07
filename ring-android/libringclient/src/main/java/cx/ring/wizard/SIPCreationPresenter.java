/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.mvp.SIPCreationView;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.RawProperty;

public class SIPCreationPresenter extends RootPresenter<SIPCreationView> implements Observer<ServiceEvent> {

    private static final String TAG = SIPCreationPresenter.class.getSimpleName();

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceService;

    @Inject
    @Named("ApplicationExecutor")
    ExecutorService mApplicationExecutor;

    private Account mAccount;

    @Override
    public void afterInjection() {
        // Nothing to do here
    }

    @Override
    public void bindView(SIPCreationView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }
        switch (event.getEventType()) {
            case REGISTRATION_STATE_CHANGED:
                String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                if (mAccount == null || !mAccount.getAccountID().contentEquals(accountId)) {
                    Log.w(TAG, "error");
                    return;
                }

                handleNewAccountState(event.getEventInput(ServiceEvent.EventInput.STATE, String.class));

                break;
            default:
                break;
        }
    }

    private void handleNewAccountState(String accountState) {
        switch (accountState) {
            case AccountConfig.STATE_ERROR_GENERIC:
            case AccountConfig.STATE_UNREGISTERED:
                getView().showRegistrationError();
                break;
            case AccountConfig.STATE_ERROR_NETWORK:
                getView().showRegistrationNetworkError();
                break;
            default:
                saveProfile(mAccount.getAccountID());
                getView().showRegistrationSuccess();
                break;
        }
    }

    /**
     * Attempts to register the account specified by the form. If there are form errors (invalid or missing fields, etc.), the
     * errors are presented and no actual creation attempt is made.
     *
     * @param alias         Alias account value
     * @param hostname      hostname account value
     * @param username      username account value
     * @param password      password account value
     * @param bypassWarning Report eventual warning to the user
     */
    public void startCreation(String alias, String hostname, String username, String password, boolean bypassWarning) {

        getView().resetErrors();

        // Store values at the time of the login attempt.
        boolean warningIPAccount = false;

        // Alias is mandatory
        if (alias == null || alias.isEmpty()) {
            getView().showAliasError();
            return;
        }

        if (hostname != null && hostname.isEmpty()) {
            warningIPAccount = true;
        }

        if (!warningIPAccount && (password == null || password.trim().isEmpty())) {
            getView().showPasswordError();
            return;
        }

        if (!warningIPAccount && (username == null || username.trim().isEmpty())) {
            getView().showUsernameError();
            return;
        }

        if (warningIPAccount && !bypassWarning) {
            getView().showIP2IPWarning();
        } else {
            HashMap<String, String> accountDetails = initAccountDetails();

            if (accountDetails != null) {
                accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), alias);
                if (hostname != null && !hostname.isEmpty()) {
                    accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), hostname);
                    accountDetails.put(ConfigKey.ACCOUNT_USERNAME.key(), username);
                    accountDetails.put(ConfigKey.ACCOUNT_PASSWORD.key(), password);
                }
            }
            registerAccount(accountDetails);
        }
    }

    private void registerAccount(final HashMap<String, String> accountDetails) {

        getView().showLoading();

        mApplicationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (mAccount == null) {
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
            }
        });
    }

    private HashMap<String, String> initAccountDetails() {

        try {
            HashMap<String, String> accountDetails = (HashMap<String, String>) mAccountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_SIP);
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }

            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(mDeviceService.hasVideoPermission()));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), "sipinfo");
            return accountDetails;
        } catch (Exception e) {
            Log.e(TAG, "Error creating account", e);
            return null;
        }
    }

    private void saveProfile(String accountID) {
        VCard vcard = new VCard();
        vcard.setFormattedName(new FormattedName(mAccount.getUsername()));
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, mDeviceService.provideFilesDir());
    }

}
