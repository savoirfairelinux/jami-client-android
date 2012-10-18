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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;
import com.savoirfairelinux.sflphone.utils.AccountDetail;
import com.savoirfairelinux.sflphone.utils.AccountDetailsHandler;
import com.savoirfairelinux.sflphone.utils.AccountDetailBasic;
import com.savoirfairelinux.sflphone.utils.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.utils.AccountDetailSrtp;
import com.savoirfairelinux.sflphone.utils.AccountDetailTls;

public class AccountManagementFragment extends PreferenceFragment
{
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    private ISipService service;

    HashMap<String,HashMap<String,String>> mAccountList = new HashMap<String,HashMap<String,String>>();
    ArrayList<AccountDetail.PreferenceEntry> basicDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> advancedDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> srtpDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> tlsDetailKeys;
    AccountDetailBasic basicDetails;
    AccountDetailAdvanced advancedDetails;
    AccountDetailSrtp srtpDetails;
    AccountDetailTls tlsDetails;
    PreferenceScreen mRoot;

    Activity context = getActivity();

    public AccountManagementFragment(ISipService s)
    {
        service = s;

        basicDetailKeys =  AccountDetailBasic.getPreferenceEntries();
        advancedDetailKeys = AccountDetailAdvanced.getPreferenceEntries();
        srtpDetailKeys = AccountDetailSrtp.getPreferenceEntries();
        tlsDetailKeys = AccountDetailTls.getPreferenceEntries();

        basicDetails = new AccountDetailBasic();
        advancedDetails = new AccountDetailAdvanced();
        srtpDetails = new AccountDetailSrtp();
        tlsDetails = new AccountDetailTls();   
    } 

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");

        setPreferenceScreen(getAccountListPreferenceScreen());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("accounts-changed"));
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.i(TAG, "onStop");

        ArrayList<String> accountList = getAccountList();

        try {
            for(String s : accountList) {
                Log.i(TAG, "         set details for " + s);
                HashMap<String, String> accountDetails = mAccountList.get(s);
                if(accountDetails != null) {
                    service.setAccountDetails(s, accountDetails);
                }
            }
        } catch (RemoteException e) {
           Log.e(TAG, "Cannot call service method", e); 
        }
    }

    @Override
    public void onDestroy()
    {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCOUNT_CREATE_REQUEST) {
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            ArrayList<String> newList = (ArrayList<String>) getAccountList();
            Set<String> currentList = (Set<String>) mAccountList.keySet();
            for(String s : newList) {
                if(!currentList.contains(s)) {
                    Log.i("receiver", "ADDING ACCOUNT!!!!!! " + s);
                    mRoot.addPreference(getAccountPreferenceScreen(s));
                }
            }
        }
    };


    Preference.OnPreferenceChangeListener changeBasicTextEditListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label) + (CharSequence)newValue);
            mAccountList.get(preference.getKey()).put(basicDetailKeys.get(preference.getOrder()).mKey, ((CharSequence)newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeAdvancedTextEditListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label) + (CharSequence)newValue);
            mAccountList.get(preference.getKey()).put(advancedDetailKeys.get(preference.getOrder()).mKey, ((CharSequence)newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceClickListener launchAccountCreationOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getTitle() == "Touch to Create New Account") {
                launchAccountCreationPanel(preference);
            }
            return true;
        }
    };

    private void launchAccountCreationPanel(Preference preference)
    {
        Log.i("MainSandbox", "launchPreferencePanel");
        Intent intent = preference.getIntent();
        startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
    } 

    private ArrayList<String> getAccountList()
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

    private HashMap getAccountDetails(String accountID)
    {
        HashMap accountDetails = null;
        try {
            accountDetails = (HashMap) service.getAccountDetails(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return accountDetails;
    }

    private void setAccountDetails(String accountID, HashMap<String, String> accountDetails)
    {
        try {
            service.setAccountDetails(accountID, accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    public PreferenceScreen getAccountListPreferenceScreen()
    {
        Activity currentContext = getActivity();

        ArrayList<String> accountList = getAccountList();
        // Log.i(TAG, "GetAccountList: " + mAccountList);

        mRoot = getPreferenceManager().createPreferenceScreen(currentContext);

        // Default account category
        PreferenceCategory defaultAccountCat = new PreferenceCategory(currentContext);
        defaultAccountCat.setTitle(R.string.default_account_category);
        mRoot.addPreference(defaultAccountCat);

        mRoot.addPreference(getAccountPreferenceScreen(DEFAULT_ACCOUNT_ID));

        // Account list category
        PreferenceCategory accountListCat = new PreferenceCategory(currentContext);
        accountListCat.setTitle(R.string.default_account_category);
        mRoot.addPreference(accountListCat);

        Preference createNewAccount = new Preference(currentContext);
        createNewAccount.setTitle("Touch to Create New Account");
        createNewAccount.setOnPreferenceClickListener(launchAccountCreationOnClick);
        createNewAccount.setIntent(new Intent().setClass(getActivity(), AccountCreationActivity.class));
        mRoot.addPreference(createNewAccount);

        for(String s : accountList)
            mRoot.addPreference(getAccountPreferenceScreen(s));
         
        return mRoot;
    }

    public PreferenceCategory createPreferenceSection(PreferenceScreen root, Context context, int titleId, ArrayList<AccountDetail.PreferenceEntry> detailEntries, 
                                                                                                                      String accountID, HashMap<String, String> map)
    {
        // Inline preference
        PreferenceCategory accountPrefCat = new PreferenceCategory(context);
        accountPrefCat.setTitle(titleId);
        root.addPreference(accountPrefCat);

        for(AccountDetail.PreferenceEntry entry : detailEntries)
        {
            EditTextPreference accountPref = new EditTextPreference(context);
            accountPref.setDialogTitle(entry.mLabelId);
            accountPref.setPersistent(false);
            accountPref.setTitle(entry.mLabelId);
            accountPref.setSummary(getString(R.string.account_current_value_label) + map.get(entry.mKey));
            accountPref.setOnPreferenceChangeListener(changeBasicTextEditListener);
            accountPref.setKey(accountID);
            accountPrefCat.addPreference(accountPref);
        }

        return accountPrefCat;
    }

    public PreferenceScreen getAccountPreferenceScreen(String accountID)
    {
        Activity currentContext = getActivity();

        HashMap<String,String> map = getAccountDetails(accountID);
        mAccountList.put(accountID, map);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(currentContext);
        root.setTitle(map.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));

        createPreferenceSection(root, currentContext, R.string.account_preferences_basic, basicDetailKeys, accountID, map);
        createPreferenceSection(root, currentContext, R.string.account_preferences_advanced, advancedDetailKeys, accountID, map);
        createPreferenceSection(root, currentContext, R.string.account_preferences_srtp, srtpDetailKeys, accountID, map);
        createPreferenceSection(root, currentContext, R.string.account_preferences_tls, tlsDetailKeys, accountID, map);

        return root;
    }
}
