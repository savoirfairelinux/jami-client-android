package org.sflphone.views;

import java.util.HashMap;

import org.sflphone.R;
import org.sflphone.account.AccountCredentials;
import org.sflphone.account.CredentialsManager;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class CredentialsPreference extends DialogPreference {

    EditText mUsernameField, mPasswordField, mRealmField;

    public CredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected View onCreateDialogView() {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.credentials_pref, null);

        mUsernameField = (EditText) view.findViewById(R.id.credentials_username);
        mPasswordField = (EditText) view.findViewById(R.id.credentials_password);
        mRealmField = (EditText) view.findViewById(R.id.credentials_realm);

        if (getExtras().getSerializable(CredentialsManager.CURRENT_CRED) != null) {
            HashMap<String, String> details = (HashMap<String, String>) getExtras().getSerializable(CredentialsManager.CURRENT_CRED);
            mUsernameField.setText(details.get(AccountCredentials.CONFIG_ACCOUNT_USERNAME));
            mPasswordField.setText(details.get(AccountCredentials.CONFIG_ACCOUNT_PASSWORD));
            mRealmField.setText(details.get(AccountCredentials.CONFIG_ACCOUNT_REALM));
        }

        mRealmField.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String to = mRealmField.getText().toString();
                if (to.contentEquals("")) {
                    mRealmField.setError(getContext().getString(R.string.dial_error_no_number_dialed));
                }
                return true;
            }
        });

        return view;
    }

    private boolean isValid() {
        return mUsernameField.getText().length() > 0 && mPasswordField.getText().length() > 0 && mRealmField.getText().length() > 0;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final AlertDialog d = (AlertDialog) getDialog();

        // Prevent dismissing the dialog if they are any empty field
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isValid()) {
                    d.dismiss();
                    onDialogClosed(true);
                } else {
                    Toast t = Toast.makeText(getContext(), "All fields are mandatory!", Toast.LENGTH_LONG);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.show();
                }
            }
        });

        d.setButton(DialogInterface.BUTTON_NEUTRAL, "Delete", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle toReturn = getExtras();
                getOnPreferenceChangeListener().onPreferenceChange(CredentialsPreference.this, toReturn);
            }
        });

    }

    @Override
    public void onPrepareDialogBuilder(Builder builder) {

        if (getExtras().getSerializable(CredentialsManager.CURRENT_CRED) != null) {
            // If the user is editing an entry, he can delete it, otherwise don't show this button
            builder.setNeutralButton("Delete", new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Bundle toReturn = getExtras();
                    getOnPreferenceChangeListener().onPreferenceChange(CredentialsPreference.this, toReturn);
                }
            });
        }
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (getExtras().getSerializable(CredentialsManager.CURRENT_CRED) != null) {
                Bundle toReturn = getExtras();
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put(AccountCredentials.CONFIG_ACCOUNT_USERNAME, mUsernameField.getText().toString());
                fields.put(AccountCredentials.CONFIG_ACCOUNT_PASSWORD, mPasswordField.getText().toString());
                fields.put(AccountCredentials.CONFIG_ACCOUNT_REALM, mRealmField.getText().toString());
                toReturn.putSerializable(CredentialsManager.NEW_CRED, fields);
                getOnPreferenceChangeListener().onPreferenceChange(this, toReturn);
            } else {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put(AccountCredentials.CONFIG_ACCOUNT_USERNAME, mUsernameField.getText().toString());
                fields.put(AccountCredentials.CONFIG_ACCOUNT_PASSWORD, mPasswordField.getText().toString());
                fields.put(AccountCredentials.CONFIG_ACCOUNT_REALM, mRealmField.getText().toString());
                getOnPreferenceChangeListener().onPreferenceChange(this, new AccountCredentials(fields));
            }

        }
    }

}
