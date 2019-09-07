/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;

public class ChangePasswordDialog extends DialogFragment {
    static final String TAG = ChangePasswordDialog.class.getSimpleName();

    @BindView(R.id.old_password_txt_box)
    protected TextInputLayout mPasswordTxtBox;

    @BindView(R.id.old_password_txt)
    protected EditText mPasswordTxt;

    @BindView(R.id.new_password_txt_box)
    protected TextInputLayout mNewPasswordTxtBox;

    @BindView(R.id.new_password_txt)
    protected EditText mNewPasswordTxt;

    @BindView(R.id.new_password_repeat_txt_box)
    protected TextInputLayout mNewPasswordRepeatsTxtBox;

    @BindView(R.id.new_password_repeat_txt)
    protected EditText mNewPasswordRepeatsTxt;

    @BindString(R.string.enter_password)
    protected String mPromptPassword;

    private PasswordChangedListener mListener = null;

    public void setListener(PasswordChangedListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_set_password, null);
        ButterKnife.bind(this, view);

        String accountId = "";
        boolean hasPassword = true;
        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString(AccountEditionActivity.ACCOUNT_ID_KEY, accountId);
            hasPassword = args.getBoolean(AccountEditionActivity.ACCOUNT_HAS_PASSWORD_KEY, true);
        }
        int passwordMessage = hasPassword ? R.string.account_password_change : R.string.account_password_set;
        mPasswordTxtBox.setVisibility(hasPassword ? View.VISIBLE : View.GONE);

        final AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
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

    public boolean checkInput() {
        if (!mNewPasswordTxt.getText().toString().contentEquals(mNewPasswordRepeatsTxt.getText())) {
            mNewPasswordTxtBox.setErrorEnabled(true);
            mNewPasswordTxtBox.setError(getText(R.string.error_passwords_not_equals));
            mNewPasswordRepeatsTxtBox.setErrorEnabled(true);
            mNewPasswordRepeatsTxtBox.setError(getText(R.string.error_passwords_not_equals));
            return false;
        } else {
            mNewPasswordTxtBox.setErrorEnabled(false);
            mNewPasswordTxtBox.setError(null);
            mNewPasswordRepeatsTxtBox.setErrorEnabled(false);
            mNewPasswordRepeatsTxtBox.setError(null);
        }
        return true;
    }

    private boolean validate() {
        if (checkInput() && mListener != null) {
            final String oldPassword = mPasswordTxt.getText().toString();
            final String newPassword = mNewPasswordTxt.getText().toString();
            mListener.onPasswordChanged(oldPassword, newPassword);
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.new_password_repeat_txt})
    public boolean onEditorAction(TextView v, int actionId) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (validate()) {
                getDialog().dismiss();
                return true;
            }
        }
        return false;
    }

    public interface PasswordChangedListener {
        void onPasswordChanged(String oldPassword, String newPassword);
    }
}
