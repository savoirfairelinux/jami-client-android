/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.account;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.mvp.BaseFragment;

public class RingAccountCreationFragment extends BaseFragment<RingAccountCreationPresenter> implements RingAccountCreationView {

    @BindView(R.id.switch_ring_username)
    protected Switch mUsernameSwitch;

    @BindView(R.id.ring_username_txt_box)
    protected TextInputLayout mUsernameTxtBox;

    @BindView(R.id.ring_username)
    protected EditText mUsernameTxt;

    @BindView(R.id.ring_password_txt_box)
    protected TextInputLayout mPasswordTxtBox;

    @BindView(R.id.ring_password)
    protected EditText mPasswordTxt;

    @BindView(R.id.ring_password_repeat_txt_box)
    protected TextInputLayout mPasswordRepeatTxtBox;

    @BindView(R.id.last_create_account)
    protected Button mLastButton;

    @BindView(R.id.ring_username_box)
    protected ViewGroup mUsernameBox;

    @BindView(R.id.create_account)
    protected Button mCreateAccountButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_create, parent, false);
        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @OnCheckedChanged(R.id.switch_ring_username)
    public void onCheckedChanged(boolean isChecked) {
        presenter.ringCheckChanged(isChecked);
    }

    @OnClick(R.id.create_account)
    public void onCreateAccountButtonClick() {
        presenter.createAccount();
    }

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.accountLast();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ActionBar ab = ((AccountWizard) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.account_create_title);
        }
    }

    @OnTextChanged(value = R.id.ring_username, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void onUsernameChanged() {
        mUsernameTxt.setError(null);
    }

    @OnTextChanged(value = R.id.ring_password, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void afterPasswordChanged(Editable txt) {
        presenter.passwordChanged(txt.toString());
    }

    @OnTextChanged(value = R.id.ring_password_repeat, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void afterPasswordConfirmChanged(Editable txt) {
        presenter.passwordConfirmChanged(txt.toString());
    }

    @OnTextChanged(value = R.id.ring_username, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterUsernameChanged(Editable txt) {
        presenter.textChanged(txt.toString());
    }

    @Override
    public void enableTextError() {
        mUsernameTxtBox.setErrorEnabled(true);
        mUsernameTxtBox.setError(getString(R.string.looking_for_username_availability));
    }

    @Override
    public void disableTextError() {
        mUsernameTxtBox.setErrorEnabled(false);
        mUsernameTxtBox.setError(null);
    }

    @Override
    public void showExistingNameError() {
        mUsernameTxtBox.setErrorEnabled(true);
        mUsernameTxtBox.setError(getString(R.string.username_already_taken));
    }

    @Override
    public void showInvalidNameError() {
        mUsernameTxtBox.setErrorEnabled(true);
        mUsernameTxtBox.setError(getString(R.string.invalid_username));
    }

    @Override
    public void showInvalidPasswordError(boolean display) {
        if (display) {
            mPasswordTxtBox.setError(getString(R.string.error_password_char_count));
        } else {
            mPasswordTxtBox.setError(null);
        }
    }

    @Override
    public void showNonMatchingPasswordError(boolean display) {
        if (display) {
            mPasswordRepeatTxtBox.setError(getString(R.string.error_passwords_not_equals));
        } else {
            mPasswordRepeatTxtBox.setError(null);
        }
    }

    @Override
    public void displayUsernameBox(boolean display) {
        mUsernameBox.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void enableNextButton(boolean enabled) {
        mCreateAccountButton.setEnabled(enabled);
    }

    @Override
    public void goToAccountCreation(String username, String password) {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null && wizardActivity instanceof AccountWizard) {
            AccountWizard wizard = (AccountWizard) wizardActivity;
            wizard.createAccount(username, null, password);
        }
    }
}
