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

package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.client.AccountWizard;
import cx.ring.service.LocalService;
import cx.ring.utils.BlockchainUtils;

public class RingAccountCreationFragment extends Fragment {
    static final String TAG = RingAccountCreationFragment.class.getSimpleName();
    private static final int PASSWORD_MIN_LENGTH = 6;

    @BindView(R.id.switch_ring_username)
    Switch mUsernameSwitch;

    @BindView(R.id.ring_username_txt_box)
    TextInputLayout mUsernameTxtBox;

    @BindView(R.id.ring_username)
    EditText mUsernameTxt;

    private TextWatcher mUsernameTextWatcher;

    @BindView(R.id.ring_password_txt_box)
    TextInputLayout mPasswordTxtBox;

    @BindView(R.id.ring_password)
    EditText mPasswordTxt;

    @BindView(R.id.ring_password_repeat_txt_box)
    TextInputLayout mPasswordRepeatTxtBox;

    @BindView(R.id.next_create_account)
    Button mNextButton;

    @BindView(R.id.last_create_account)
    Button mLastButton;

    @BindView(R.id.ring_username_box)
    ViewGroup mUsernameBox;

    @BindView(R.id.create_account)
    Button mCreateAccountButton;

    @OnTextChanged({R.id.ring_password, R.id.ring_password_repeat})
    @OnCheckedChanged(R.id.switch_ring_username)
    public void checkNextState() {
        boolean formHasError = validateForm();
        mCreateAccountButton.setEnabled(!formHasError);
    }

    /**
     * Checks the validity of the given password.
     * @return false if there is no error, true otherwise.
     */
    private boolean validateForm() {
        //~ Init
        boolean error = false;
        mPasswordTxtBox.setError(null);
        //~ Checking username presence.
        if (mUsernameSwitch.isChecked() && mUsernameTxtBox.getError() != null) {
            return true;
        }

        if (mUsernameSwitch.isChecked() && TextUtils.isEmpty(mUsernameTxt.getText().toString())) {
            mUsernameTxtBox.setErrorEnabled(true);
            mUsernameTxtBox.setError(getString(R.string.error_username_empty));
            return true;
        }

        //~ Checking initial password.
        if (mPasswordTxtBox.getEditText() == null || TextUtils.isEmpty(mPasswordTxtBox.getEditText().getText())) {
            error = true;
        } else if (mPasswordTxtBox.getEditText().getText().length() < PASSWORD_MIN_LENGTH) {
            mPasswordTxtBox.setErrorEnabled(true);
            mPasswordTxtBox.setError(getString(R.string.error_password_char_count));
            return true;
        } else {
            mPasswordTxtBox.setError(null);
        }
        //~ Checking confirmation password.
        if (mPasswordRepeatTxtBox != null) {
            mPasswordRepeatTxtBox.setErrorEnabled(true);
            if (mPasswordRepeatTxtBox.getEditText() == null || !mPasswordTxtBox.getEditText().getText().toString()
                    .equals(mPasswordRepeatTxtBox.getEditText().getText().toString())) {
                mPasswordRepeatTxtBox.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                mPasswordRepeatTxtBox.setError(null);
            }
        }
        return error;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_create, parent, false);
        ButterKnife.bind(this, view);

        BlockchainUtils.attachUsernameTextFilter(mUsernameTxt);

        if (isAdded()) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), mUsernameTxtBox, mUsernameTxt);
        }

        AccountWizard accountWizard = (AccountWizard) getActivity();
        if (accountWizard.isFirstAccount()) {
            mCreateAccountButton.setVisibility(View.GONE);
        } else {
            mNextButton.setVisibility(View.GONE);
        }

        return view;
    }

    @OnCheckedChanged(R.id.switch_ring_username)
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mUsernameBox.setVisibility(isChecked ? View.VISIBLE : View.GONE);
    }

    @OnEditorAction(R.id.ring_password)
    public boolean onPasswordEditorAction(TextView view, int actionId, KeyEvent event) {
        Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (mPasswordTxt.getText().length() != 0 && !validateForm()) {
                nextAccount(false);
                return true;
            }
        }
        return false;
    }

    @OnFocusChange(R.id.ring_password)
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            validateForm();
        }
    }

    @OnEditorAction(R.id.ring_password_repeat)
    public boolean onPasswordRepeatEditorAction(TextView view, int actionId, KeyEvent event) {
        Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (mPasswordTxt.getText().length() != 0 && !validateForm()) {
                nextAccount(true);
                return true;
            }
        }
        return false;
    }

    private boolean isValidUsername() {
        return mUsernameTxtBox.getError() == null;
    }

    @OnClick(R.id.next_create_account)
    public void onNextButtonClick() {
        nextAccount(false);
    }

    @OnClick(R.id.create_account)
    public void onCreateAccountButtonClick() {
        nextAccount(true);
    }

    private void nextAccount(Boolean startCreation) {
        if (!validateForm()) {
            Activity wizardActivity = getActivity();
            if (wizardActivity != null && wizardActivity instanceof AccountWizard) {
                AccountWizard wizard = (AccountWizard) wizardActivity;
                String username = null;
                if (mUsernameSwitch.isChecked()
                        && !TextUtils.isEmpty(mUsernameTxt.getText().toString())) {
                    if (!isValidUsername()) {
                        mUsernameTxt.requestFocus();
                        return;
                    }

                    username = mUsernameTxt.getText().toString();
                }

                if (startCreation) {
                    wizard.createAccount(username, null, mPasswordTxt.getText().toString());
                } else {
                    wizard.accountNext(username, null, mPasswordTxt.getText().toString());
                }
            }
        }
    }

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.accountLast();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ActionBar ab = ((AccountWizard) activity).getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.account_create_title);
        }
        if (mUsernameTxt != null) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), mUsernameTxtBox, mUsernameTxt);
        }
    }

    @Override
    public void onDetach() {
        if (mUsernameTxt != null) {
            mUsernameTxt.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }
}