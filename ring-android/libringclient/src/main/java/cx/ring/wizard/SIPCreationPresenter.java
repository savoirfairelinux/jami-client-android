/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.wizard;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.RootPresenter;
import cx.ring.mvp.SIPCreationView;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.Scheduler;
import io.reactivex.observers.DisposableObserver;

public class SIPCreationPresenter extends RootPresenter<SIPCreationView> {

    private static final String TAG = SIPCreationPresenter.class.getSimpleName();

    private AccountService mAccountService;

    private DeviceRuntimeService mDeviceService;

    private Account mAccount;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;


    @Inject
    public SIPCreationPresenter(AccountService mAccountService, DeviceRuntimeService mDeviceService) {
        this.mAccountService = mAccountService;
        this.mDeviceService = mDeviceService;
    }

    @Override
    public void bindView(SIPCreationView view) {
        super.bindView(view);
    }

    @Override
    public void unbindView() {
        super.unbindView();
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

    public void removeAccount() {
        if (mAccount != null) {
            mAccountService.removeAccount(mAccount.getAccountID());
            mAccount = null;
        }
    }

    private void registerAccount(final HashMap<String, String> accountDetails) {
        getView().showLoading();
        mCompositeDisposable.add(
                mAccountService.addAccount(accountDetails)
                .observeOn(mUiScheduler)
                .subscribeWith(new DisposableObserver<Account>() {
                    @Override
                    public void onNext(Account account) {
                        mAccount = account;
                        switch (account.getRegistrationState()) {
                            case AccountConfig.STATE_REGISTERED:
                            case AccountConfig.STATE_SUCCESS:
                            case AccountConfig.STATE_READY:
                                saveProfile(account.getAccountID());
                                getView().showRegistrationSuccess();
                                dispose();
                                break;
                            case AccountConfig.STATE_ERROR_NETWORK:
                                getView().showRegistrationNetworkError();
                                dispose();
                                break;
                            case AccountConfig.STATE_TRYING:
                            case AccountConfig.STATE_UNREGISTERED:
                                return;
                            default:
                                getView().showRegistrationError();
                                dispose();
                                break;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        getView().showRegistrationError();
                        dispose();
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                    }
                }));
    }

    private HashMap<String, String> initAccountDetails() {

        try {
            HashMap<String, String> accountDetails = mAccountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_SIP).blockingGet();
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }

            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(true));

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
        String formattedName = mAccount.getUsername();
        if (formattedName.isEmpty()) {
            formattedName = mAccount.getAlias();
        }
        vcard.setFormattedName(new FormattedName(formattedName));
        String vcardUid = formattedName + accountID;
        vcard.setUid(new Uid(vcardUid));
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, mDeviceService.provideFilesDir()).subscribe();
        mAccount.setProfile(vcard);
    }
}