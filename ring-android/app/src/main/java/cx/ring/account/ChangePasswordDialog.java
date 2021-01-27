/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cx.ring.R;
import cx.ring.databinding.DialogSetPasswordBinding;

public class ChangePasswordDialog extends DialogFragment {
    static final String TAG = ChangePasswordDialog.class.getSimpleName();

    private PasswordChangedListener mListener = null;
    private DialogSetPasswordBinding binding;

    public void setListener(PasswordChangedListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogSetPasswordBinding.inflate(getActivity().getLayoutInflater());

        String accountId = "";
        boolean hasPassword = true;
        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
            hasPassword = args.getBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, true);
        }
        int passwordMessage = hasPassword ? R.string.account_password_change : R.string.account_password_set;
        binding.oldPasswordTxtBox.setVisibility(hasPassword ? View.VISIBLE : View.GONE);
        binding.newPasswordRepeatTxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validate()) {
                    getDialog().dismiss();
                    return true;
                }
            }
            return false;
        });

        final AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setMessage(R.string.help_password_choose)
                .setTitle(passwordMessage)
                .setPositiveButton(passwordMessage, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dismiss())
                .create();
        result.setOnShowListener(dialog -> {
            Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view1 -> {
                if (validate()) {
                    dismiss();
                }
            });
        });
        result.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return result;
    }

    private boolean checkInput() {
        if (!binding.newPasswordTxt.getText().toString().contentEquals(binding.newPasswordRepeatTxt.getText())) {
            binding.newPasswordTxtBox.setErrorEnabled(true);
            binding.newPasswordTxtBox.setError(getText(R.string.error_passwords_not_equals));
            binding.newPasswordRepeatTxtBox.setErrorEnabled(true);
            binding.newPasswordRepeatTxtBox.setError(getText(R.string.error_passwords_not_equals));
            return false;
        } else {
            binding.newPasswordTxtBox.setErrorEnabled(false);
            binding.newPasswordTxtBox.setError(null);
            binding.newPasswordRepeatTxtBox.setErrorEnabled(false);
            binding.newPasswordRepeatTxtBox.setError(null);
        }
        return true;
    }

    private boolean validate() {
        if (checkInput() && mListener != null) {
            final String oldPassword = binding.oldPasswordTxt.getText().toString();
            final String newPassword = binding.newPasswordTxt.getText().toString();
            mListener.onPasswordChanged(oldPassword, newPassword);
            return true;
        }
        return false;
    }

    public interface PasswordChangedListener {
        void onPasswordChanged(String oldPassword, String newPassword);
    }
}
