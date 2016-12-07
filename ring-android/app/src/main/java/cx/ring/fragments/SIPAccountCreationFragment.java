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
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.mvp.SIPCreationView;
import cx.ring.wizard.SIPCreationPresenter;

public class SIPAccountCreationFragment extends Fragment implements SIPCreationView {
    static final String TAG = SIPAccountCreationFragment.class.getSimpleName();

    @Inject
    SIPCreationPresenter mPresenter;

    private ProgressDialog mProgress = null;

    @BindView(R.id.alias)
    EditText mAliasView;

    @BindView(R.id.hostname)
    EditText mHostnameView;

    @BindView(R.id.username)
    EditText mUsernameView;

    @BindView(R.id.password)
    EditText mPasswordView;

    @BindView(R.id.create_sip_button)
    Button mCreateSIPAccountButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_acc_sip_create, parent, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
    }

    @OnEditorAction(R.id.password)
    @SuppressWarnings("unused")
    public boolean keyPressedOnPasswordField(TextView v, int actionId, KeyEvent event) {
        if (actionId == getResources().getInteger(R.integer.register_sip_account_actionid)
                || event == null
                || (event.getAction() == KeyEvent.ACTION_UP)) {
            mCreateSIPAccountButton.callOnClick();
        }
        return true;
    }

    /************************
     * SIP Account ADD
     ***********************/
    @OnClick(R.id.create_sip_button)
    public void createSIPAccount() {
        createSIPAccount(false);
    }

    /**
     * Start the creation process in the presenter
     *
     * @param bypassWarnings boolean stating if we want to display warning to the user or create the account anyway
     */
    private void createSIPAccount(boolean bypassWarnings) {
        String alias = mAliasView.getText().toString();
        String hostname = mHostnameView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        mPresenter.startCreation(alias, hostname, username, password, bypassWarnings);
    }

    @Override
    public void onResume() {
        super.onResume();
        // view binding
        mPresenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // view unbinding
        mPresenter.unbindView();
    }

    @Override
    public void showUsernameError() {
        mUsernameView.setError(getString(R.string.error_field_required));
        mUsernameView.requestFocus();
    }

    @Override
    public void showLoading() {
        mProgress = new ProgressDialog(getActivity());
        mProgress.setTitle(R.string.dialog_wait_create);
        mProgress.setMessage(getString(R.string.dialog_wait_create_details));
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }

    @Override
    public void resetErrors() {
        mAliasView.setError(null);
        mPasswordView.setError(null);
    }

    @Override
    public void showAliasError() {
        mAliasView.setError(getString(R.string.error_field_required));
        mAliasView.requestFocus();
    }

    @Override
    public void showPasswordError() {
        mPasswordView.setError(getString(R.string.error_field_required));
        mPasswordView.requestFocus();
    }

    @Override
    public void showIP2IPWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_warn_ip2ip_account_title)
                .setMessage(R.string.dialog_warn_ip2ip_account_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createSIPAccount(true);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    @Override
    public void showRegistrationError() {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_sip_cannot_be_registered)
                        .setMessage(R.string.account_sip_cannot_be_registered_message);
                dialogBuilder.setNegativeButton(R.string.account_sip_register_anyway, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().setResult(Activity.RESULT_OK, new Intent());
                        getActivity().finish();
                    }
                });
                dialogBuilder.show();
            }
        });
    }

    @Override
    public void showRegistrationNetworkError() {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setTitle(R.string.account_no_network_title)
                        .setMessage(R.string.account_no_network_message);
                dialogBuilder.setNegativeButton(R.string.account_sip_register_anyway, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().setResult(Activity.RESULT_OK, new Intent());
                        getActivity().finish();
                    }
                });
                dialogBuilder.show();
            }
        });
    }

    @Override
    public void showRegistrationSuccess() {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().setResult(Activity.RESULT_OK, new Intent());
                        getActivity().finish();
                    }
                });
                dialogBuilder.setTitle(R.string.account_sip_success_title)
                        .setMessage(R.string.account_sip_success_message);
                dialogBuilder.show();
            }
        });
    }
}
