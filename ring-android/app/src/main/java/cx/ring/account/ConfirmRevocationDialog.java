/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class ConfirmRevocationDialog extends DialogFragment implements Observer<ServiceEvent> {
    static final String TAG = ConfirmRevocationDialog.class.getSimpleName();
    public static final String DEVICEID_KEY = "deviceid_key";

    private String mDeviceId;

    @Override
    public void update(Observable observable, ServiceEvent event) {

    }

    public interface ConfirmRevocationListener {
        void onConfirmRevocation(String deviceId, String password);
    }

    @Inject
    AccountService mAccountService;

    @BindView(R.id.ring_password_txt_box)
    public TextInputLayout mPasswordTxtBox;

    @BindView(R.id.ring_password_txt)
    public EditText mPasswordTxt;

    @BindString(R.string.enter_password)
    public String mPromptPassword;

    @BindString(R.string.revoke_device_title)
    public String mRegisterTitle;

    private ConfirmRevocationListener mListener = null;

    public void setListener(ConfirmRevocationListener l) {
        mListener = l;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mDeviceId = getArguments().getString(DEVICEID_KEY);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_confirm_revocation, null);

        ButterKnife.bind(this, view);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.setView(view);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        builder.setView(view);

        String message = getString(R.string.revoke_device_message, mDeviceId);

        builder.setMessage(message)
                .setTitle(mRegisterTitle)
                .setPositiveButton(R.string.revoke_device_title, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        }
                );

        AlertDialog result = builder.create();

        result.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (validate()) {
                            dismiss();
                        }
                    }
                });
            }
        });

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    public boolean checkInput() {
        if (mPasswordTxt.getText().toString().isEmpty()) {
            mPasswordTxtBox.setErrorEnabled(true);
            mPasswordTxtBox.setError(mPromptPassword);
            return false;
        } else {
            mPasswordTxtBox.setErrorEnabled(false);
            mPasswordTxtBox.setError(null);
        }
        return true;
    }

    boolean validate() {
        if (checkInput() && mListener != null) {
            final String password = mPasswordTxt.getText().toString();
            mListener.onConfirmRevocation(mDeviceId, password);
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.ring_password_txt})
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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
}
