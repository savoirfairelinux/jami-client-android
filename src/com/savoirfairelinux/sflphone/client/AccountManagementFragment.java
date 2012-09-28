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
import android.preference.ListPreference;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;
import java.util.ArrayList;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;

public class AccountManagementFragment extends PreferenceFragment
{
    static final String TAG = "AccountManagementFragment";
    static final String CURRENT_VALUE = "Current value:: ";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    private ISipService service;
    HashMap<String, String> mAccountDetails = null;
    ArrayList<String> mAccountList = null;
    ArrayList<PreferenceEntry> basicDetailKeys = new ArrayList<PreferenceEntry>();
    ArrayList<PreferenceEntry> advancedDetailKeys = new ArrayList<PreferenceEntry>();
    Activity context = getActivity();

    public AccountManagementFragment(ISipService s)
    {
        service = s;

        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_TYPE,
                            R.string.account_type_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ALIAS,
                            R.string.account_alias_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ENABLE,
                            R.string.account_enabled_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME,
                            R.string.account_hostname_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERNAME,
                            R.string.account_username_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_ROUTESET,
                            R.string.account_routeset_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_PASSWORD,
                            R.string.account_password_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REALM,
                            R.string.account_realm_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM,
                            R.string.account_useragent_label));
        basicDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_USERAGENT,
                            R.string.account_autoanswer_label));

        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE,
                            R.string.account_registration_exp_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS,
                            R.string.account_registration_status_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE,
                            R.string.account_registration_code_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC,
                            R.string.account_registration_state_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_CREDENTIAL_NUMBER,
                            R.string.account_credential_count_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE,
                            R.string.account_config_dtmf_type_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_PATH,
                            R.string.account_ringtone_path_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_RINGTONE_ENABLED,
                            R.string.account_ringtone_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED,
                            R.string.account_keep_alive_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER,
                            R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_INTERFACE,
                            R.string.account_local_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_INTERFACE,
                            R.string.account_account_interface_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL,
                            R.string.account_published_same_as_local_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_LOCAL_PORT,
                            R.string.account_local_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_PORT,
                            R.string.account_published_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_PUBLISHED_ADDRESS,
                            R.string.account_published_address_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_DISPLAY_NAME,
                            R.string.account_displayname_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_SERVER,
                            R.string.account_stun_server_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_STUN_ENABLE,
                            R.string.account_stun_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENABLE,
                            R.string.account_srtp_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE,
                            R.string.account_srtp_exchange_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO,
                            R.string.account_encryption_algo_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK,
                            R.string.account_srtp_fallback_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_HELLO_HASH,
                            R.string.account_hello_hash_enable_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS,
                            R.string.account_display_sas_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING,
                            R.string.account_not_supported_warning_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE,
                            R.string.account_display_sas_once_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_LISTENER_PORT,
                            R.string.account_listener_port_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_ENABLE,
                            R.string.account_tls_enabled_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CA_LIST_FILE,
                            R.string.account_tls_certificate_list_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE,
                            R.string.account_tls_certificate_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE,
                            R.string.account_tls_private_key_file_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_PASSWORD,
                            R.string.account_tls_password_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_METHOD,
                            R.string.account_tls_method_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_CIPHERS,
                            R.string.account_tls_ciphers_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_SERVER_NAME,
                            R.string.account_tls_server_name_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_SERVER,
                            R.string.account_tls_verify_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT,
                            R.string.account_tls_verify_client_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE,
                            R.string.account_tls_require_client_certificat_label));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC,
                            R.string.account_tls_negotiation_timeout_sec));
        advancedDetailKeys.add(new PreferenceEntry(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC,
                            R.string.account_tls_negotiation_timeout_msec));
    } 

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");

        // setPreferenceScreen(getAccountPreferenceScreen());
        setPreferenceScreen(getAccountListPreferenceScreen());
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

    ArrayList<String> getAccountList()
    {
        ArrayList<String> accountList = null;
        try {
            accountList = (ArrayList) service.getAccountList(); 
        } catch (RemoteException e) {
           Log.e(TAG, "Cannot call service method", e); 
        }

        // Remove the default account from list
        accountList.remove(DEFAULT_ACCOUNT_ID);

        return accountList;
    }

    HashMap getAccountDetails(String accountID)
    {
        HashMap accountDetails = null;
        try {
            accountDetails = (HashMap) service.getAccountDetails(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return accountDetails;
    }

    public PreferenceScreen getAccountListPreferenceScreen()
    {
        Activity currentContext = getActivity();

        mAccountList = getAccountList();
        Log.i(TAG, "GetAccountList: " + mAccountList);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(currentContext);

        // Default account category
        PreferenceCategory defaultAccountCat = new PreferenceCategory(currentContext);
        defaultAccountCat.setTitle(R.string.default_account_category);
        root.addPreference(defaultAccountCat);

        root.addPreference(getAccountPreferenceScreen(DEFAULT_ACCOUNT_ID));

        // Account list category
        PreferenceCategory accountListCat = new PreferenceCategory(currentContext);
        accountListCat.setTitle(R.string.default_account_category);
        root.addPreference(accountListCat);

        for(String s : mAccountList)
            root.addPreference(getAccountPreferenceScreen(s));

        return root;
    }

    public PreferenceScreen getAccountPreferenceScreen(String accountID)
    {
        Activity currentContext = getActivity();

        mAccountDetails = getAccountDetails(accountID);
        Log.i(TAG, "GetAccountDetails: " + mAccountDetails.size());

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(currentContext);

        root.setTitle(mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));

        // Inline preference
        PreferenceCategory accountPrefCat = new PreferenceCategory(currentContext);
        accountPrefCat.setTitle(R.string.account_preferences);
        root.addPreference(accountPrefCat);

        // 
        for(PreferenceEntry entry : basicDetailKeys)
        {
            EditTextPreference accountAliasPref = new EditTextPreference(currentContext);
            accountAliasPref.setDialogTitle(entry.mLabelId);
            accountAliasPref.setPersistent(false);
            accountAliasPref.setTitle(entry.mLabelId);
            accountAliasPref.setSummary(CURRENT_VALUE + mAccountDetails.get(entry.mKey));
            accountAliasPref.setOnPreferenceChangeListener(changeTextEditListener);
            accountPrefCat.addPreference(accountAliasPref);
        }

        // 
        for(PreferenceEntry entry : advancedDetailKeys)
        {
            EditTextPreference accountAliasPref = new EditTextPreference(currentContext);
            accountAliasPref.setDialogTitle(entry.mLabelId);
            accountAliasPref.setPersistent(false);
            accountAliasPref.setTitle(entry.mLabelId);
            accountAliasPref.setSummary(CURRENT_VALUE + mAccountDetails.get(entry.mKey));
            accountAliasPref.setOnPreferenceChangeListener(changeTextEditListener);
            accountPrefCat.addPreference(accountAliasPref);
        }

        /*
        // Alias
        EditTextPreference accountAliasPref = new EditTextPreference(currentContext);
        accountAliasPref.setDialogTitle(R.string.dialogtitle_account_alias_field);
        accountAliasPref.setPersistent(false);
        accountAliasPref.setTitle(R.string.title_account_alias_field);
        accountAliasPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
        accountAliasPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountAliasPref);

        // Hostname
        EditTextPreference accountHostnamePref = new EditTextPreference(currentContext);
        accountHostnamePref.setDialogTitle(R.string.dialogtitle_account_hostname_field);
        accountHostnamePref.setPersistent(false);
        accountHostnamePref.setTitle(R.string.title_account_hostname_field);
        accountHostnamePref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
        accountHostnamePref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountHostnamePref);

        // Username
        EditTextPreference accountUsernamePref = new EditTextPreference(currentContext);
        accountUsernamePref.setDialogTitle(R.string.dialogtitle_account_username_field);
        accountUsernamePref.setPersistent(false);
        accountUsernamePref.setTitle(R.string.title_account_username_field);
        accountUsernamePref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
        accountUsernamePref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountUsernamePref);

        // Proxy
        EditTextPreference accountProxyPref = new EditTextPreference(currentContext); 
        accountProxyPref.setDialogTitle(R.string.dialogtitle_account_proxy_field);
        accountProxyPref.setPersistent(false);
        accountProxyPref.setTitle(R.string.title_account_proxy_field);
        accountProxyPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
        accountProxyPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountProxyPref);

        // Registration Timeout
        EditTextPreference accountRegistrationPref = new EditTextPreference(currentContext); 
        accountRegistrationPref.setDialogTitle(R.string.dialogtitle_account_registration_field);
        accountRegistrationPref.setPersistent(false);
        accountRegistrationPref.setTitle(R.string.title_account_registration_field);
        accountRegistrationPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
        accountRegistrationPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountRegistrationPref);

        // Netowrk interface
        EditTextPreference accountNetworkPref = new EditTextPreference(currentContext); 
        accountNetworkPref.setDialogTitle(R.string.dialogtitle_account_network_field);
        accountNetworkPref.setPersistent(false);
        accountNetworkPref.setTitle(R.string.title_account_network_field);
        accountNetworkPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
        accountNetworkPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountNetworkPref);

        // Account stun server
        EditTextPreference accountSecurityPref = new EditTextPreference(currentContext); 
        accountSecurityPref.setDialogTitle(R.string.dialogtitle_account_security_field);
        accountSecurityPref.setPersistent(false);
        accountSecurityPref.setTitle(R.string.title_account_security_field);
        accountSecurityPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_STUN_SERVER));
        accountSecurityPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountSecurityPref);

        // Account tls feature
        EditTextPreference accountTlsPref = new EditTextPreference(currentContext); 
        accountTlsPref.setDialogTitle(R.string.dialogtitle_account_tls_field);
        accountTlsPref.setPersistent(false);
        accountTlsPref.setTitle(R.string.title_account_tls_field);
        accountTlsPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_TLS_ENABLE));
        accountTlsPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountTlsPref);

        // Account srtp feature
        EditTextPreference accountSrtpPref = new EditTextPreference(currentContext); 
        accountSrtpPref.setDialogTitle(R.string.dialogtitle_account_srtp_field);
        accountSrtpPref.setPersistent(false);
        accountSrtpPref.setTitle(R.string.title_account_srtp_field);
        accountSrtpPref.setSummary(CURRENT_VALUE + mAccountDetails.get(ServiceConstants.CONFIG_SRTP_ENABLE));
        accountSrtpPref.setOnPreferenceChangeListener(changeTextEditListener);
        accountPrefCat.addPreference(accountSrtpPref);
        */

        return root;
    }

    public static class PreferenceEntry
    {
        public String mKey;
        public int mLabelId;

        public PreferenceEntry(String key, int labelId)
        {
            mKey = key;
            mLabelId = labelId;
        }
    }
}
