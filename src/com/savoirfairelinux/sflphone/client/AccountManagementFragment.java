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
import android.preference.PreferenceManager;
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
import com.savoirfairelinux.sflphone.client.AccountPreferenceActivity;

public class AccountManagementFragment extends PreferenceFragment
{
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    static final int ACCOUNT_EDIT_REQUEST = 2;
    private ISipService service;

    // HashMap<String,HashMap<String,String>> mAccountList = new HashMap<String,HashMap<String,String>>();
    HashMap<String, AccountPreferenceScreen> mAccountList = new HashMap<String, AccountPreferenceScreen>();
    ArrayList<AccountDetail.PreferenceEntry> basicDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> advancedDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> srtpDetailKeys;
    ArrayList<AccountDetail.PreferenceEntry> tlsDetailKeys;
    PreferenceScreen mRoot = null;

    Activity context = getActivity();

    public AccountManagementFragment(ISipService s)
    {
        service = s;

        basicDetailKeys =  AccountDetailBasic.getPreferenceEntries();
        advancedDetailKeys = AccountDetailAdvanced.getPreferenceEntries();
        srtpDetailKeys = AccountDetailSrtp.getPreferenceEntries();
        tlsDetailKeys = AccountDetailTls.getPreferenceEntries();
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
                AccountPreferenceScreen accountPref = mAccountList.get(s);
                if(accountPref != null) {
                    service.setAccountDetails(s, accountPref.preferenceMap);
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
        switch(requestCode) {
            case ACCOUNT_CREATE_REQUEST:
                Log.i(TAG, "ACCOUNT_CREATE_REQUEST Done");
                break;
            case ACCOUNT_EDIT_REQUEST:
                if(resultCode == AccountPreferenceActivity.ACCOUNT_MODIFIED) {
                    Bundle bundle = data.getExtras();
                    String accountID = bundle.getString("AccountID");
                    Log.i(TAG, "Update account settings for " + accountID);
                }
                break;
            default:
                break;
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            ArrayList<String> newList = (ArrayList<String>) getAccountList();
            Set<String> currentList = (Set<String>) mAccountList.keySet();
            currentList.remove(DEFAULT_ACCOUNT_ID);
            if(newList.size() > currentList.size()) {
                for(String s : newList) {
                    if(!currentList.contains(s)) {
                        Log.i("receiver", "ADDING ACCOUNT!!!!!! " + s);
                        mRoot.addPreference(createAccountPreferenceScreen(s));
                    }
                }
            }
            else if(newList.size() < currentList.size()) {
                for(String s : currentList) {
                    if(!newList.contains(s)) {
                        Log.i("receiver", "REMOVING ACCOUNT!!!!!! " + s);
                        mRoot.removePreference(mAccountList.get(s).mScreen);
                        mAccountList.remove(s);
                    }
                }
            } 
        }
    };


    Preference.OnPreferenceChangeListener changeBasicTextEditListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label) + (CharSequence)newValue);
            AccountPreferenceScreen accountPreference = mAccountList.get(preference.getKey());
            String preferenceKey = basicDetailKeys.get(preference.getOrder()).mKey; 
            accountPreference.preferenceMap.put(preferenceKey, ((CharSequence)newValue).toString());
            if(preferenceKey == AccountDetailBasic.CONFIG_ACCOUNT_ALIAS)
                accountPreference.mScreen.setTitle(((CharSequence)newValue.toString()));
            return true;
        }
    };

    Preference.OnPreferenceChangeListener changeAdvancedTextEditListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary(getString(R.string.account_current_value_label) + (CharSequence)newValue);
            mAccountList.get(preference.getKey()).preferenceMap.put(advancedDetailKeys.get(preference.getOrder()).mKey, ((CharSequence)newValue).toString());
            return true;
        }
    };

    Preference.OnPreferenceClickListener launchAccountCreationOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getTitle() == "Create New Account") {
                launchAccountCreationActivity(preference);
            }
            return true;
        }
    };

    Preference.OnPreferenceClickListener launchAccountEditOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            launchAccountEditActivity(preference);
            return true;
        }
    };

    Preference.OnPreferenceClickListener removeSelectedAccountOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getTitle() == "Delete Account") {
                deleteSelectedAccount(preference.getKey());
            }
            return true;
        }
    };

    private void launchAccountCreationActivity(Preference preference)
    {
        Log.i(TAG, "Launch account creation activity");
        Intent intent = preference.getIntent();
        startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
    }

    private void launchAccountEditActivity(Preference preference)
    {
        Log.i(TAG, "Launch account edit activity");
        Intent intent = preference.getIntent();
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    private void deleteSelectedAccount(String accountID) {
        Log.i(TAG, "DeleteSelectedAccount");
        try {
            service.removeAccount(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    };

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

        mRoot.addPreference(createAccountPreferenceScreen(DEFAULT_ACCOUNT_ID));

        // Account list category
        PreferenceCategory accountListCat = new PreferenceCategory(currentContext);
        accountListCat.setTitle(R.string.default_account_category);
        mRoot.addPreference(accountListCat);

        Preference createNewAccount = new Preference(currentContext);
        createNewAccount.setTitle("Create New Account");
        createNewAccount.setOnPreferenceClickListener(launchAccountCreationOnClick);
        createNewAccount.setIntent(new Intent().setClass(getActivity(), AccountCreationActivity.class));
        mRoot.addPreference(createNewAccount);

        for(String s : accountList) {
            // mRoot.addPreference(getAccountPreferenceScreen(s));
            mRoot.addPreference(createAccountPreferenceScreen(s));
        }
         
        return mRoot;
    }

    Preference createAccountPreferenceScreen(String accountID) {

        HashMap<String, String> preferenceMap = getAccountDetails(accountID);

        AccountDetailBasic basicDetails = new AccountDetailBasic(preferenceMap);
        AccountDetailAdvanced advancedDetails = new AccountDetailAdvanced(preferenceMap);
        AccountDetailSrtp srtpDetails = new AccountDetailSrtp(preferenceMap);
        AccountDetailTls tlsDetails = new AccountDetailTls(preferenceMap);

        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);
        bundle.putStringArrayList(AccountDetailBasic.BUNDLE_TAG, basicDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailAdvanced.BUNDLE_TAG, advancedDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailSrtp.BUNDLE_TAG, srtpDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailTls.BUNDLE_TAG, tlsDetails.getValuesOnly());

        Intent intent = new Intent().setClass(getActivity(), AccountPreferenceActivity.class); 
        intent.putExtras(bundle);

        Preference editAccount = new Preference(getActivity());
        editAccount.setTitle(accountID);
        editAccount.setOnPreferenceClickListener(launchAccountEditOnClick);
        editAccount.setIntent(intent);
        
        return editAccount;
    }

    /*
    AccountPreferenceScreen createAccountPreferenceScreen(String accountID) {
        AccountPreferenceScreen preference = new AccountPreferenceScreen(getPreferenceManager(), getActivity(), accountID);
        mAccountList.put(accountID, preference);

        return preference;
    }
    */

    private class AccountPreferenceScreen
    {
        public PreferenceScreen mScreen;
        public HashMap<String, String> preferenceMap;

        public AccountPreferenceScreen(PreferenceManager prefManager, Context context, String accountID)
        {
            mScreen = prefManager.createPreferenceScreen(context);
            preferenceMap = getAccountDetails(accountID);

            mScreen.setTitle(preferenceMap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));

            if(accountID != DEFAULT_ACCOUNT_ID) {
                Preference deleteThisAccount = new Preference(context);
                deleteThisAccount.setTitle("Delete Account");
                deleteThisAccount.setKey(accountID);
                deleteThisAccount.setOnPreferenceClickListener(removeSelectedAccountOnClick);
                deleteThisAccount.setIntent(new Intent().setClass(getActivity(), AccountCreationActivity.class));
                mScreen.addPreference(deleteThisAccount);
            }

            createPreferenceSection(mScreen, context, R.string.account_preferences_basic, basicDetailKeys, accountID, preferenceMap);
            createPreferenceSection(mScreen, context, R.string.account_preferences_advanced, advancedDetailKeys, accountID, preferenceMap);
            createPreferenceSection(mScreen, context, R.string.account_preferences_srtp, srtpDetailKeys, accountID, preferenceMap);
            createPreferenceSection(mScreen, context, R.string.account_preferences_tls, tlsDetailKeys, accountID, preferenceMap);
        }

        public void createPreferenceSection(PreferenceScreen root, Context context, int titleId, ArrayList<AccountDetail.PreferenceEntry> detailEntries, 
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
        }
    }
}
