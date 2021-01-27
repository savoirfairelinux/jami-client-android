/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;
import cx.ring.R;
import cx.ring.databinding.DialogConfirmRevocationBinding;

public class ConfirmRevocationDialog extends DialogFragment {
    public static final String DEVICEID_KEY = "deviceid_key";
    public static final String HAS_PASSWORD_KEY = "has_password_key";
    static final String TAG = ConfirmRevocationDialog.class.getSimpleName();
    private String mDeviceId;
    private Boolean mHasPassword = true;
    private ConfirmRevocationListener mListener = null;
    private DialogConfirmRevocationBinding binding;

    public void setListener(ConfirmRevocationListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogConfirmRevocationBinding.inflate(getActivity().getLayoutInflater());

        mDeviceId = requireArguments().getString(DEVICEID_KEY);
        mHasPassword = requireArguments().getBoolean(HAS_PASSWORD_KEY);

        final AlertDialog result = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setMessage(getString(R.string.revoke_device_message, mDeviceId))
                .setTitle(getText(R.string.revoke_device_title))
                .setPositiveButton(R.string.revoke_device_title, null) //Set to null. We override the onclick
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

        if(mHasPassword) {
            result.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            binding.passwordTxtBox.setVisibility(View.GONE);
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
        return result;
    }

    private boolean checkInput() {
        if (mHasPassword && binding.passwordTxt.getText().toString().isEmpty()) {
            binding.passwordTxtBox.setErrorEnabled(true);
            binding.passwordTxtBox.setError(getText(R.string.enter_password));
            return false;
        } else {
            binding.passwordTxtBox.setErrorEnabled(false);
            binding.passwordTxtBox.setError(null);
        }
        return true;
    }

    private boolean validate() {
        if (checkInput() && mListener != null) {
            final String password = binding.passwordTxt.getText().toString();
            mListener.onConfirmRevocation(mDeviceId, password);
            return true;
        }
        return false;
    }

    public interface ConfirmRevocationListener {
        void onConfirmRevocation(String deviceId, String password);
    }
}
