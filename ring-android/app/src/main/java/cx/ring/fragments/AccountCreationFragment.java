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

import java.util.HashMap;
import java.util.Map;

import cx.ring.R;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.client.HomeActivity;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import cx.ring.service.LocalService;

public class AccountCreationFragment extends Fragment {
    static final String TAG = AccountCreationFragment.class.getSimpleName();

    // Values for email and password at the time of the login attempt.
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;
    private String mAccountType;

    // UI references.
    private EditText mAliasView;
    private EditText mHostnameView;
    private EditText mUsernameView;
    private EditText mPasswordView;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_account_creation, parent, false);

        mAliasView = (EditText) inflatedView.findViewById(R.id.alias);
        mHostnameView = (EditText) inflatedView.findViewById(R.id.hostname);
        mUsernameView = (EditText) inflatedView.findViewById(R.id.username);
        mPasswordView = (EditText) inflatedView.findViewById(R.id.password);

        mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mAccountType = "SIP";
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
                return true;
            }
        });
        inflatedView.findViewById(R.id.create_ring_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountType = "RING";
                initCreation();
            }
        });
        inflatedView.findViewById(R.id.create_sip_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountType = "SIP";
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
            }
        });

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.ab_account_creation);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptCreation() {

        // Reset errors.
        mAliasView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mHostname)) {
            mHostnameView.setError(getString(R.string.error_field_required));
            focusView = mHostnameView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mAlias)) {
            mAliasView.setError(getString(R.string.error_field_required));
            focusView = mAliasView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            initCreation();

        }
    }

    @SuppressWarnings("unchecked")
    private void initCreation() {

        try {

            HashMap<String, String> accountDetails = (HashMap<String, String>) mCallbacks.getRemoteService().getAccountTemplate(mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_VIDEO_ENABLED, "true");
            if (mAccountType.equals("RING")) {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, "Ring");
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, "bootstrap.ring.cx");
            } else {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, mAlias);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, mHostname);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, mUsername);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, mPassword);
            }

            createNewAccount(accountDetails);

        } catch (RemoteException e) {
            Toast.makeText(getActivity(), "Error creating account", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        //noinspection unchecked
        new AsyncTask<HashMap<String, String>, Void, String>() {
            private ProgressDialog progress = null;

            @Override
            protected void onPreExecute() {
                progress = new ProgressDialog(getActivity());
                progress.setTitle(R.string.dialog_wait_create);
                progress.setMessage(getString(R.string.dialog_wait_create_details));
                progress.setCancelable(false);
                progress.setCanceledOnTouchOutside(false);
                progress.show();
            }

            @Override
            protected String doInBackground(HashMap<String, String>... accs) {
                try {
                    return mCallbacks.getRemoteService().addAccount(accs[0]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (progress != null) {
                    progress.dismiss();
                    progress = null;
                }
                Intent resultIntent = new Intent(getActivity(), HomeActivity.class);
                getActivity().setResult(s.isEmpty() ? Activity.RESULT_CANCELED : Activity.RESULT_OK, resultIntent);
                //resultIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                //startActivity(resultIntent);
                getActivity().finish();
            }
        }.execute(accountDetails);
    }

}
