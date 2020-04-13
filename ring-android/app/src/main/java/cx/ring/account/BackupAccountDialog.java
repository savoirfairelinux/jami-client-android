/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cx.ring.R;
import cx.ring.databinding.DialogConfirmRevocationBinding;

public class BackupAccountDialog extends DialogFragment {
    static final String TAG = BackupAccountDialog.class.getSimpleName();

    private String mAccountId;

    private UnlockAccountListener mListener = null;
    private DialogConfirmRevocationBinding binding;

    public void setListener(UnlockAccountListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogConfirmRevocationBinding.inflate(getActivity().getLayoutInflater());

        Bundle args = getArguments();
        if (args != null) {
            mAccountId = args.getString(AccountEditionFragment.ACCOUNT_ID_KEY);
        }

        binding.passwordTxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                boolean validationResult = validate();
                if (validationResult) {
                    getDialog().dismiss();
                }
                return validationResult;
            }
            return false;
        });

        final AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_enter_password)
                .setMessage(R.string.account_new_device_password)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> dismiss()
                )
                .create();
        result.setOnShowListener(dialog -> {
            Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view1 -> {
                if (validate()) {
                    dismiss();
                }
            });
        });
        result.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return result;
    }

    private boolean validate() {
        if (mListener != null) {
            mListener.onUnlockAccount(mAccountId, binding.passwordTxt.getText().toString());
            return true;
        }
        return false;
    }

    public interface UnlockAccountListener {
        void onUnlockAccount(String accountId, String password);
    }
}
