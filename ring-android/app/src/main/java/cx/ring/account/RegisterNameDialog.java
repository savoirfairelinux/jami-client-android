/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.account;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.services.AccountService;
import cx.ring.utils.RegisteredNameFilter;
import cx.ring.utils.RegisteredNameTextWatcher;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

public class RegisterNameDialog extends DialogFragment {
    static final String TAG = RegisterNameDialog.class.getSimpleName();
    @BindView(R.id.ring_username_txt_box)
    public TextInputLayout mUsernameTxtBox;
    @BindView(R.id.ring_username)
    public EditText mUsernameTxt;
    @BindView(R.id.password_txt_box)
    public TextInputLayout mPasswordTxtBox;
    @BindView(R.id.password_txt)
    public EditText mPasswordTxt;
    @BindString(R.string.register_name)
    public String mRegisterTitle;
    @BindString(R.string.register_username)
    public String mRegisterMessage;
    @BindString(R.string.prompt_new_username)
    public String mPromptUsername;
    @BindString(R.string.prompt_password)
    public String mPromptPassword;
    @Inject
    AccountService mAccountService;
    @BindString(R.string.username_already_taken)
    String mUserNameAlreadyTaken;
    @BindString(R.string.invalid_username)
    String mInvalidUsername;
    @Inject
    Scheduler mUiScheduler;

    private TextWatcher mUsernameTextWatcher;
    private RegisterNameDialogListener mListener = null;

    private Disposable mDisposableListener;

    public void setListener(RegisterNameDialogListener l) {
        mListener = l;
    }

    private void handleBlockchainResult(final int state, final String name) {
        String actualName = mUsernameTxt.getText().toString();
        if (actualName.isEmpty()) {
            mUsernameTxtBox.setErrorEnabled(false);
            mUsernameTxtBox.setError(null);
            return;
        }

        if (actualName.equals(name)) {
            switch (state) {
                case 0:
                    // on found
                    mUsernameTxtBox.setErrorEnabled(true);
                    mUsernameTxtBox.setError(mUserNameAlreadyTaken);
                    break;
                case 1:
                    // invalid name
                    mUsernameTxtBox.setErrorEnabled(true);
                    mUsernameTxtBox.setError(mInvalidUsername);
                    break;
                default:
                    // on error
                    mUsernameTxtBox.setErrorEnabled(false);
                    mUsernameTxtBox.setError(null);
                    break;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.frag_register_name, null);
        ButterKnife.bind(this, view);

        // dependency injection
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        String accountId = "";
        boolean hasPassword = true;
        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString(AccountEditionActivity.ACCOUNT_ID_KEY, accountId);
            hasPassword = args.getBoolean(AccountEditionActivity.ACCOUNT_HAS_PASSWORD_KEY, true);
        }

        mUsernameTxt.setFilters(new InputFilter[]{new RegisteredNameFilter()});
        mUsernameTextWatcher = new RegisteredNameTextWatcher(getActivity(), mAccountService, accountId, mUsernameTxtBox, mUsernameTxt);
        mUsernameTxt.addTextChangedListener(mUsernameTextWatcher);
        mPasswordTxtBox.setVisibility(hasPassword ? View.VISIBLE : View.GONE);

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.setView(view);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setMessage(mRegisterMessage)
                .setTitle(mRegisterTitle)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, (d, b) -> dismiss())
                .create();

        result.setOnShowListener(d -> {
            Button positiveButton = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view1 -> {
                if (validate()) {
                    dismiss();
                }
            });
        });

        return result;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mUsernameTxt != null) {
            mUsernameTxt.addTextChangedListener(mUsernameTextWatcher);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDisposableListener = mAccountService
                .getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> handleBlockchainResult(r.state, r.name));
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisposableListener.dispose();
    }

    @Override
    public void onDetach() {
        if (mUsernameTxt != null) {
            mUsernameTxt.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }

    private boolean isValidUsername() {
        return mUsernameTxtBox.getError() == null;
    }

    public boolean checkInput() {
        if (mUsernameTxt.getText().toString().isEmpty()) {
            mUsernameTxtBox.setErrorEnabled(true);
            mUsernameTxtBox.setError(mPromptUsername);
            return false;
        }

        if (!isValidUsername()) {
            mUsernameTxt.requestFocus();
            return false;
        }

        mUsernameTxtBox.setErrorEnabled(false);
        mUsernameTxtBox.setError(null);

        if (mPasswordTxtBox.getVisibility() == View.VISIBLE) {
            if (mPasswordTxt.getText().toString().isEmpty()) {
                mPasswordTxtBox.setErrorEnabled(true);
                mPasswordTxtBox.setError(mPromptPassword);
                return false;
            } else {
                mPasswordTxtBox.setErrorEnabled(false);
                mPasswordTxtBox.setError(null);
            }
        }
        return true;
    }

    boolean validate() {
        if (checkInput() && mListener != null) {
            final String username = mUsernameTxt.getText().toString();
            final String password = mPasswordTxt.getText().toString();
            mListener.onRegisterName(username, password);
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.ring_username, R.id.password_txt})
    public boolean onEditorAction(TextView v, int actionId) {
        if (v == mPasswordTxt) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                boolean validationResult = validate();
                if (validationResult) {
                    getDialog().dismiss();
                }

                return validationResult;
            }
        }
        return false;
    }

    public interface RegisterNameDialogListener {
        void onRegisterName(String name, String password);
    }
}
