/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *      Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package org.sflphone.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import org.sflphone.R;
import org.sflphone.account.AccountDetail;
import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;
import org.sflphone.client.AccountEditionActivity;
import org.sflphone.client.AccountWizard;
import org.sflphone.interfaces.AccountsInterface;
import org.sflphone.loaders.AccountsLoader;
import org.sflphone.loaders.LoaderConstants;
import org.sflphone.model.Account;
import org.sflphone.receivers.AccountsReceiver;
import org.sflphone.service.ConfigurationManagerCallback;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class AccountManagementFragment extends ListFragment implements LoaderCallbacks<ArrayList<Account>>, AccountsInterface {
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    static final int ACCOUNT_EDIT_REQUEST = 2;
    AccountsReceiver accountReceiver;
    AccountsAdapter mAdapter;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Create Account Management Fragment");
        mAdapter = new AccountsAdapter(getActivity(), new ArrayList<Account>());
        this.setHasOptionsMenu(true);
        accountReceiver = new AccountsReceiver(this);
        getLoaderManager().initLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(android.R.layout.list_content, parent, false);
        setListAdapter(mAdapter);
        return inflatedView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                launchAccountEditActivity(mAdapter.getItem(pos));
            }
        });

        setEmptyText(getResources().getString(R.string.empty_account_list));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(accountReceiver);
    }

    public void onResume() {
        super.onResume();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter2.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        getActivity().registerReceiver(accountReceiver, intentFilter2);
    }

    @SuppressWarnings("unchecked")
    // No proper solution with HashMap runtime cast
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACCOUNT_CREATE_REQUEST:
            if (resultCode == AccountWizard.ACCOUNT_CREATED) {
                Bundle bundle = data.getExtras();
                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);

                createNewAccount(accountDetails);
            }
            break;
        case ACCOUNT_EDIT_REQUEST:
            if (resultCode == AccountEditionActivity.result.ACCOUNT_MODIFIED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");
                Log.i(TAG, "Update account settings for " + accountID);

                HashMap<String, String> accountDetails = new HashMap<String, String>();
                accountDetails = (HashMap<String, String>) bundle.getSerializable(AccountDetail.TAG);
                setAccountDetails(accountID, accountDetails);

            } else if (resultCode == AccountEditionActivity.result.ACCOUNT_DELETED) {
                Bundle bundle = data.getExtras();
                String accountID = bundle.getString("AccountID");

                Log.i(TAG, "Remove account " + accountID);
                deleteSelectedAccount(accountID);
                // Preference accountScreen = accountPreferenceHashMap.get(accountID);
                // mRoot.removePreference(accountScreen);
                // accountPreferenceHashMap.remove(accountID);
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
            mCallbacks.getService().addAccount(accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    private void setAccountDetails(String accountID, HashMap<String, String> accountDetails) {
        try {
            mCallbacks.getService().setAccountDetails(accountID, accountDetails);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    }

    @Override
    public Loader<ArrayList<Account>> onCreateLoader(int id, Bundle args) {
        AccountsLoader l = new AccountsLoader(getActivity(), mCallbacks.getService());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Account>> loader, ArrayList<Account> results) {
        mAdapter.removeAll();
        mAdapter.addAll(results);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Account>> arg0) {
        // TODO Auto-generated method stub

    }

    private void deleteSelectedAccount(String accountID) {
        Log.i(TAG, "DeleteSelectedAccount");
        try {
            mCallbacks.getService().removeAccount(accountID);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot call service method", e);
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inf) {
        super.onCreateOptionsMenu(m, inf);
        inf.inflate(R.menu.account_creation, m);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case R.id.menuitem_create:
            Intent intent = new Intent().setClass(getActivity(), AccountWizard.class);
            startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
            break;
        }

        return true;
    }

    private void launchAccountEditActivity(Account acc) {
        Log.i(TAG, "Launch account edit activity");

        Intent intent = new Intent().setClass(getActivity(), AccountEditionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("AccountID", acc.getAccountID());
        bundle.putStringArrayList(AccountDetailBasic.BUNDLE_TAG, acc.getBasicDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailAdvanced.BUNDLE_TAG, acc.getAdvancedDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailSrtp.BUNDLE_TAG, acc.getSrtpDetails().getValuesOnly());
        bundle.putStringArrayList(AccountDetailTls.BUNDLE_TAG, acc.getTlsDetails().getValuesOnly());

        intent.putExtras(bundle);

        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void accountsChanged() {
        if (getActivity() != null)
            getActivity().getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    @Override
    public void accountStateChanged(Intent accountState) {
        mAdapter.updateAccount(accountState);
    }

    /**
     * 
     * Adapter for accounts List
     * 
     * @author lisional
     * 
     */
    public class AccountsAdapter extends BaseAdapter {

        // private static final String TAG = AccountSelectionAdapter.class.getSimpleName();

        ArrayList<Account> accounts;
        Context mContext;

        public AccountsAdapter(Context cont, ArrayList<Account> newList) {
            super();
            accounts = newList;
            mContext = cont;
        }

        @Override
        public int getCount() {
            return accounts.size();
        }

        @Override
        public Account getItem(int pos) {
            return accounts.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return 0;
        }

        @Override
        public View getView(final int pos, View convertView, ViewGroup parent) {
            View rowView = convertView;
            AccountView entryView = null;

            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.item_account_pref, null);

                entryView = new AccountView();
                entryView.alias = (TextView) rowView.findViewById(R.id.account_alias);
                entryView.host = (TextView) rowView.findViewById(R.id.account_host);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.account_checked);
                rowView.setTag(entryView);
            } else {
                entryView = (AccountView) rowView.getTag();
            }

            entryView.alias.setText(accounts.get(pos).getAlias());
            entryView.host.setText(accounts.get(pos).getHost() + " - " + accounts.get(pos).getRegistered_state());
            entryView.enabled.setChecked(accounts.get(pos).isEnabled());

            entryView.enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    accounts.get(pos).setEnabled(isChecked);

                    try {
                        mCallbacks.getService().setAccountDetails(accounts.get(pos).getAccountID(), accounts.get(pos).getDetails());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

            return rowView;
        }

        /*********************
         * ViewHolder Pattern
         *********************/
        public class AccountView {
            public TextView alias;
            public TextView host;
            public CheckBox enabled;
        }

        public void removeAll() {
            accounts.clear();
            notifyDataSetChanged();

        }

        public void addAll(ArrayList<Account> results) {
            accounts.addAll(results);
            notifyDataSetChanged();
        }

        /**
         * Modify state of specific account
         * 
         * @param accountState
         */
        public void updateAccount(Intent accountState) {
            Log.i(TAG, "updateAccount");
            String id = accountState.getStringExtra("Account");
            String newState = accountState.getStringExtra("state");
            accountState.getStringExtra("Account");

            for (Account a : accounts) {
                if (a.getAccountID().contentEquals(id)) {
                    a.setRegistered_state(newState);
                    notifyDataSetChanged();
                    return;
                }
            }

        }

    }
}
