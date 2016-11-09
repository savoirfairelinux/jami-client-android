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
import android.support.annotation.NonNull;
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

    @BindView(R.id.add_button)
    Button mAddAccountBtn;

    @BindView(R.id.ring_username_box)
    ViewGroup mUsernameBox;

    /**
     * Checks the validity of the given password.
     *
     * @param pwd     the first password.
     * @param confirm the confirmation password.
     * @return false if there is no error, true otherwise.
     */
    private boolean checkPassword(@NonNull TextInputLayout pwd, TextInputLayout confirm) {
        //~ Init
        boolean error = false;
        pwd.setError(null);
        //~ Checking username presence.
        if (mUsernameSwitch.isChecked() && TextUtils.isEmpty(mUsernameTxt.getText().toString())) {
            mUsernameTxtBox.setErrorEnabled(true);
            mUsernameTxtBox.setError(getString(R.string.error_username_empty));
            return true;
        }
        //~ Checking initial password.
        if (pwd.getEditText() == null || TextUtils.isEmpty(pwd.getEditText().getText())) {
            error = true;
        } else if (pwd.getEditText().getText().length() < PASSWORD_MIN_LENGTH) {
            pwd.setError(getString(R.string.error_password_char_count));
            error = true;
        } else {
            pwd.setError(null);
        }
        //~ Checking confirmation password.
        if (confirm != null) {
            if (confirm.getEditText() == null || !pwd.getEditText().getText().toString()
                    .equals(confirm.getEditText().getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                confirm.setError(null);
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
            if (mPasswordTxt.getText().length() != 0 && !checkPassword(mPasswordTxtBox, mPasswordRepeatTxtBox)) {
                mAddAccountBtn.callOnClick();
                return true;
            }
        }
        return false;
    }

    @OnFocusChange(R.id.ring_password)
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            checkPassword(mPasswordTxtBox, null);
        }
    }

    @OnEditorAction(R.id.ring_password_repeat)
    public boolean onPasswordRepeatEditorAction(TextView view, int actionId, KeyEvent event) {
        Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (mPasswordTxt.getText().length() != 0 && !checkPassword(mPasswordTxtBox, mPasswordRepeatTxtBox)) {
                mAddAccountBtn.callOnClick();
                return true;
            }
        }
        return false;
    }

    private boolean isValidUsername() {
        return mUsernameTxtBox.getError() == null;
    }

    @OnClick(R.id.add_button)
    public void onAddButtonClick() {
        if (!checkPassword(mPasswordTxtBox, mPasswordRepeatTxtBox)) {
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
                wizard.initAccountCreation(true,
                        username,
                        null,
                        mPasswordTxt.getText().toString(),
                        null);
            }
        }
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