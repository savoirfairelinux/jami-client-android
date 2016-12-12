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
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class AccountMigrationFragment extends Fragment implements Observer<ServiceEvent> {
    static final String TAG = AccountMigrationFragment.class.getSimpleName();

    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private String mAccountId;

    @Inject
    AccountService mAccountService;

    // UI references.
    @BindView(R.id.ring_password)
    EditText mRingPassword;

    private ProgressDialog mProgress = null;

    private boolean migratingAccount = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_account_migration, parent, false);
        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
    }

    @OnEditorAction(R.id.ring_password)
    public boolean onPasswordEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.d(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        return actionId == EditorInfo.IME_ACTION_NEXT && checkPassword(v, null);
    }

    @OnFocusChange(R.id.ring_password)
    public void onPasswordFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            checkPassword((TextView) v, null);
        }
    }

    @OnClick(R.id.ring_migrate_btn)
    @SuppressWarnings("unused")
    void onMigrateButtonClick(View view) {
        initAccountMigration(mRingPassword.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
        if (getArguments() != null) {
            mAccountId = getArguments().getString(ACCOUNT_ID);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    @SuppressWarnings("unchecked")
    private void initAccountMigration(String password) {
        if (migratingAccount) {
            return;
        }

        migratingAccount = true;

        new MigrateAccountTask(mAccountId, password).execute();
    }

    private boolean checkPassword(@NonNull TextView pwd, TextView confirm) {
        boolean error = false;
        if (pwd.getText().length() == 0) {
            error = true;
        } else {
            if (pwd.getText().length() < 6) {
                pwd.setError(getString(R.string.error_password_char_count));
                error = true;
            } else {
                pwd.setError(null);
            }
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                confirm.requestFocus();
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
    }

    @Override
    public void update(Observable observable, final ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTRATION_STATE_CHANGED:
                handleMigrationState(event);
                break;
            default:
                Log.d(TAG, "This event " + event.getEventType() + " is not handled here");
                break;
        }
    }

    private void handleMigrationState(final ServiceEvent event) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {

                String newState = event.getEventInput(ServiceEvent.EventInput.STATE, String.class);
                if (TextUtils.isEmpty(newState)) {
                    if (mProgress != null) {
                        mProgress.dismiss();
                        mProgress = null;
                    }
                    return;
                }

                if (!AccountConfig.STATE_INITIALIZING.contentEquals(newState)) {
                    if (mProgress != null) {
                        mProgress.dismiss();
                        mProgress = null;
                    }
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                    dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    });
                    boolean success = false;
                    switch (newState) {
                        case AccountConfig.STATE_ERROR_GENERIC:
                            dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                                    .setMessage(R.string.account_cannot_be_found_message);
                            break;
                        case AccountConfig.STATE_ERROR_NETWORK:
                            dialogBuilder.setTitle(R.string.account_no_network_title)
                                    .setMessage(R.string.account_no_network_message);
                            break;
                        default:
                            dialogBuilder.setTitle(R.string.account_device_updated_title)
                                    .setMessage(R.string.account_device_updated_message);
                            success = true;
                            break;
                    }
                    AlertDialog dialogSuccess = dialogBuilder.show();
                    if (success) {
                        dialogSuccess.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                getActivity().setResult(Activity.RESULT_OK, new Intent());
                                getActivity().finish();
                            }
                        });
                    }
                }
            }
        });
    }

    private class MigrateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private final String mAccountId;
        private final String mPassword;

        MigrateAccountTask(String accountId, String password) {
            Log.d(TAG, "MigrateAccountTask");
            mAccountId = accountId;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            mProgress = new ProgressDialog(getActivity());
            mProgress.setTitle(R.string.dialog_wait_update);
            mProgress.setMessage(getString(R.string.dialog_wait_update_details));
            mProgress.setCancelable(false);
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accs) {

            final Account account = mAccountService.getAccount(mAccountId);
            if (account == null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgress != null) {
                            mProgress.dismiss();
                            mProgress = null;
                        }
                        Log.e(TAG, "Error updating account, no account or remote service");
                        AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(getActivity());
                        dialogBuilder.setPositiveButton(android.R.string.ok, null);
                        dialogBuilder.setTitle(R.string.generic_error_migration)
                                .setMessage(R.string.generic_error_migration_message);
                        dialogBuilder.create().show();
                    }
                });
                return null;
            }

            HashMap<String, String> details = account.getDetails();
            details.put(ConfigKey.ARCHIVE_PASSWORD.key(), mPassword);

            mAccountService.setAccountDetails(account.getAccountID(), details);

            return account.getAccountID();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            migratingAccount = false;
        }
    }
}