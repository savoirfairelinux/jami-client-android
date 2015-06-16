package cx.ring.fragments;

import java.util.HashMap;

import cx.ring.R;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.client.HomeActivity;
import cx.ring.service.ISipService;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import cx.ring.views.PasswordEditText;

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
    private PasswordEditText mPasswordView;
    private Spinner mAccountTypeView;
    private ViewGroup mFieldsSip;
    private ViewGroup mFieldsRing;

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

        mFieldsSip = (ViewGroup) inflatedView.findViewById(R.id.sip_fields);
        mFieldsRing = (ViewGroup) inflatedView.findViewById(R.id.ring_fields);

        mAliasView = (EditText) inflatedView.findViewById(R.id.alias);
        mHostnameView = (EditText) inflatedView.findViewById(R.id.hostname);
        mUsernameView = (EditText) inflatedView.findViewById(R.id.username);
        mPasswordView = (PasswordEditText) inflatedView.findViewById(R.id.password);
        mAccountTypeView = (Spinner) inflatedView.findViewById(R.id.account_type);
        mAccountTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getSelectedItem().toString().equals("RING")) {
                    mFieldsSip.setVisibility(View.GONE);
                    mFieldsRing.setVisibility(View.VISIBLE);
                } else {
                    mFieldsSip.setVisibility(View.VISIBLE);
                    mFieldsRing.setVisibility(View.GONE);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mPasswordView.getEdit_text().setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mAccountType = mAccountTypeView.getSelectedItem().toString();
                // if(actionId == EditorInfo.IME_ACTION_GO || event.getAction() == KeyEvent.KEYCODE_ENTER){
                if (mAccountType.equals("RING")) {
                    initCreation();
                } else {
                    mAlias = mAliasView.getText().toString();
                    mHostname = mHostnameView.getText().toString();
                    mUsername = mUsernameView.getText().toString();
                    mPassword = mPasswordView.getText().toString();
                    attemptCreation();
                }
                // }

                return true;
            }
        });
        inflatedView.findViewById(R.id.create_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountType = mAccountTypeView.getSelectedItem().toString();
                if (mAccountType.equals("RING")) {
                    initCreation();
                } else {
                    mAlias = mAliasView.getText().toString();
                    mHostname = mHostnameView.getText().toString();
                    mUsername = mUsernameView.getText().toString();
                    mPassword = mPasswordView.getText().toString();
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

    @SuppressWarnings("unchecked")
    private void initCreation() {

        try {

            HashMap<String, String> accountDetails = (HashMap<String, String>) mCallbacks.getService().getAccountTemplate(mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, mAccountType);
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
                    return mCallbacks.getService().addAccount(accs[0]);
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
