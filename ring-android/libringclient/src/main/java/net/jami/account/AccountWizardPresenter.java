/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.account;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.model.Account;
import net.jami.model.AccountConfig;
import net.jami.model.ConfigKey;
import net.jami.model.Settings;
import net.jami.mvp.AccountCreationModel;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.PreferencesService;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class AccountWizardPresenter extends RootPresenter<net.jami.account.AccountWizardView> {

    public static final String TAG = AccountWizardPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final PreferencesService mPreferences;
    private final DeviceRuntimeService mDeviceService;
    private final Scheduler mUiScheduler;

    //private boolean mCreationError = false;
    private boolean mCreatingAccount = false;
    private String mAccountType;
    private AccountCreationModel mAccountCreationModel;

    private Observable<Account> newAccount;

    @Inject
    public AccountWizardPresenter(AccountService accountService,
                                  PreferencesService preferences,
                                  DeviceRuntimeService deviceService,
                                  @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mPreferences = preferences;
        mDeviceService = deviceService;
        mUiScheduler = uiScheduler;
    }

    public void init(String accountType) {
        mAccountType = accountType;
        if (AccountConfig.ACCOUNT_TYPE_SIP.equals(mAccountType)) {
            getView().goToSipCreation();
        } else {
            getView().goToHomeCreation();
        }
    }

    private void setProxyDetails(net.jami.mvp.AccountCreationModel accountCreationModel, Map<String, String> details)  {
        if (accountCreationModel.isPush()) {
            details.put(ConfigKey.PROXY_ENABLED.key(), AccountConfig.TRUE_STR);
            String pushToken = mDeviceService.getPushToken();
            if (!StringUtils.isEmpty(pushToken))
                details.put(ConfigKey.PROXY_PUSH_TOKEN.key(), pushToken);
        }
    }

    public void initJamiAccountConnect(AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    if (!StringUtils.isEmpty(accountCreationModel.getManagementServer())) {
                        accountDetails.put(ConfigKey.MANAGER_URI.key(), accountCreationModel.getManagementServer());
                        if (!StringUtils.isEmpty(accountCreationModel.getUsername())) {
                            accountDetails.put(ConfigKey.MANAGER_USERNAME.key(), accountCreationModel.getUsername());
                        }
                    } else if (!StringUtils.isEmpty(accountCreationModel.getUsername())) {
                        accountDetails.put(ConfigKey.ACCOUNT_USERNAME.key(), accountCreationModel.getUsername());
                    }
                    if (!StringUtils.isEmpty(accountCreationModel.getPassword())) {
                        accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), accountCreationModel.getPassword());
                    }
                    setProxyDetails(accountCreationModel, accountDetails);
                    return accountDetails;
                });
        createAccount(accountCreationModel, newAccount);
    }

    public void initJamiAccountCreation(net.jami.mvp.AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    if (!StringUtils.isEmpty(accountCreationModel.getUsername())) {
                        accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), accountCreationModel.getUsername());
                    }
                    if (!StringUtils.isEmpty(accountCreationModel.getPassword())) {
                        accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), accountCreationModel.getPassword());
                    }
                    setProxyDetails(accountCreationModel, accountDetails);
                    return accountDetails;
                });
        createAccount(accountCreationModel, newAccount);
    }

    public void initJamiAccountLink(net.jami.mvp.AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    Settings settings = mPreferences.getSettings();
                    if (settings != null && settings.isAllowPushNotifications()) {
                        accountCreationModel.setPush(true);
                        setProxyDetails(accountCreationModel, accountDetails);
                    }
                    if (!StringUtils.isEmpty(accountCreationModel.getPassword())) {
                        accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), accountCreationModel.getPassword());
                    }
                    if (accountCreationModel.getArchive() != null) {
                        accountDetails.put(ConfigKey.ARCHIVE_PATH.key(), accountCreationModel.getArchive().getAbsolutePath());
                    } else if (!accountCreationModel.getPin().isEmpty()) {
                        accountDetails.put(ConfigKey.ARCHIVE_PIN.key(), accountCreationModel.getPin());
                    }
                    return accountDetails;
                });
        createAccount(accountCreationModel, newAccount);
    }

    private void createAccount(AccountCreationModel accountCreationModel, Single<Map<String, String>> details) {
        mAccountCreationModel = accountCreationModel;
        Observable<Account> newAccount = details.flatMapObservable(accountDetails -> createNewAccount(accountCreationModel, accountDetails));
        accountCreationModel.setAccountObservable(newAccount);
        mCompositeDisposable.add(newAccount
                .observeOn(mUiScheduler)
                .subscribe(accountCreationModel::setNewAccount, e-> Log.e(TAG, "Can't create account", e)));
        if (accountCreationModel.isLink()) {
            getView().displayProgress(true);
            mCompositeDisposable.add(newAccount
                    .filter(a -> {
                        String newState = a.getRegistrationState();
                        return !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING));
                    })
                    .firstOrError()
                    .observeOn(mUiScheduler)
                    .subscribe(acc -> {
                        accountCreationModel.setNewAccount(acc);
                        net.jami.account.AccountWizardView view = getView();
                        if (view != null) {
                            view.displayProgress(false);
                            String newState = acc.getRegistrationState();
                            if (newState.contentEquals(AccountConfig.STATE_ERROR_GENERIC)) {
                                mCreatingAccount = false;
                                if (accountCreationModel.getArchive() == null)
                                    view.displayCannotBeFoundError();
                                else
                                    view.displayGenericError();
                            } else {
                                view.goToProfileCreation(accountCreationModel);
                            }
                        }
                    }, e -> {
                        mCreatingAccount = false;
                        getView().displayProgress(false);
                        getView().displayCannotBeFoundError();
                    }));
        } else {
            getView().goToProfileCreation(accountCreationModel);
        }
    }

    public void successDialogClosed() {
        net.jami.account.AccountWizardView view = getView();
        if (view != null) {
            getView().finish(true);
        }
    }

    private Single<HashMap<String, String>> initRingAccountDetails(String defaultAccountName) {
        return initAccountDetails().map(accountDetails -> {
            accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), mAccountService.getNewAccountName(defaultAccountName));
            accountDetails.put(ConfigKey.ACCOUNT_UPNP_ENABLE.key(), AccountConfig.TRUE_STR);
            return accountDetails;
        });
    }

    private Single<HashMap<String, String>> initAccountDetails() {
        if (mAccountType == null)
            return Single.error(new IllegalStateException());
        return mAccountService.getAccountTemplate(mAccountType)
                .map(accountDetails -> {
                    accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(true));
                    accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), "sipinfo");
                    return accountDetails;
                });
    }

    private Observable<Account> createNewAccount(AccountCreationModel model, Map<String, String> accountDetails) {
        if (mCreatingAccount) {
            return newAccount;
        }

        mCreatingAccount = true;
        //mCreationError = false;

        BehaviorSubject<Account> account = BehaviorSubject.create();
        account.filter(a -> {
                    String newState = a.getRegistrationState();
                    return !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING));
                })
                .firstElement()
                .subscribe(a -> {
                    if (!model.isLink() && a.isJami() && !StringUtils.isEmpty(model.getUsername()))
                        mAccountService.registerName(a, model.getPassword(), model.getUsername());
                    mAccountService.setCurrentAccount(a);
                    if (model.isPush()) {
                        Settings settings = mPreferences.getSettings();
                        settings.setAllowPushNotifications(true);
                        mPreferences.setSettings(settings);
                    }
                });

        mAccountService
                .addAccount(accountDetails)
                .subscribe(account);

        newAccount = account;
        return account;
    }

    public void profileCreated(net.jami.mvp.AccountCreationModel accountCreationModel, boolean saveProfile) {
        getView().blockOrientation();
        getView().displayProgress(true);

        Single<Account> newAccount = mAccountCreationModel
                .getAccountObservable()
                .filter(a -> {
                    String newState = a.getRegistrationState();
                    return !(newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING));
                })
                .firstOrError();

        if (saveProfile) {
            newAccount = newAccount.flatMap(a -> getView()
                    .saveProfile(a, accountCreationModel)
                    .map(vcard -> a));
        }

        mCompositeDisposable.add(newAccount
                .observeOn(mUiScheduler)
                .subscribe(account -> {
                    mCreatingAccount = false;
                    net.jami.account.AccountWizardView view = getView();
                    if (view != null) {
                        view.displayProgress(false);
                        String newState = account.getRegistrationState();
                        Log.w(TAG, "newState " + newState);
                        switch (newState) {
                            case AccountConfig.STATE_ERROR_GENERIC:
                                view.displayGenericError();
                                //mCreationError = true;
                                break;
                            case AccountConfig.STATE_UNREGISTERED:
                                //mCreationError = false;
                                break;
                            case AccountConfig.STATE_ERROR_NETWORK:
                                view.displayNetworkError();
                                //mCreationError = true;
                                break;
                            default:
                                //mCreationError = false;
                                break;
                        }
                        view.displaySuccessDialog();
                    }
                }, e -> {
                    mCreatingAccount = false;
                    //mCreationError = true;
                    AccountWizardView view = getView();
                    if (view != null) {
                        view.displayGenericError();
                        getView().finish(true);
                    }
                }));
    }

    public void errorDialogClosed() {
        getView().goToHomeCreation();
    }
}
