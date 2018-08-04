/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import cx.ring.mvp.RingAccountViewModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;

public class RingAccountCreationPresenter extends RootPresenter<RingAccountCreationView> {

    public static final String TAG = RingAccountCreationPresenter.class.getSimpleName();
    public static final int PASSWORD_MIN_LENGTH = 6;

    protected AccountService mAccountService;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private RingAccountViewModel mRingAccountViewModel;

    private boolean isRingUserNameCorrect = false;
    private boolean isPasswordCorrect = true;
    private boolean isConfirmCorrect = true;
    private boolean isRegisterUsernameChecked = true;
    private String mPasswordConfirm = "";

    private final PublishSubject<String> contactQuery = PublishSubject.create();

    @Inject
    public RingAccountCreationPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    @Override
    public void bindView(RingAccountCreationView view) {
        super.bindView(view);
        //mCompositeDisposable.add(mAccountService.getRegisteredNames().subscribe(r -> handleBlockchainResult(r.state, r.name)));
        mCompositeDisposable.add(contactQuery
                .debounce(350, TimeUnit.MILLISECONDS)
                .switchMapSingle(q -> mAccountService.findRegistrationByName("", "", q))
                .observeOn(mUiScheduler)
                .subscribe(q -> handleBlockchainResult(q.name, q.address, q.state)));

    }

    public void init(RingAccountViewModel ringAccountViewModel) {
        mRingAccountViewModel = ringAccountViewModel;
    }

    public void userNameChanged(String userName) {
        if (!userName.isEmpty()) {
            mRingAccountViewModel.setUsername(userName);
            contactQuery.onNext(userName);
            isRingUserNameCorrect = false;
            getView().enableTextError();
        } else {
            mRingAccountViewModel.setUsername("");
            getView().disableTextError();
        }
        checkForms();
    }

    public void ringCheckChanged(boolean isChecked) {
        getView().displayUsernameBox(isChecked);
        isRegisterUsernameChecked = isChecked;
        if (!isChecked) {
            mRingAccountViewModel.setUsername("");
        }
        checkForms();
    }

    public void passwordChanged(String password) {
        mRingAccountViewModel.setPassword(password);
        if (!password.equals(mPasswordConfirm)) {
            getView().showNonMatchingPasswordError(true);
            isConfirmCorrect = false;
        } else {
            getView().showNonMatchingPasswordError(false);
            isConfirmCorrect = true;
        }
        if (!password.isEmpty() && password.length() < PASSWORD_MIN_LENGTH) {
            getView().showInvalidPasswordError(true);
            isPasswordCorrect = false;
        } else {
            getView().showInvalidPasswordError(false);
            isPasswordCorrect = true;
        }
        checkForms();
    }

    public void passwordConfirmChanged(String passwordConfirm) {
        if (!passwordConfirm.equals(mRingAccountViewModel.getPassword())) {
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
        getView().enableNextButton(false);
        getView().goToAccountCreation(mRingAccountViewModel);
    }

    private void checkForms() {
        if (isRegisterUsernameChecked) {
            getView().enableNextButton(isRingUserNameCorrect && isPasswordCorrect && isConfirmCorrect);
        } else {
            getView().enableNextButton(isPasswordCorrect && isConfirmCorrect);
        }
    }

    private void handleBlockchainResult(String name, String address, int state) {
        RingAccountCreationView view = getView();
        if (view == null) {
            return;
        }
        if (name == null || name.isEmpty()) {
            view.disableTextError();
            isRingUserNameCorrect = false;
        } else {
            switch (state) {
                case 0:
                    // on found
                    view.showExistingNameError();
                    isRingUserNameCorrect = false;
                    break;
                case 1:
                    // invalid name
                    view.showInvalidNameError();
                    isRingUserNameCorrect = false;
                    break;
                case 2:
                    // available
                    view.disableTextError();
                    mRingAccountViewModel.setUsername(name);
                    isRingUserNameCorrect = true;
                    break;
                default:
                    // on error
                    view.disableTextError();
                    isRingUserNameCorrect = false;
                    break;
            }
        }
        checkForms();
    }
}
