/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;

public class AccountManagementFragment extends PreferenceFragment
{
    static final String TAG = "AccountManagementFragment";
    static final String CURRENT_VALUE = "Current value:: "; 
    static final String ALIAS_KEY = "ALIAS";
    static final String HOSTNAME_KEY = "HOSTNAME"; 
    static final String USERNAME_KEY = "USERNAME";
    static final String PROXY_KEY = "PROXY"; 
    static final String REGISTRATION_KEY = "REGISTRATION";
    static final String NETWORK_KEY = "NETWORK"; 
    static final String SECURITY_KEY = "SECURITY";
    static final String TLS_KEY = "TLS";
    static final String SRTP_KEY = "SRTP";
    private ISipService service;

    public AccountManagementFragment(ISipService s)
    {
        service = s;
    } 

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");

        setPreferenceScreen(getAccountPreferenceScreen());
    }

    boolean onTextEditPreferenceChange(Preference preference, Object newValue)
    {
        Log.i(TAG, "Account Preference Changed " + preference.getTitle());

        preference.setSummary(CURRENT_VALUE + (CharSequence)newValue);

        return true;
    }

    Preference.OnPreferenceChangeListener changeTextEditListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(CURRENT_VALUE + (CharSequence)newValue);
            return true;
        }
    };


    Preference.OnPreferenceClickListener preferenceClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            return false;
        }
    };

    HashMap getAccountDetails()
    {
        HashMap accountDetails = null;
        try {
            accountDetails = (HashMap) service.getAccountDetails("IP2IP");
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
        /*
        accountDetails.put(ALIAS_KEY, "Test-Account");
        accountDetails.put(HOSTNAME_KEY, "office.srv.com");
        accountDetails.put(USERNAME_KEY, "181");
        accountDetails.put(PROXY_KEY, "none");
        accountDetails.put(REGISTRATION_KEY, "500");
        accountDetails.put(NETWORK_KEY, "eth0");
        accountDetails.put(SECURITY_KEY, "disabled");
        accountDetails.put(TLS_KEY, "disabled");
        accountDetails.put(SRTP_KEY, "disabled");
        */

        return accountDetails;
    }

    public PreferenceScreen getAccountPreferenceScreen()
    {
        Activity currentContext = getActivity();
        HashMap accountDetails = getAccountDetails();
        Log.i(TAG, "GetAccountDetails: " + accountDetails.size());

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(currentContext);

        // Inline preference
        PreferenceCategory accountPrefCat = new PreferenceCategory(currentContext);
        accountPrefCat.setTitle(R.string.account_preferences);
        root.addPreference(accountPrefCat);

        // Alias
        EditTextPreference accountAliasPref = new EditTextPreference(currentContext);
        accountAliasPref.setDialogTitle(R.string.dialogtitle_account_alias_field);
        accountAliasPref.setPersistent(false);
        accountAliasPref.setTitle(R.string.title_account_alias_field);
        accountAliasPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
        accountAliasPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountAliasPref);

        // Hostname
        EditTextPreference accountHostnamePref = new EditTextPreference(currentContext);
        accountHostnamePref.setDialogTitle(R.string.dialogtitle_account_hostname_field);
        accountHostnamePref.setPersistent(false);
        accountHostnamePref.setTitle(R.string.title_account_hostname_field);
        accountHostnamePref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
        accountHostnamePref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountHostnamePref);

        // Username
        EditTextPreference accountUsernamePref = new EditTextPreference(currentContext);
        accountUsernamePref.setDialogTitle(R.string.dialogtitle_account_username_field);
        accountUsernamePref.setPersistent(false);
        accountUsernamePref.setTitle(R.string.title_account_username_field);
        accountUsernamePref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
        accountUsernamePref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountUsernamePref);

        // Proxy
        EditTextPreference accountProxyPref = new EditTextPreference(currentContext); 
        accountProxyPref.setDialogTitle(R.string.dialogtitle_account_proxy_field);
        accountProxyPref.setPersistent(false);
        accountProxyPref.setTitle(R.string.title_account_proxy_field);
        accountProxyPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
        accountProxyPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountProxyPref);

        // Registration Timeout
        EditTextPreference accountRegistrationPref = new EditTextPreference(currentContext); 
        accountRegistrationPref.setDialogTitle(R.string.dialogtitle_account_registration_field);
        accountRegistrationPref.setPersistent(false);
        accountRegistrationPref.setTitle(R.string.title_account_registration_field);
        accountRegistrationPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
        accountRegistrationPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountRegistrationPref);

        // Netowrk interface
        EditTextPreference accountNetworkPref = new EditTextPreference(currentContext); 
        accountNetworkPref.setDialogTitle(R.string.dialogtitle_account_network_field);
        accountNetworkPref.setPersistent(false);
        accountNetworkPref.setTitle(R.string.title_account_network_field);
        accountNetworkPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
        accountNetworkPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountNetworkPref);

        // Account stun server
        EditTextPreference accountSecurityPref = new EditTextPreference(currentContext); 
        accountSecurityPref.setDialogTitle(R.string.dialogtitle_account_security_field);
        accountSecurityPref.setPersistent(false);
        accountSecurityPref.setTitle(R.string.title_account_security_field);
        accountSecurityPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_STUN_SERVER));
        accountSecurityPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountSecurityPref);

        // Account tls feature
        EditTextPreference accountTlsPref = new EditTextPreference(currentContext); 
        accountTlsPref.setDialogTitle(R.string.dialogtitle_account_tls_field);
        accountTlsPref.setPersistent(false);
        accountTlsPref.setTitle(R.string.title_account_tls_field);
        accountTlsPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_TLS_ENABLE));
        accountTlsPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountTlsPref);

        // Account srtp feature
        EditTextPreference accountSrtpPref = new EditTextPreference(currentContext); 
        accountSrtpPref.setDialogTitle(R.string.dialogtitle_account_srtp_field);
        accountSrtpPref.setPersistent(false);
        accountSrtpPref.setTitle(R.string.title_account_srtp_field);
        accountSrtpPref.setSummary(CURRENT_VALUE + accountDetails.get(ServiceConstants.CONFIG_SRTP_ENABLE));
        accountSrtpPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountSrtpPref);

        return root;
    }
}
