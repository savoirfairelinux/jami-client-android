/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.views;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceDialogFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import cx.ring.R;
import cx.ring.model.account.AccountCredentials;

public class CredentialPreferenceDialog extends PreferenceDialogFragment {
    private static final String SAVE_STATE_TEXT = "CredentialPreferenceDialog.creds";
    private EditText mUsernameField;
    private EditText mPasswordField;
    private EditText mRealmField;
    private AccountCredentials creds;

    public CredentialPreferenceDialog() {
    }

    public static CredentialPreferenceDialog newInstance(String key) {
        CredentialPreferenceDialog fragment = new CredentialPreferenceDialog();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            creds = this.getCredentialsPreference().getCreds();
        } else {
            creds = savedInstanceState.getParcelable(SAVE_STATE_TEXT);
        }
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVE_STATE_TEXT, creds);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        //builder.setPositiveButton(android.R.string.ok, null);
        //builder.setNegativeButton(android.R.string.cancel, null);
        //builder.setMessage(R.string.account_credentials_dialog_mesage);
    }

    @Override
    protected View onCreateDialogView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.credentials_pref, null);
    }

    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mUsernameField = (EditText) view.findViewById(R.id.credentials_username);
        mPasswordField = (EditText) view.findViewById(R.id.credentials_password);
        mRealmField = (EditText) view.findViewById(R.id.credentials_realm);
        if(mUsernameField == null) {
            throw new IllegalStateException("Dialog view must contain an EditText with id @id/credentials_username");
        } else if (creds != null) {
            mUsernameField.setText(creds.getUsername());
            mPasswordField.setText(creds.getPassword());
            mRealmField.setText(creds.getRealm());
        }
    }

    private CredentialsPreference getCredentialsPreference() {
        return (CredentialsPreference)this.getPreference();
    }

    protected boolean needInputMethod() {
        return true;
    }

    public void onDialogClosed(boolean positiveResult) {
        AccountCredentials newcreds = new AccountCredentials(
                mUsernameField.getText().toString(),
                mPasswordField.getText().toString(),
                mRealmField.getText().toString());
        if(positiveResult) {
            if(this.getCredentialsPreference().callChangeListener(new Pair<>(creds, newcreds))) {
                this.getCredentialsPreference().setCreds(newcreds);
            }
        }
    }
}
