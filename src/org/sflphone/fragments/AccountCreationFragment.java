package org.sflphone.fragments;

import java.util.HashMap;

import org.sflphone.R;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;
import org.sflphone.client.SettingsActivity;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class AccountCreationFragment extends Fragment {

    // Values for email and password at the time of the login attempt.
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;

    // UI references.
    private EditText mAliasView;
    private EditText mHostnameView;
    private EditText mUsernameView;
    private EditText mPasswordView;

    private Callbacks mCallbacks = sDummyCallbacks;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }
    };

    public interface Callbacks {

        public ISipService getService();

    }

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
                // if(actionId == EditorInfo.IME_ACTION_GO || event.getAction() == KeyEvent.KEYCODE_ENTER){
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
                // }

                return true;
            }
        });
        inflatedView.findViewById(R.id.create_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
            }
        });

//         inflatedView.findViewById(R.id.dev_account).setVisibility(View.GONE); // Hide this button in release apk
        inflatedView.findViewById(R.id.dev_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDevAccount();
            }

            private void createDevAccount() {
                mUsername = mUsernameView.getText().toString();
                if (TextUtils.isEmpty(mUsername)) {
                    mUsernameView.setError(getString(R.string.error_field_required));
                    mUsernameView.requestFocus();
                    return;
                } else {
                    mAlias = mUsername;
                    mHostname = "192.95.9.63";
                    mPassword = "sfl_u" + mUsername;
                    attemptCreation();
                }

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

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
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

    private void initCreation() {

        HashMap<String, String> accountDetails = new HashMap<String, String>();

        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_TYPE);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, mAlias);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, mHostname);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, mUsername);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, mPassword);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, "");
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_REALM, AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_REALM);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_ENABLE);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, mPassword);
        accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_USERAGENT, AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_USERAGENT);

        accountDetails.put(AccountDetailAdvanced.CONFIG_LOCAL_PORT, AccountDetailAdvanced.CONFIG_DEFAULT_LOCAL_PORT);
        accountDetails.put(AccountDetailAdvanced.CONFIG_LOCAL_INTERFACE, AccountDetailAdvanced.CONFIG_DEFAULT_INTERFACE);
        accountDetails.put(AccountDetailAdvanced.CONFIG_PUBLISHED_PORT, AccountDetailAdvanced.CONFIG_DEFAULT_PUBLISHED_PORT);
        accountDetails.put(AccountDetailAdvanced.CONFIG_PUBLISHED_ADDRESS, AccountDetailAdvanced.CONFIG_DEFAULT_ADDRESS);
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, AccountDetailAdvanced.CONFIG_DEFAULT_REGISTRATION_EXPIRE);
        accountDetails.put(AccountDetailAdvanced.CONFIG_STUN_SERVER, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_AUTOANSWER, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, AccountDetailAdvanced.CONFIG_DEFAULT_DTMF_TYPE);
        accountDetails.put(AccountDetailAdvanced.CONFIG_KEEP_ALIVE_ENABLED, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailAdvanced.CONFIG_STUN_SERVER, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_PUBLISHED_SAMEAS_LOCAL, AccountDetailAdvanced.CONFIG_DEFAULT_PUBLISHED_SAMEAS_LOCAL);
        accountDetails.put(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, "");
        accountDetails.put(AccountDetailAdvanced.CONFIG_STUN_ENABLE, AccountDetailAdvanced.FALSE_STR);

        accountDetails.put(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE, "");
        accountDetails.put(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK, "");
        accountDetails.put(AccountDetailSrtp.CONFIG_SRTP_ENABLE, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS, "");
        accountDetails.put(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE, "");
        accountDetails.put(AccountDetailSrtp.CONFIG_ZRTP_HELLO_HASH, "");
        accountDetails.put(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING, "");

        accountDetails.put(AccountDetailTls.CONFIG_TLS_CIPHERS, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_LISTENER_PORT, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_METHOD, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_ENABLE, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailTls.CONFIG_TLS_PASSWORD, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE, "");

        accountDetails.put(AccountDetailTls.CONFIG_TLS_SERVER_NAME, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, AccountDetailAdvanced.FALSE_STR);
        accountDetails.put(AccountDetailTls.CONFIG_TLS_LISTENER_PORT, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_VERIFY_CLIENT, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE, "");
        accountDetails.put(AccountDetailTls.CONFIG_TLS_VERIFY_SERVER, "");

        createNewAccount(accountDetails);

        Intent resultIntent = new Intent(getActivity(), SettingsActivity.class);
        getActivity().setResult(Activity.RESULT_OK, resultIntent);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(resultIntent);
        getActivity().finish();

    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        try {

            mCallbacks.getService().addAccount(accountDetails);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
