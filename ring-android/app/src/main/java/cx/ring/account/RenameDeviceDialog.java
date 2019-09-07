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
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.account;

import android.app.Dialog;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;

public class RenameDeviceDialog extends DialogFragment {
    public static final String DEVICENAME_KEY = "devicename_key";
    static final String TAG = RenameDeviceDialog.class.getSimpleName();
    @BindView(R.id.ring_device_name_txt_box)
    public TextInputLayout mDeviceNameTxtBox;
    @BindView(R.id.ring_device_name_txt)
    public EditText mDeviceNameTxt;
    @BindString(R.string.account_device_name_empty)
    protected String mPromptDeviceName;
    private RenameDeviceListener mListener = null;

    public void setListener(RenameDeviceListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_device_rename, null);
        ButterKnife.bind(this, view);

        mDeviceNameTxt.setText(getArguments().getString(DEVICENAME_KEY));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(R.string.rename_device_title)
                .setMessage(R.string.rename_device_message)
                .setPositiveButton(R.string.rename_device_button, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(dialog1 -> {
            Button button = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                if (validate()) {
                    dialog1.dismiss();
                }
            });
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    @Override
    public void onDestroy() {
        mListener = null;
        super.onDestroy();
    }

    private boolean checkInput(String input) {
        if (input.isEmpty()) {
            mDeviceNameTxtBox.setErrorEnabled(true);
            mDeviceNameTxtBox.setError(mPromptDeviceName);
            return false;
        } else {
            mDeviceNameTxtBox.setErrorEnabled(false);
            mDeviceNameTxtBox.setError(null);
        }
        return true;
    }

    private boolean validate() {
        String input = mDeviceNameTxt.getText().toString().trim();
        if (checkInput(input) && mListener != null) {
            mListener.onDeviceRename(input);
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.ring_device_name_txt})
    public boolean onEditorAction(TextView v, int actionId) {
        if (v == mDeviceNameTxt) {
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

    public interface RenameDeviceListener {
        void onDeviceRename(String newName);
    }
}
