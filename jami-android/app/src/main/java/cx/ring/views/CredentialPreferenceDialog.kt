/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.views

import android.content.Context
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.preference.PreferenceDialogFragmentCompat
import cx.ring.R
import net.jami.model.AccountCredentials

class CredentialPreferenceDialog : PreferenceDialogFragmentCompat() {
    private var mUsernameField: EditText? = null
    private var mPasswordField: EditText? = null
    private var mRealmField: EditText? = null
    private var creds: AccountCredentials? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creds = if (savedInstanceState == null) {
            credentialsPreference.creds
        } else {
            savedInstanceState.getSerializable(SAVE_STATE_TEXT) as AccountCredentials?
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVE_STATE_TEXT, creds)
    }

    override fun onCreateDialogView(context: Context): View? {
        return LayoutInflater.from(context).inflate(R.layout.credentials_pref, null)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mUsernameField = view.findViewById(R.id.credentials_username)
        mPasswordField = view.findViewById(R.id.credentials_password)
        mRealmField = view.findViewById(R.id.credentials_realm)
        checkNotNull(mUsernameField) { "Dialog view must contain an EditText with id @id/credentials_username" }
        creds?.let {
            mUsernameField?.setText(it.username)
            mPasswordField?.setText(it.password)
            mRealmField?.setText(it.realm)
        }
    }

    private val credentialsPreference: CredentialsPreference
        private get() = this.preference as CredentialsPreference

    override fun needInputMethod(): Boolean {
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val newcreds = AccountCredentials(
            mUsernameField!!.text.toString(),
            mPasswordField!!.text.toString(),
            mRealmField!!.text.toString()
        )
        if (positiveResult) {
            if (credentialsPreference.callChangeListener(Pair(creds, newcreds))) {
                credentialsPreference.creds = newcreds
            }
        }
    }

    companion object {
        private const val SAVE_STATE_TEXT = "CredentialPreferenceDialog.creds"
        fun newInstance(key: String?): CredentialPreferenceDialog {
            val fragment = CredentialPreferenceDialog()
            val b = Bundle(1)
            b.putString("key", key)
            fragment.arguments = b
            return fragment
        }
    }
}
