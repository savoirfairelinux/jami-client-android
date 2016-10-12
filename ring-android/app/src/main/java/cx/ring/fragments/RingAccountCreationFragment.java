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
import android.support.v7.widget.AppCompatButton;
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

import cx.ring.R;
import cx.ring.client.AccountWizard;
import cx.ring.service.LocalService;
import cx.ring.utils.BlockchainUtils;

public class RingAccountCreationFragment extends Fragment {
    static final String TAG = RingAccountCreationFragment.class.getSimpleName();
    private static final int PASSWORD_MIN_LENGTH = 4;

    private Switch usernameSwitch;
    private TextInputLayout usernameTxtBox;
    private EditText usernameTxt;
    private TextWatcher mUsernameTextWatcher;
    private TextInputLayout passwordTxtBox;
    private EditText passwordTxt;
    private TextInputLayout passwordRepeatTxtBox;
    private EditText passwordRepeatTxt;
    private Button addAccountBtn;
    private ViewGroup usernameBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private boolean checkPassword(@NonNull TextInputLayout pwd, TextInputLayout confirm) {
        boolean error = false;
        pwd.setError(null);
        if (usernameSwitch.isChecked()) {
            if (usernameTxt.getText().toString().isEmpty()) {
                usernameTxtBox.setErrorEnabled(true);
                usernameTxtBox.setError(getString(R.string.error_username_empty));
                return true;
            }
        }
        if (pwd.getEditText().getText().length() == 0) {
            error = true;
        } else {
            if (pwd.getEditText().getText().length() < PASSWORD_MIN_LENGTH) {
                pwd.setError(getString(R.string.error_password_char_count));
                error = true;
            } else {
                pwd.setError(null);
            }
        }
        if (confirm != null) {
            if (!pwd.getEditText().getText().toString().equals(confirm.getEditText().getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        addAccountBtn.setEnabled(!error);
        return error;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_create, parent, false);

        addAccountBtn = (AppCompatButton) view.findViewById(R.id.add_button);
        addAccountBtn.setEnabled(false);
        usernameBox = (ViewGroup) view.findViewById(R.id.ring_username_box);
        usernameTxtBox = (TextInputLayout) view.findViewById(R.id.ring_username_txt_box);
        usernameTxt = (EditText) view.findViewById(R.id.ring_username);

        BlockchainUtils.attachUsernameTextFilter(usernameTxt);

        if (isAdded()) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), usernameTxtBox, usernameTxt);
        }

        passwordTxt = (EditText) view.findViewById(R.id.ring_password);
        passwordTxtBox = (TextInputLayout) view.findViewById(R.id.ring_password_txt_box);
        passwordRepeatTxt = (EditText) view.findViewById(R.id.ring_password_repeat);
        passwordRepeatTxtBox = (TextInputLayout) view.findViewById(R.id.ring_password_repeat_txt_box);
        usernameSwitch = (Switch) view.findViewById(R.id.switch_ring_username);
        usernameSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                usernameBox.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        passwordTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_NEXT)
                    return checkPassword(passwordTxtBox, null);
                return false;
            }
        });
        passwordTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkPassword(passwordTxtBox, null);
                }
            }
        });
        passwordRepeatTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (passwordTxt.getText().length() != 0 && !checkPassword(passwordTxtBox, passwordRepeatTxtBox)) {
                        addAccountBtn.callOnClick();
                        return true;
                    }
                }
                return false;
            }
        });
        passwordRepeatTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addAccountBtn.callOnClick();
                    return true;
                }
                return false;
            }
        });
        addAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPassword(passwordTxtBox, passwordRepeatTxtBox))
                    ((AccountWizard) getActivity()).initAccountCreation(true, usernameTxt.getText().toString(), null, passwordTxt.getText().toString(), null);
            }
        });
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ActionBar ab = ((AccountWizard) activity).getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.account_create_title);
        }
        if (usernameTxt != null) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), usernameTxtBox, usernameTxt);
        }
    }

    @Override
    public void onDetach() {
        if (usernameTxt != null) {
            usernameTxt.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }
}