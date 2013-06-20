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

package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountDetail;
import com.savoirfairelinux.sflphone.account.AccountDetailAdvanced;
import com.savoirfairelinux.sflphone.account.AccountDetailBasic;
import com.savoirfairelinux.sflphone.account.AccountDetailSrtp;
import com.savoirfairelinux.sflphone.account.AccountDetailTls;
import com.savoirfairelinux.sflphone.client.AccountPreferenceActivity;
import com.savoirfairelinux.sflphone.client.SFLPhonePreferenceActivity;
import com.savoirfairelinux.sflphone.client.SFLphoneApplication;
import com.savoirfairelinux.sflphone.service.ConfigurationManagerCallback;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.ServiceConstants;

public class AccountManagementFragment extends PreferenceFragment {
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    static final int ACCOUNT_EDIT_REQUEST = 2;
    private SFLPhonePreferenceActivity sflphonePreferenceActivity;
    private ISipService service = null;

    ArrayList<AccountDetail.PreferenceEntry> basicDetailKeys = null;
    ArrayList<AccountDetail.PreferenceEntry> advancedDetailKeys = null;
    ArrayList<AccountDetail.PreferenceEntry> srtpDetailKeys = null;
    ArrayList<AccountDetail.PreferenceEntry> tlsDetailKeys = null;
    HashMap<String, Preference> accountPreferenceHashMap = null;
    PreferenceScreen mRoot = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        sflphonePreferenceActivity = (SFLPhonePreferenceActivity) activity;
        service = sflphonePreferenceActivity.getSipService();
        Log.w(TAG, "onAttach() service=" + service);
    }

    public AccountManagementFragment() {
        basicDetailKeys = AccountDetailBasic.getPreferenceEntries();
        advancedDetailKeys = AccountDetailAdvanced.getPreferenceEntries();
        srtpDetailKeys = AccountDetailSrtp.getPreferenceEntries();
        tlsDetailKeys = AccountDetailTls.getPreferenceEntries();

        accountPreferenceHashMap = new HashMap<String, Preference>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");

        /*
         * FIXME if service cannot be obtained from SFLPhonePreferenceActivity, then get it from Application
         */
        service = sflphonePreferenceActivity.getSipService();
        if (service == null) {
            service = ((SFLphoneApplication) sflphonePreferenceActivity.getApplication()).getSipService();
            if (service == null) {
                Log.e(TAG, "onCreate() service=" + service);
            }
        }
        Log.w(TAG, "onCreate() service=" + service);

        setPreferenceScreen(getAccountListPreferenceScreen());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(ConfigurationManagerCallback.ACCOUNTS_CHANGED));
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACCOUNT_CREATE_REQUEST:
            if (resultCode == AccountPreferenceActivity.result.ACCOUNT_CREATED) {
                Bundle bundle = data.getExtras();
                Log.i(TAG, "Create account settings");
                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);
                createNewAccount(accountDetails);
            }
            break;
        case ACCOUNT_EDIT_REQUEST:
            if (resultCode == AccountPreferenceActivity.result.ACCOUNT_MODIFIED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");
                Log.i(TAG, "Update account settings for " + accountID);

                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);
                
                Log.w(TAG,"realm "+accountDetails.get(ServiceConstants.CONFIG_ACCOUNT_REALM));
                
                Preference accountScreen = accountPreferenceHashMap.get(accountID);
                mRoot.removePreference(accountScreen);
                accountPreferenceHashMap.remove(accountID);
                setAccountDetails(accountID, accountDetails);

            } else if (resultCode == AccountPreferenceActivity.result.ACCOUNT_DELETED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");

                Log.i(TAG, "Remove account " + accountID);
                deleteSelectedAccount(accountID);
                Preference accountScreen = accountPreferenceHashMap.get(accountID);
                mRoot.removePreference(accountScreen);
                accountPreferenceHashMap.remove(accountID);
            } else {
                Log.i(TAG, "Edition canceled");
            }
            break;
        default:
            break;
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        try {
            Log.i(TAG, "ADD ACCOUNT");
            service.addAccount(accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    private void setAccountDetails(String accountID, HashMap<String, String> accountDetails) {
        try {
            service.setAccountDetails(accountID, accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    private void deleteSelectedAccount(String accountID) {
        Log.i(TAG, "DeleteSelectedAccount");
        try {
            service.removeAccount(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> newList = (ArrayList<String>) getAccountList();
            Set<String> currentList = (Set<String>) accountPreferenceHashMap.keySet();
            if (newList.size() > currentList.size()) {
                for (String s : newList) {
                    if (!currentList.contains(s)) {
                        Preference accountScreen = createAccountPreferenceScreen(s);
                        mRoot.addPreference(accountScreen);
                        accountPreferenceHashMap.put(s, accountScreen);
                    }
                }
            }
        }
    };

    Preference.OnPreferenceClickListener launchAccountCreationOnClick = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getTitle() == "Create New Account") {
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
            if (preference.getTitle() == "Delete Account") {
                deleteSelectedAccount(preference.getKey());
            }
            return true;
        }
    };

    private void launchAccountCreationActivity(Preference preference) {
        Log.i(TAG, "Launch account creation activity");
        Intent intent = preference.getIntent();
        intent.putExtra(AccountPreferenceActivity.KEY_MODE, AccountPreferenceActivity.mode.CREATION_MODE);
        startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
    }

    private void launchAccountEditActivity(Preference preference) {
        Log.i(TAG, "Launch account edit activity");
        Intent intent = preference.getIntent();
        intent.putExtra(AccountPreferenceActivity.KEY_MODE, AccountPreferenceActivity.mode.EDITION_MODE);
        Bundle bundle = intent.getExtras();
        String accountID = bundle.getString("AccountID");

        HashMap<String, String> preferenceMap = getAccountDetails(accountID);

        AccountDetailBasic basicDetails = new AccountDetailBasic(preferenceMap);
        AccountDetailAdvanced advancedDetails = new AccountDetailAdvanced(preferenceMap);
        AccountDetailSrtp srtpDetails = new AccountDetailSrtp(preferenceMap);
        AccountDetailTls tlsDetails = new AccountDetailTls(preferenceMap);

        bundle.putStringArrayList(AccountDetailBasic.BUNDLE_TAG, basicDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailAdvanced.BUNDLE_TAG, advancedDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailSrtp.BUNDLE_TAG, srtpDetails.getValuesOnly());
        bundle.putStringArrayList(AccountDetailTls.BUNDLE_TAG, tlsDetails.getValuesOnly());

        intent.putExtras(bundle);

        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    private ArrayList<String> getAccountList() {
        ArrayList<String> accountList = null;
        try {
            accountList = (ArrayList<String>) service.getAccountList();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        // Remove the default account from list
        accountList.remove(DEFAULT_ACCOUNT_ID);

        return accountList;
    }

    private HashMap<String, String> getAccountDetails(String accountID) {
        HashMap<String, String> accountDetails = null;
        try {
            accountDetails = (HashMap<String, String>) service.getAccountDetails(accountID);
//            ArrayList<Integer> tmp = (ArrayList<Integer>) service.getAudioCodecList(accountID);
//            for(Integer i : tmp){
//                Log.w(TAG,"Codec : "+i);
//            }

//            if (accountDetails.containsKey("TLS.negotiationTimeoutSec"))
//                Log.i(TAG, "localinterface existe");
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }

        return accountDetails;
    }

    public PreferenceScreen getAccountListPreferenceScreen() {
        Activity currentContext = getActivity();

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
        createNewAccount.setIntent(new Intent().setClass(getActivity(), AccountPreferenceActivity.class));
        mRoot.addPreference(createNewAccount);

        ArrayList<String> accountList = getAccountList();
        for (String s : accountList) {
            Preference accountScreen = createAccountPreferenceScreen(s);
            mRoot.addPreference(accountScreen);
            accountPreferenceHashMap.put(s, accountScreen);
        }

        return mRoot;
    }

    Preference createAccountPreferenceScreen(String accountID) {

        HashMap<String, String> details = getAccountDetails(accountID);
        // Set<String> keys = details.keySet();
        // Iterator<String> ite = keys.iterator();
        // while(ite.hasNext()){
        // Log.i(TAG,"key : "+ ite.next());
        // }
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", accountID);

        Intent intent = new Intent().setClass(getActivity(), AccountPreferenceActivity.class);
        intent.putExtras(bundle);

        Preference editAccount = new Preference(getActivity());
        editAccount.setTitle(details.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
        editAccount.setSummary(details.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
        editAccount.setOnPreferenceClickListener(launchAccountEditOnClick);
        editAccount.setIntent(intent);

        return editAccount;
    }
}
