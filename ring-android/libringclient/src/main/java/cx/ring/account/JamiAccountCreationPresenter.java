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
package cx.ring.account;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;

public class JamiAccountCreationPresenter extends RootPresenter<JamiAccountCreationView> {

    public static final String TAG = JamiAccountCreationPresenter.class.getSimpleName();
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final long TYPING_DELAY = 350L;
    private final PublishSubject<String> contactQuery = PublishSubject.create();
    protected AccountService mAccountService;
    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;
    private AccountCreationModel mAccountCreationModel;
    private boolean isRingUserNameCorrect = false;
    private boolean isPasswordCorrect = true;
    private boolean isConfirmCorrect = true;
    private boolean isRegisterUsernameChecked = true;
    private boolean startUsernameAvailabitlityProgressBarAnimation = true;
    private String mPasswordConfirm = "";

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
                .subscribe(q -> handleBlockchainResult(q.name, q.address, q.state)));
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
     * @param userName
     */
    public void userNameChanged(String userName) {
        if (mAccountCreationModel != null)
            mAccountCreationModel.setUsername(userName);
        contactQuery.onNext(userName);
        isRingUserNameCorrect = false;

        if (startUsernameAvailabitlityProgressBarAnimation) {
            JamiAccountCreationView view = getView();
            if (view != null)
                view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.LOADING);
            startUsernameAvailabitlityProgressBarAnimation = false;
        }
    }

    public void registerUsernameChanged(boolean isChecked) {
        getView().displayUsernameBox(isChecked);
        isRegisterUsernameChecked = isChecked;
        if (mAccountCreationModel != null) {
            if (!isChecked) {
                mAccountCreationModel.setUsername("");
            }
            checkForms();
        }
    }

    public void passwordUnset() {
        mAccountCreationModel.setPassword(null);
        isPasswordCorrect = true;
        isConfirmCorrect = true;
        getView().showInvalidPasswordError(false);
        checkForms();
    }

    public void passwordChanged(String password, String repeat) {
        mPasswordConfirm = repeat;
        passwordChanged(password);
    }

    public void passwordChanged(String password) {
        mAccountCreationModel.setPassword(password);
        if (!password.isEmpty() && password.length() < PASSWORD_MIN_LENGTH) {
            getView().showInvalidPasswordError(true);
            isPasswordCorrect = false;
        } else {
            getView().showInvalidPasswordError(false);
            isPasswordCorrect = true;
            if (!password.equals(mPasswordConfirm)) {
                getView().showNonMatchingPasswordError(true);
                isConfirmCorrect = false;
            } else {
                getView().showNonMatchingPasswordError(false);
                isConfirmCorrect = true;
            }
        }
        checkForms();
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
        checkForms();
    }

    public void createAccount() {
        if (isInputValid()) {
            JamiAccountCreationView view = getView();
            view.enableNextButton(false);
            view.goToAccountCreation(mAccountCreationModel);
        }
    }

    private boolean isInputValid() {
        boolean passwordOk = isPasswordCorrect && isConfirmCorrect;
        boolean usernameOk = !isRegisterUsernameChecked || isRingUserNameCorrect;
        return passwordOk && usernameOk;
    }

    private void checkForms() {
        boolean valid = isInputValid();
        getView().enableNextButton(valid);
        if(valid && isRingUserNameCorrect)
            getView().updateUsernameAvailability(JamiAccountCreationView.
                    UsernameAvailabilityStatus.AVAILABLE);
    }

    private void handleBlockchainResult(String name, String address, int state) {
        JamiAccountCreationView view = getView();
        //Once we get the result, we can show the loading animation again when the user types
        startUsernameAvailabitlityProgressBarAnimation = true;
        if (view == null) {
            return;
        }
        if (name == null || name.isEmpty()) {

            view.updateUsernameAvailability(JamiAccountCreationView.
                    UsernameAvailabilityStatus.RESET);
            isRingUserNameCorrect = false;
        } else {
            switch (state) {
                case 0:
                    // on found
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN);
                    isRingUserNameCorrect = false;
                    break;
                case 1:
                    // invalid name
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.ERROR_USERNAME_INVALID);
                    isRingUserNameCorrect = false;
                    break;
                case 2:
                    // available
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.AVAILABLE);
                    mAccountCreationModel.setUsername(name);
                    isRingUserNameCorrect = true;
                    break;
                default:
                    // on error
                    view.updateUsernameAvailability(JamiAccountCreationView.
                            UsernameAvailabilityStatus.ERROR);
                    isRingUserNameCorrect = false;
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
