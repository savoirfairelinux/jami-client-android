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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.SIPCreationView;
import cx.ring.wizard.SIPCreationPresenter;

public class SIPAccountCreationFragment extends BaseSupportFragment<SIPCreationPresenter> implements SIPCreationView {

    public static final String TAG = SIPAccountCreationFragment.class.getSimpleName();

    @BindView(R.id.alias)
    protected EditText mAliasView;

    @BindView(R.id.hostname)
    protected EditText mHostnameView;

    @BindView(R.id.username)
    protected EditText mUsernameView;

    @BindView(R.id.password)
    protected EditText mPasswordView;

    @BindView(R.id.create_sip_button)
    protected Button mCreateSIPAccountButton;

    private ProgressDialog mProgress = null;

    @Override
    public int getLayout() {
        return R.layout.frag_acc_sip_create;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @OnEditorAction(R.id.password)
    public boolean keyPressedOnPasswordField(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            mCreateSIPAccountButton.callOnClick();
        }
        return false;
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
        //orientation is locked during the create of account to avoid the destruction of the thread
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        String alias = mAliasView.getText().toString();
        String hostname = mHostnameView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        presenter.startCreation(alias, hostname, username, password, bypassWarnings);
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
        showDialog(getActivity().getString(R.string.dialog_warn_ip2ip_account_title),
                getActivity().getString(R.string.dialog_warn_ip2ip_account_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(android.R.string.cancel),
                (dialog, which) -> {
                    dialog.dismiss();
                    createSIPAccount(true);
                },
                null);
    }

    @Override
    public void showRegistrationError() {
        showDialog(getActivity().getString(R.string.account_sip_cannot_be_registered),
                getActivity().getString(R.string.account_sip_cannot_be_registered_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(R.string.account_sip_register_anyway),
                (dialog, which) -> presenter.removeAccount(),
                (dialog, id) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                });
    }

    @Override
    public void showRegistrationNetworkError() {
        showDialog(getActivity().getString(R.string.account_no_network_title),
                getActivity().getString(R.string.account_no_network_message),
                getActivity().getString(android.R.string.ok),
                getActivity().getString(R.string.account_sip_register_anyway),
                (dialog, which) -> presenter.removeAccount(),
                (dialog, id) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                });
    }

    @Override
    public void showRegistrationSuccess() {
        showDialog(getActivity().getString(R.string.account_sip_success_title),
                getActivity().getString(R.string.account_sip_success_message),
                getActivity().getString(android.R.string.ok),
                null,
                (dialog, which) -> {
                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                    getActivity().finish();
                },
                null);
    }

    public void showDialog(final String title,
                           final String message,
                           final String positive,
                           final String negative,
                           final DialogInterface.OnClickListener listenerPositive,
                           final DialogInterface.OnClickListener listenerNegative) {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }

        //orientation is locked during the create of account to avoid the destruction of the thread
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        new MaterialAlertDialogBuilder(requireContext())
                .setPositiveButton(positive, listenerPositive)
                .setNegativeButton(negative, listenerNegative)
                .setTitle(title).setMessage(message)
                .setOnDismissListener(dialog -> {
                    //unlock the screen orientation
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                })
                .show();
    }
}
