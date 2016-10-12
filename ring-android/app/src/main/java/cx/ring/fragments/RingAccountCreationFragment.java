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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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

public class RingAccountCreationFragment extends Fragment
{
    static final String TAG = RingAccountCreationFragment.class.getSimpleName();
    private static final int PASSWORD_MIN_LENGTH = 4;

    private Switch usernameSwitch;
    private TextInputLayout usernameTxtBox;
    private EditText usernameTxt;
    //private RegisterNameDialog usernameDialog;
    private TextInputLayout passwordTxtBox;
    private EditText passwordTxt;
    private TextInputLayout passwordRepeatTxtBox;
    private EditText passwordRepeatTxt;
    private Button nextBtn;
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
        nextBtn.setEnabled(!error);
        return error;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_create, parent, false);

        Toolbar bottomToolbar = (Toolbar) view.findViewById(R.id.toolbar_bottom);

        Button loginBtn = (Button)  view.findViewById(R.id.ring_login_btn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountWizard a = (AccountWizard) getActivity();
                if (a != null)
                    a.ringLogin(true);
            }
        });
        nextBtn = (Button) bottomToolbar.findViewById(R.id.btn_next);
        nextBtn.setEnabled(false);
        usernameBox = (ViewGroup) view.findViewById(R.id.ring_username_box);
        usernameTxtBox = (TextInputLayout) view.findViewById(R.id.ring_username_txt_box);
        usernameTxt = (EditText) view.findViewById(R.id.ring_username);
        usernameTxt.setFilters(new InputFilter[] { usernameFilter });
        if (isAdded())
            usernameTxt.addTextChangedListener(usernameValidator);

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
                        nextBtn.callOnClick();
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
                    nextBtn.callOnClick();
                    return true;
                }
                return false;
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPassword(passwordTxtBox, passwordRepeatTxtBox))
                    ((AccountWizard)getActivity()).initAccountCreation(true, usernameTxt.getText().toString(), null, passwordTxt.getText().toString(), null);
            }
        });
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ActionBar ab = ((AccountWizard)activity).getSupportActionBar();
        if (ab != null)
            ab.setTitle(R.string.account_create_title);
        if (usernameTxt != null)
            usernameTxt.addTextChangedListener(usernameValidator);
    }

    @Override
    public void onDetach() {
        if (usernameTxt != null)
            usernameTxt.removeTextChangedListener(usernameValidator);
        super.onDetach();
    }


    private final TextWatcher usernameValidator = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            usernameTxt.setError(null);
        }

        @Override
        public void afterTextChanged(final Editable txt) {
            final String name = txt.toString();
            Log.w(TAG, "afterTextChanged name lookup " + name);
            final LocalService s = ((LocalService.Callbacks)getActivity()).getService();
            if (s == null)
                return;
            s.lookupName("", name, new LocalService.NameLookupCallback() {
                @Override
                public void onFound(String name, String address) {
                    Log.w(TAG, "Name lookup UI : onFound " + name + " " + address + " (current " + txt.toString() + ")");
                    if (name.equals(txt.toString())) {
                        usernameTxtBox.setErrorEnabled(true);
                        usernameTxtBox.setError("Username already taken");
                    }
                }
                @Override
                public void onInvalidName(String name) {
                    Log.w(TAG, "Name lookup UI : onInvalidName " + name + " (current " + txt.toString() + ")");
                    if (name.equals(txt.toString())) {
                        usernameTxtBox.setErrorEnabled(true);
                        usernameTxtBox.setError("Invalid username");
                    }
                }
                @Override
                public void onError(String name, String address) {
                    Log.w(TAG, "Name lookup UI : onError " + name + " " + address + " (current " + txt.toString() + ")");
                    if (name.equals(txt.toString())) {
                        usernameTxtBox.setErrorEnabled(false);
                        usernameTxtBox.setError(null);
                    }
                }
            });
        }
    };

    private static final InputFilter usernameFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder) source;
                for (int i = end - 1; i >= start; i--) {
                    char currentChar = source.charAt(i);
                    if (Character.isLetterOrDigit(currentChar)) {
                        if (Character.isUpperCase(currentChar))
                            sourceAsSpannableBuilder.replace(i, i + 1, String.valueOf(Character.toLowerCase(currentChar)));
                    } else if (currentChar != '-' && currentChar != '_') {
                        sourceAsSpannableBuilder.delete(i, i + 1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char currentChar = source.charAt(i);
                    if (Character.isLetterOrDigit(currentChar)) {
                        filteredStringBuilder.append(Character.toLowerCase(currentChar));
                    } else if (currentChar == '-' || currentChar == '_') {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        }
    };


}