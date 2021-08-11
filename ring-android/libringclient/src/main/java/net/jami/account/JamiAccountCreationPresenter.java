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
package net.jami.account;

import net.jami.services.AccountService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.mvp.AccountCreationModel;
import net.jami.mvp.RootPresenter;
import net.jami.utils.StringUtils;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class JamiAccountCreationPresenter extends RootPresenter<JamiAccountCreationView> {

    public static final String TAG = JamiAccountCreationPresenter.class.getSimpleName();
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final long TYPING_DELAY = 350L;
    private final PublishSubject<String> contactQuery = PublishSubject.create();
    protected net.jami.services.AccountService mAccountService;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;
    private AccountCreationModel mAccountCreationModel;
    private boolean isUsernameCorrect = false;
    private boolean isPasswordCorrect = true;
    private boolean isConfirmCorrect = true;
    private boolean showLoadingAnimation = true;
    private CharSequence mPasswordConfirm = "";

    @Inject
    public JamiAccountCreationPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    @Override
    public void bindView(JamiAccountCreationView view) {
        super.bindView(view);
        mCompositeDisposable.add(contactQuery
                .debounce(TYPING_DELAY, TimeUnit.MILLISECONDS)
                .switchMapSingle(q -> mAccountService.
                        findRegistrationByName("", "", q))
                .observeOn(mUiScheduler)
                .subscribe(q -> onLookupResult(q.getName(), q.getAddress(), q.getState())));
    }

    public void init(AccountCreationModel accountCreationModel) {
        if (accountCreationModel == null) {
            getView().cancel();
        }
        mAccountCreationModel = accountCreationModel;
    }

    /**
     * Called everytime the provided username for the new account changes
     * Sends the new value of the username to the ContactQuery subjet and shows the loading
     * animation if it has not been started before
     */
    public void userNameChanged(String userName) {
        if (mAccountCreationModel != null)
            mAccountCreationModel.setUsername(userName);
        contactQuery.onNext(userName);
        isUsernameCorrect = false;

        if (showLoadingAnimation) {
            JamiAccountCreationView view = getView();
            if (view != null)
                view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.LOADING);
            showLoadingAnimation = false;
        }
    }

    public void registerUsernameChanged(boolean isChecked) {
        if (mAccountCreationModel != null) {
            if (!isChecked) {
                mAccountCreationModel.setUsername("");
            }
            checkForms();
        }
    }

    public void passwordUnset() {
        if (mAccountCreationModel != null)
            mAccountCreationModel.setPassword(null);
        isPasswordCorrect = true;
        isConfirmCorrect = true;
        getView().showInvalidPasswordError(false);
        getView().enableNextButton(true);
    }

    public void passwordChanged(String password, CharSequence repeat) {
        mPasswordConfirm = repeat;
        passwordChanged(password);
    }

    public void passwordChanged(String password) {
        if (mAccountCreationModel != null)
            mAccountCreationModel.setPassword(password);
        if (!StringUtils.isEmpty(password) && password.length() < PASSWORD_MIN_LENGTH) {
            getView().showInvalidPasswordError(true);
            isPasswordCorrect = false;
        } else {
            getView().showInvalidPasswordError(false);
            isPasswordCorrect = password.length() != 0;
            if (!password.contentEquals(mPasswordConfirm)) {
                if (mPasswordConfirm.length() > 0)
                    getView().showNonMatchingPasswordError(true);
                isConfirmCorrect = false;
            } else {
                getView().showNonMatchingPasswordError(false);
                isConfirmCorrect = true;
            }
        }
        getView().enableNextButton(isPasswordCorrect && isConfirmCorrect);
    }

    public void passwordConfirmChanged(String passwordConfirm) {
        if (!passwordConfirm.equals(mAccountCreationModel.getPassword())) {
            getView().showNonMatchingPasswordError(true);
            isConfirmCorrect = false;
        } else {
            getView().showNonMatchingPasswordError(false);
            isConfirmCorrect = true;
        }
        mPasswordConfirm = passwordConfirm;
        getView().enableNextButton(isPasswordCorrect && isConfirmCorrect);
    }

    public void createAccount() {
        if (isInputValid()) {
            JamiAccountCreationView view = getView();
            view.goToAccountCreation(mAccountCreationModel);
        }
    }

    private boolean isInputValid() {
        boolean passwordOk = isPasswordCorrect && isConfirmCorrect;
        boolean usernameOk = mAccountCreationModel != null && mAccountCreationModel.getUsername() != null || isUsernameCorrect;
        return passwordOk && usernameOk;
    }

    private void checkForms() {
        boolean valid = isInputValid();
        if (valid && isUsernameCorrect)
            getView().updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.AVAILABLE);
    }

    private void onLookupResult(String name, String address, int state) {
        JamiAccountCreationView view = getView();
        //Once we get the result, we can show the loading animation again when the user types
        showLoadingAnimation = true;
        if (view == null) {
            return;
        }
        if (name == null || name.isEmpty()) {
            view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.RESET);
            isUsernameCorrect = false;
        } else {
            switch (state) {
                case 0:
                    // on found
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN);
                    isUsernameCorrect = false;
                    break;
                case 1:
                    // invalid name
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_INVALID);
                    isUsernameCorrect = false;
                    break;
                case 2:
                    // available
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.AVAILABLE);
                    mAccountCreationModel.setUsername(name);
                    isUsernameCorrect = true;
                    break;
                default:
                    // on error
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.ERROR);
                    isUsernameCorrect = false;
                    break;
            }
        }
        checkForms();
    }

    public void setPush(boolean push) {
        if (mAccountCreationModel != null) {
            mAccountCreationModel.setPush(push);
        }
    }
}
