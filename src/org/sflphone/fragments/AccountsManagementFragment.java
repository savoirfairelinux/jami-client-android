/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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

import java.io.File;
import java.util.ArrayList;

import org.sflphone.R;
import org.sflphone.client.AccountEditionActivity;
import org.sflphone.client.AccountWizard;
import org.sflphone.interfaces.AccountsInterface;
import org.sflphone.loaders.AccountsLoader;
import org.sflphone.loaders.LoaderConstants;
import org.sflphone.model.Account;
import org.sflphone.receivers.AccountsReceiver;
import org.sflphone.service.ConfigurationManagerCallback;
import org.sflphone.service.ISipService;
import org.sflphone.views.dragsortlv.DragSortListView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class AccountsManagementFragment extends ListFragment implements LoaderCallbacks<Bundle>, AccountsInterface {
    static final String TAG = "AccountManagementFragment";
    static final String DEFAULT_ACCOUNT_ID = "IP2IP";
    static final int ACCOUNT_CREATE_REQUEST = 1;
    static final int ACCOUNT_EDIT_REQUEST = 2;
    AccountsReceiver accountReceiver;
    AccountsAdapter mAccountsAdapter;
    AccountsAdapter mIP2IPAdapter;

    DragSortListView mDnDListView;
    private View mLoadingView;
    private int mShortAnimationDuration;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Account item = mAccountsAdapter.getItem(from);
                mAccountsAdapter.remove(item);
                mAccountsAdapter.insert(item, to);
                try {
                    mCallbacks.getService().setAccountOrder(mAccountsAdapter.generateAccountOrder());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

    };

    private Callbacks mCallbacks = sDummyCallbacks;
    private Account ip2ip;
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
        mAccountsAdapter = new AccountsAdapter(getActivity(), new ArrayList<Account>());
        mIP2IPAdapter = new AccountsAdapter(getActivity(), new ArrayList<Account>());
        this.setHasOptionsMenu(true);
        accountReceiver = new AccountsReceiver(this);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        Log.i(TAG, "anim time: " + mShortAnimationDuration);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_accounts_list, parent, false);
        setListAdapter(mAccountsAdapter);

        return inflatedView;
    }

    @Override
    public DragSortListView getListView() {
        return (DragSortListView) super.getListView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDnDListView = getListView();

        mDnDListView.setDropListener(onDrop);
        mDnDListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                launchAccountEditActivity(mAccountsAdapter.getItem(pos));
            }
        });

        ((ListView) getView().findViewById(R.id.ip2ip)).setAdapter(mIP2IPAdapter);
        ((ListView) getView().findViewById(R.id.ip2ip)).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                launchAccountEditActivity(ip2ip);

            }
        });

        mLoadingView = view.findViewById(R.id.loading_spinner);
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
        getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
        getActivity().getActionBar().setTitle(R.string.menu_item_accounts);
    }

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
        bundle.putParcelable("account", acc);

        intent.putExtras(bundle);

        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void accountsChanged() {
        if (getActivity() != null)
            getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, this);
    }

    @Override
    public void accountStateChanged(Intent accountState) {
        mAccountsAdapter.updateAccount(accountState);
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

        public void insert(Account item, int to) {
            accounts.add(to, item);
            notifyDataSetChanged();
        }

        public void remove(Account item) {
            accounts.remove(item);
            notifyDataSetChanged();
        }

        @Override
        public boolean hasStableIds() {
            return true;
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

            final Account item = accounts.get(pos);
            entryView.alias.setText(accounts.get(pos).getAlias());
            if (item.isIP2IP()) {
                entryView.host.setText(item.getRegistered_state());
                entryView.enabled.setVisibility(View.GONE);
            } else {
                entryView.host.setText(item.getHost() + " - " + item.getRegistered_state());
                entryView.enabled.setChecked(item.isEnabled());
                entryView.enabled.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        item.setEnabled(!item.isEnabled());

                        try {
                            mCallbacks.getService().setAccountDetails(item.getAccountID(), item.getDetails());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

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
            String id = accountState.getStringExtra("Account");
            String newState = accountState.getStringExtra("state");

            Log.i(TAG, "updateAccount:" + newState);
            for (Account a : accounts) {
                if (a.getAccountID().contentEquals(id)) {
                    a.setRegistered_state(newState);
                    notifyDataSetChanged();
                    return;
                }
            }

        }

        private String generateAccountOrder() {
            String result = DEFAULT_ACCOUNT_ID + File.separator;
            for (Account a : accounts) {
                result += a.getAccountID() + File.separator;
            }
            return result;
        }

    }

    private void crossfade() {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        mDnDListView.setAlpha(0f);
        mDnDListView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        mDnDListView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        mLoadingView.animate().alpha(0f).setDuration(mShortAnimationDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoadingView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Bundle> arg0, Bundle results) {
        mAccountsAdapter.removeAll();
        ArrayList<Account> tmp = results.getParcelableArrayList(AccountsLoader.ACCOUNTS);
        ip2ip = results.getParcelable(AccountsLoader.ACCOUNT_IP2IP);
        mAccountsAdapter.addAll(tmp);
        mIP2IPAdapter.removeAll();
        mIP2IPAdapter.insert(ip2ip, 0);
        if (mAccountsAdapter.isEmpty()) {
            mDnDListView.setEmptyView(getView().findViewById(R.id.empty_account_list));
        }
        crossfade();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Bundle> arg0) {

    }

    @Override
    public android.support.v4.content.Loader<Bundle> onCreateLoader(int arg0, Bundle arg1) {
        AccountsLoader l = new AccountsLoader(getActivity(), mCallbacks.getService());
        l.forceLoad();
        return l;
    }
}
