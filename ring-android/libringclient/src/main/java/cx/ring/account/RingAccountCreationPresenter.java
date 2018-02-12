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

import javax.inject.Inject;

import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class RingAccountCreationPresenter extends RootPresenter<RingAccountCreationView> implements Observer<ServiceEvent> {

    public static final String TAG = RingAccountCreationPresenter.class.getSimpleName();
    public static final int PASSWORD_MIN_LENGTH = 6;

    protected AccountService mAccountService;

    private NameLookupInputHandler mNameLookupInputHandler;

    private RingAccountViewModel mRingAccountViewModel;

    private boolean isRingUserNameCorrect = false;
    private boolean isPasswordCorrect = true;
    private boolean isConfirmCorrect = true;
    private boolean isRegisterUsernameChecked = true;
    private String mPasswordConfirm = "";

    @Inject
    public RingAccountCreationPresenter(AccountService accountService) {
        this.mAccountService = accountService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
    }

    @Override
    public void bindView(RingAccountCreationView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
    }

    public void init(RingAccountViewModel ringAccountViewModel) {
        mRingAccountViewModel = ringAccountViewModel;
    }

    public void userNameChanged(String userName) {
        if (!userName.isEmpty()) {
            if (mNameLookupInputHandler == null) {
                mNameLookupInputHandler = new NameLookupInputHandler(mAccountService, "");
            }
            mRingAccountViewModel.setUsername(userName);
            mNameLookupInputHandler.enqueueNextLookup(userName);
            isRingUserNameCorrect = false;
            getView().enableTextError();
        } else {
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

    private void handleBlockchainResult(int state, String name) {
        if (getView() == null) {
            return;
        }
        if (mRingAccountViewModel.getUsername() == null || mRingAccountViewModel.getUsername().isEmpty()) {
            getView().disableTextError();
            isRingUserNameCorrect = false;
        } else {
            if (mRingAccountViewModel.getUsername().equals(name)) {
                switch (state) {
                    case 0:
                        // on found
                        getView().showExistingNameError();
                        isRingUserNameCorrect = false;
                        break;
                    case 1:
                        // invalid name
                        getView().showInvalidNameError();
                        isRingUserNameCorrect = false;
                        break;
                    case 2:
                        // available
                        getView().disableTextError();
                        mRingAccountViewModel.setUsername(name);
                        isRingUserNameCorrect = true;
                        break;
                    default:
                        // on error
                        getView().disableTextError();
                        isRingUserNameCorrect = false;
                        break;
                }
            }
        }
        checkForms();
    }

    @Override
    public void update(Observable observable, final ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                handleBlockchainResult(state, name);
                break;
            default:
                break;
        }
    }
}
