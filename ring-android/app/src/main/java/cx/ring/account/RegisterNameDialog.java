/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cx.ring.R;
import cx.ring.databinding.FragRegisterNameBinding;
import net.jami.services.AccountService;
import cx.ring.utils.RegisteredNameFilter;
import cx.ring.utils.RegisteredNameTextWatcher;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

@AndroidEntryPoint
public class RegisterNameDialog extends DialogFragment {
    static final String TAG = RegisterNameDialog.class.getSimpleName();
    @Inject
    AccountService mAccountService;

    private TextWatcher mUsernameTextWatcher;
    private RegisterNameDialogListener mListener = null;

    private Disposable mDisposableListener;
    private FragRegisterNameBinding binding;

    public void setListener(RegisterNameDialogListener l) {
        mListener = l;
    }

    private void onLookupResult(final int state, final String name) {
        CharSequence actualName = binding.ringUsername.getText();
        if (actualName == null || actualName.length() == 0) {
            binding.ringUsernameTxtBox.setErrorEnabled(false);
            binding.ringUsernameTxtBox.setError(null);
            return;
        }

        if (name.contentEquals(actualName)) {
            switch (state) {
                case 0:
                    // on found
                    binding.ringUsernameTxtBox.setErrorEnabled(true);
                    binding.ringUsernameTxtBox.setError(getText(R.string.username_already_taken));
                    break;
                case 1:
                    // invalid name
                    binding.ringUsernameTxtBox.setErrorEnabled(true);
                    binding.ringUsernameTxtBox.setError(getText(R.string.invalid_username));
                    break;
                default:
                    // on error
                    binding.ringUsernameTxtBox.setErrorEnabled(false);
                    binding.ringUsernameTxtBox.setError(null);
                    break;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = FragRegisterNameBinding.inflate(getActivity().getLayoutInflater());
        View view = binding.getRoot();

        String accountId = "";
        boolean hasPassword = true;
        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
            hasPassword = args.getBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, true);
        }

        mUsernameTextWatcher = new RegisteredNameTextWatcher(getActivity(), mAccountService, accountId, binding.ringUsernameTxtBox, binding.ringUsername);
        binding.ringUsername.setFilters(new InputFilter[]{new RegisteredNameFilter()});
        binding.ringUsername.addTextChangedListener(mUsernameTextWatcher);
        // binding.ringUsername.setOnEditorActionListener((v, actionId, event) -> RegisterNameDialog.this.onEditorAction(v, actionId));

        binding.passwordTxtBox.setVisibility(hasPassword ? View.VISIBLE : View.GONE);
        binding.passwordTxt.setOnEditorActionListener((v, actionId, event) -> RegisterNameDialog.this.onEditorAction(v, actionId));

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.setView(view);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setMessage(R.string.register_username)
                .setTitle(R.string.register_name)
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (binding != null) {
            binding.ringUsername.addTextChangedListener(mUsernameTextWatcher);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDisposableListener = mAccountService
                .getRegisteredNames()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> onLookupResult(r.state, r.name));
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisposableListener.dispose();
    }

    @Override
    public void onDetach() {
        if (binding != null) {
            binding.ringUsername.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }

    private boolean isValidUsername() {
        return binding.ringUsernameTxtBox.getError() == null;
    }

    private boolean checkInput() {
        if (binding.ringUsername.getText() == null || binding.ringUsername.getText().length() == 0) {
            binding.ringUsernameTxtBox.setErrorEnabled(true);
            binding.ringUsernameTxtBox.setError(getText(R.string.prompt_new_username));
            return false;
        }

        if (!isValidUsername()) {
            binding.ringUsername.requestFocus();
            return false;
        }

        binding.ringUsernameTxtBox.setErrorEnabled(false);
        binding.ringUsernameTxtBox.setError(null);

        if (binding.passwordTxtBox.getVisibility() == View.VISIBLE) {
            if (binding.passwordTxt.getText() == null || binding.passwordTxt.getText().length() == 0) {
                binding.passwordTxtBox.setErrorEnabled(true);
                binding.passwordTxtBox.setError(getString(R.string.prompt_password));
                return false;
            } else {
                binding.passwordTxtBox.setErrorEnabled(false);
                binding.passwordTxtBox.setError(null);
            }
        }
        return true;
    }

    private boolean validate() {
        if (checkInput() && mListener != null) {
            final String username = binding.ringUsername.getText().toString();
            final String password = binding.passwordTxt.getText().toString();
            mListener.onRegisterName(username, password);
            return true;
        }
        return false;
    }

    private boolean onEditorAction(TextView v, int actionId) {
        if (v == binding.passwordTxt) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                boolean validationResult = validate();
                if (validationResult) {
                    Dialog dialog = getDialog();
                    if (dialog != null)
                        dialog.dismiss();
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
