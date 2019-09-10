/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.Settings;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.PreferencesService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

public class AccountWizardPresenter extends RootPresenter<AccountWizardView> {

    public static final String TAG = AccountWizardPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final PreferencesService mPreferences;
    private final Scheduler mUiScheduler;

    //private boolean mCreationError = false;
    private boolean mCreatingAccount = false;
    private String mAccountType;
    private AccountCreationModel mAccountCreationModel;

    private Observable<Account> newAccount;

    @Inject
    public AccountWizardPresenter(AccountService accountService, PreferencesService preferences, @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mPreferences = preferences;
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

    public void initJamiAccountConnect(AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    if (!StringUtils.isEmpty(accountCreationModel.getManagementServer())) {
                        accountDetails.put(ConfigKey.MANAGER_URI.key(), accountCreationModel.getManagementServer());
                    }
                    if (!StringUtils.isEmpty(accountCreationModel.getPassword())) {
                        accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), accountCreationModel.getPassword());
                    }
                    if (!StringUtils.isEmpty(accountCreationModel.getPassword())) {
                        accountDetails.put(ConfigKey.ACCOUNT_USERNAME.key(), accountCreationModel.getUsername());
                    }
                    if (accountCreationModel.isPush()) {
                        accountDetails.put(ConfigKey.PROXY_ENABLED.key(), AccountConfig.TRUE_STR);
                    }
                    return accountDetails;
                });
        createAccount(accountCreationModel, newAccount);
    }

    public void initRingAccountCreation(AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    if (!accountCreationModel.getUsername().isEmpty()) {
                        accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), accountCreationModel.getUsername());
                    }
                    if (!accountCreationModel.getPassword().isEmpty()) {
                        accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), accountCreationModel.getPassword());
                    }
                    if (accountCreationModel.isPush()) {
                        accountDetails.put(ConfigKey.PROXY_ENABLED.key(), AccountConfig.TRUE_STR);
                    }
                    return accountDetails;
                });
        createAccount(accountCreationModel, newAccount);
    }

    public void initRingAccountLink(AccountCreationModel accountCreationModel, String defaultAccountName) {
        Single<Map<String, String>> newAccount = initRingAccountDetails(defaultAccountName)
                .map(accountDetails -> {
                    Settings settings = mPreferences.getSettings();
                    if(settings != null && settings.isAllowPushNotifications()) {
                        accountCreationModel.setPush(true);
                        accountDetails.put(ConfigKey.PROXY_ENABLED.key(), AccountConfig.TRUE_STR);
                    }
                    if (!accountCreationModel.getPassword().isEmpty()) {
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
                        AccountWizardView view = getView();
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
        AccountWizardView view = getView();
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
                    if (!model.isLink() && a.isRing() && !StringUtils.isEmpty(model.getUsername()))
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

    public void profileCreated(AccountCreationModel accountCreationModel, boolean saveProfile) {
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
                    AccountWizardView view = getView();
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
