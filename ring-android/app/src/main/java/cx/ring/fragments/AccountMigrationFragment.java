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
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.HashMap;

import cx.ring.R;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.model.account.AccountDetailVolatile;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class AccountMigrationFragment extends Fragment {
    static final String TAG = AccountMigrationFragment.class.getSimpleName();

    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private String mAccountId;

    // UI references.
    private EditText mRingPassword;
    private EditText mRingPasswordRepeat;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private boolean migratingAccount = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_account_migration, parent, false);

        mRingPassword = (EditText) inflatedView.findViewById(R.id.ring_password);
        mRingPasswordRepeat = (EditText) inflatedView.findViewById(R.id.ring_password_repeat);

        final Button ringMigrateButton = (Button) inflatedView.findViewById(R.id.ring_migrate_btn);

        mRingPassword.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                return actionId == EditorInfo.IME_ACTION_NEXT && checkPassword(v, null);
            }
        });
        mRingPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkPassword((TextView) v, null);
                }
            }
        });
        mRingPasswordRepeat.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE
                        && mRingPassword.getText().length() != 0
                        && !checkPassword(mRingPassword, v)) {
                        ringMigrateButton.callOnClick();
                        return true;
                }
                return false;
            }
        });

        ringMigrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkPassword(mRingPassword, mRingPasswordRepeat)) {
                    initAccountMigration(mRingPassword.getText().toString());
                }
            }
        });

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() != null) {
            mAccountId = getArguments().getString(ACCOUNT_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.update_account);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
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
                pwd.requestFocus();
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

    private class MigrateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private ProgressDialog progress = null;
        private final String mAccountId;
        private final String mPassword;

        MigrateAccountTask(String accountId, String password) {
            Log.d(TAG, "MigrateAccountTask");
            mAccountId = accountId;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(R.string.dialog_wait_update);
            progress.setMessage(getString(R.string.dialog_wait_update_details));
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accs) {

            final Account account = mCallbacks.getService().getAccount(mAccountId);
            final IDRingService remote = mCallbacks.getService().getRemoteService();
            if (account == null || remote == null) {
                Log.e(TAG, "Error updating account, no account or remote service");
                return null;
            }

            account.stateListener = new Account.OnStateChangedListener() {
                @Override
                public void stateChanged(String state, int code) {
                    Log.d(TAG, "stateListener -> stateChanged " + state + " " + code);
                    if (!AccountDetailVolatile.STATE_INITIALIZING.contentEquals(state)) {
                        if (progress != null) {
                            progress.dismiss();
                            progress = null;
                        }
                        AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(getActivity());
                        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                        boolean success = false;
                        switch (state) {
                            case AccountDetailVolatile.STATE_ERROR_GENERIC:
                                dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                                        .setMessage(R.string.account_cannot_be_found_message);
                                break;
                            case AccountDetailVolatile.STATE_ERROR_NETWORK:
                                dialogBuilder.setTitle(R.string.account_no_network_title)
                                        .setMessage(R.string.account_no_network_message);
                                break;
                            case AccountDetailVolatile.STATE_REGISTERED:
                                dialogBuilder.setTitle(R.string.account_device_updated_title)
                                        .setMessage(R.string.account_device_updated_message);
                                success = true;
                                account.stateListener = null;
                                break;
                            default:
                                return;
                        }
                        android.support.v7.app.AlertDialog dialogSuccess = dialogBuilder.show();
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
            };

            HashMap<String, String> details = account.getDetails();
            details.put(AccountDetailBasic.CONFIG_ARCHIVE_PASSWORD, mPassword);

            try {
                remote.setAccountDetails(account.getAccountID(), details);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while setting ARCHIVE_PASSWORD", e);
            }

            return account.getAccountID();
        }
    }
}