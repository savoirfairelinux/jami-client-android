/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;

public class RenameDeviceDialog extends DialogFragment {
    static final String TAG = RenameDeviceDialog.class.getSimpleName();
    public static final String DEVICENAME_KEY = "devicename_key";

    public interface RenameDeviceListener {
        void onDeviceRename(String newName);
    }

    @BindView(R.id.ring_device_name_txt_box)
    public TextInputLayout mDeviceNameTxtBox;

    @BindString(R.string.account_device_name_empty)
    protected String mPromptDeviceName;

    @BindView(R.id.ring_device_name_txt)
    public EditText mDeviceNameTxt;

    private RenameDeviceListener mListener = null;

    public void setListener(RenameDeviceListener l) {
        mListener = l;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_device_rename, null);
        ButterKnife.bind(this, view);

        mDeviceNameTxt.setText(getArguments().getString(DEVICENAME_KEY));

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.rename_device_title)
                .setMessage(R.string.rename_device_message)
                .setPositiveButton(R.string.rename_device_button, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (validate()) {
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    public boolean checkInput() {
        if (mDeviceNameTxt.getText().toString().isEmpty()) {
            mDeviceNameTxtBox.setErrorEnabled(true);
            mDeviceNameTxtBox.setError(mPromptDeviceName);
            return false;
        } else {
            mDeviceNameTxtBox.setErrorEnabled(false);
            mDeviceNameTxtBox.setError(null);
        }
        return true;
    }

    boolean validate() {
        if (checkInput() && mListener != null) {
            mListener.onDeviceRename(mDeviceNameTxt.getText().toString());
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.ring_device_name_txt})
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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
}
