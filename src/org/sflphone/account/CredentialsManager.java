/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.account;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.model.Account;
import org.sflphone.views.CredentialsPreference;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;

public class CredentialsManager {

    PreferenceScreen mScreen;
    public static final String CURRENT_CRED = "current_cred";
    public static final String NEW_CRED = "new_cred";
    private Context mContext;
    private Account mAccount;

    
    
    public void onCreate(Context cont, PreferenceScreen preferenceScreen, Account acc) {
        mContext = cont;
        mScreen = preferenceScreen;
        mAccount = acc;
    }

    public void reloadCredentials() {
        removeAllCredentials();
        addAllCredentials();
    }

    public void setAddCredentialListener() {
        mScreen.findPreference("Add.credentials").setOnPreferenceChangeListener(addCredentialListener);
    }

    public void setEditCredentialListener() {
        mScreen.findPreference("Add.credentials").setOnPreferenceChangeListener(addCredentialListener);
    }

    private void addAllCredentials() {
        ArrayList<AccountCredentials> credentials = mAccount.getCredentials();
        for (AccountCredentials cred : credentials) {
            CredentialsPreference toAdd = new CredentialsPreference(mContext, null);
            toAdd.setKey("credential");
            toAdd.setTitle(cred.getDetailString(AccountCredentials.CONFIG_ACCOUNT_USERNAME));
            toAdd.setSummary(cred.getDetailString(AccountCredentials.CONFIG_ACCOUNT_REALM));
            toAdd.getExtras().putSerializable(CURRENT_CRED, cred.getDetailsHashMap());
            toAdd.setOnPreferenceChangeListener(editCredentialListener);
            toAdd.setIcon(null);
            mScreen.addPreference(toAdd);
        }

    }

    private void removeAllCredentials() {
        Preference toRemove = mScreen.findPreference("credential");
        while (mScreen.findPreference("credential") != null) {
            mScreen.removePreference(toRemove);
            toRemove = mScreen.findPreference("credential");
        }
    }
    
    private OnPreferenceChangeListener editCredentialListener = new OnPreferenceChangeListener() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            
            // We need the old and new value to correctly edit the list of credentials
            Bundle result = (Bundle) newValue;
            mAccount.removeCredential(new AccountCredentials((HashMap<String, String>) result.get(CURRENT_CRED)));
            
            if(result.get(NEW_CRED) != null){
                // There is a new value for this credentials it means it has been edited (otherwise deleted)
                mAccount.addCredential(new AccountCredentials((HashMap<String, String>) result.get(NEW_CRED)));
            }
            mAccount.notifyObservers();
            reloadCredentials();
            return false;
        }
    };
    
    private OnPreferenceChangeListener addCredentialListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            mAccount.addCredential((AccountCredentials) newValue);
            mAccount.notifyObservers();
            reloadCredentials();
            return false;
        }
    };



}
