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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_NEXT)
                    return checkPassword(v, null);
                return false;
            }
        });
        mRingPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkPassword((TextView) v, null);
                } else {
                    //alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        mRingPasswordRepeat.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.w(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (mRingPassword.getText().length() != 0 && !checkPassword(mRingPassword, v)) {
                        ringMigrateButton.callOnClick();
                        return true;
                    }
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
                error = true;
            } else {
                pwd.setError(null);
            }
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
    }

    private class MigrateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private ProgressDialog progress = null;
        private String mAccountId;
        private String mPassword;

        MigrateAccountTask(String accountId, String password) {
            Log.w(TAG, "MigrateAccountTask");
            mAccountId = accountId;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(R.string.dialog_wait_create);
            progress.setMessage(getString(R.string.dialog_wait_create_details));
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accs) {

            final Account acc = mCallbacks.getService().getAccount(mAccountId);
            if (acc != null) {
                acc.setPassword(mPassword);
            }

            acc.stateListener = new Account.OnStateChangedListener() {
                @Override
                public void stateChanged(String state, int code) {
                    Log.w(TAG, "stateListener -> stateChanged " + state + " " + code);
                    if (!AccountDetailVolatile.STATE_INITIALIZING.contentEquals(state)) {
                        acc.stateListener = null;
                        if (progress != null) {
                            progress.dismiss();
                            progress = null;
                        }
                        //Intent resultIntent = new Intent();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                        boolean success = false;
                        switch (state) {
                            case AccountDetailVolatile.STATE_ERROR_GENERIC:
                                dialog.setTitle("Can't find account")
                                        .setMessage("Account couldn't be found on the Ring network." +
                                                "\nMake sure it was exported on Ring from an existing device, and that provided credentials are correct.");
                                break;
                            case AccountDetailVolatile.STATE_ERROR_NETWORK:
                                dialog.setTitle("Can't connect to the network")
                                        .setMessage("Could not add account because Ring coudn't connect to the distributed network. Check your device connectivity.");
                                break;
                            default:
                                dialog.setTitle("Account device added")
                                        .setMessage("You have successfully setup your Ring account on this device.");
                                success = true;
                                break;
                        }
                        AlertDialog d = dialog.show();
                        if (success) {
                            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
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


            final IDRingService remote = mCallbacks.getService().getRemoteService();
            if (remote == null) {
                Log.w(TAG, "Error updating account, remote service is null");
                return acc.getAccountID();
            }
            mCallbacks.getService().getThreadPool().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        remote.setAccountDetails(acc.getAccountID(), acc.getDetails());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

            return acc.getAccountID();
        }
    }
}