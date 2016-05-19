/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import cx.ring.R;
import cx.ring.client.AccountEditionActivity;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.LocalService;
import cx.ring.views.dragsortlv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

public class AccountsManagementFragment extends Fragment implements HomeActivity.Refreshable {
    static final String TAG = AccountsManagementFragment.class.getSimpleName();

    public static final int ACCOUNT_CREATE_REQUEST = 1;
    public static final int ACCOUNT_EDIT_REQUEST = 2;
    private AccountsAdapter mAccountsAdapter;

    private DragSortListView mDnDListView;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Account item = mAccountsAdapter.getItem(from);
                mAccountsAdapter.remove(item);
                mAccountsAdapter.insert(item, to);
                mCallbacks.getService().setAccountOrder(mAccountsAdapter.getAccountOrder());
            }
        }
    };

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Create Account Management Fragment");
        mAccountsAdapter = new AccountsAdapter(getActivity());

        getActivity().registerReceiver(mReceiver, new IntentFilter(LocalService.ACTION_ACCOUNT_UPDATE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_accounts_list, parent, false);
        ((ListView) inflatedView.findViewById(R.id.accounts_list)).setAdapter(mAccountsAdapter);

        return inflatedView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDnDListView = (DragSortListView) getView().findViewById(R.id.accounts_list);

        mDnDListView.setDropListener(onDrop);
        mDnDListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                launchAccountEditActivity(mAccountsAdapter.getItem(pos));
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        refresh();
        ((HomeActivity) getActivity()).setToolbarState(true, R.string.menu_item_accounts);
        FloatingActionButton btn = ((HomeActivity) getActivity()).getActionButton();
        btn.setImageResource(R.drawable.ic_add_white_24dp);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent().setClass(getActivity(), AccountWizard.class);
                startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
            }
        });
    }

    private void launchAccountEditActivity(Account acc) {
        Log.i(TAG, "Launch account edit activity");

        Intent intent = new Intent()
                .setClass(getActivity(), AccountEditionActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(Uri.withAppendedPath(AccountEditionActivity.CONTENT_URI, acc.getAccountID()));
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refresh();
    }

    /**
     * Adapter for accounts List
     *
     * @author lisional
     */
    public class AccountsAdapter extends BaseAdapter {

        // private static final String TAG = AccountSelectionAdapter.class.getSimpleName();

        private final ArrayList<Account> accounts = new ArrayList<>();
        private final Context mContext;

        public AccountsAdapter(Context c) {
            super();
            mContext = c;
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
            AccountView entryView;

            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.item_account_pref, parent, false);

                entryView = new AccountView();
                entryView.handle = (ImageView) rowView.findViewById(R.id.drag_handle);
                entryView.alias = (TextView) rowView.findViewById(R.id.account_alias);
                entryView.host = (TextView) rowView.findViewById(R.id.account_host);
                entryView.loading_indicator = rowView.findViewById(R.id.loading_indicator);
                entryView.error_indicator = (ImageView) rowView.findViewById(R.id.error_indicator);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.account_checked);
                entryView.error_indicator.setColorFilter(mContext.getResources().getColor(R.color.error_red));
                entryView.error_indicator.setVisibility(View.GONE);
                entryView.loading_indicator.setVisibility(View.GONE);
                rowView.setTag(entryView);
            } else {
                entryView = (AccountView) rowView.getTag();
            }

            final Account item = accounts.get(pos);
            entryView.alias.setText(item.getAlias());

            if (item.isIP2IP())
                entryView.host.setText(item.getRegistered_state());
            else if (item.isSip())
                entryView.host.setText(item.getHost() + " - " + item.getRegistered_state());
            else
                entryView.host.setText(item.getBasicDetails().getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME));

            entryView.enabled.setChecked(item.isEnabled());
            entryView.enabled.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    item.setEnabled(!item.isEnabled());
                    try {
                        mCallbacks.getService().getRemoteService().setAccountDetails(item.getAccountID(), item.getDetails());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
            entryView.handle.setVisibility(View.VISIBLE);

            if (item.isEnabled()) {
                if (item.isTrying()) {
                    entryView.error_indicator.setVisibility(View.GONE);
                    entryView.loading_indicator.setVisibility(View.VISIBLE);
                } else if (item.isInError()) {
                    entryView.error_indicator.setImageResource(R.drawable.ic_error_white_24dp);
                    entryView.error_indicator.setColorFilter(Color.RED);
                    entryView.error_indicator.setVisibility(View.VISIBLE);
                    entryView.loading_indicator.setVisibility(View.GONE);
                } else if (!item.isRegistered()) {
                    entryView.error_indicator.setImageResource(R.drawable.ic_network_disconnect_black_24dp);
                    entryView.error_indicator.setColorFilter(Color.BLACK);
                    entryView.error_indicator.setVisibility(View.VISIBLE);
                    entryView.loading_indicator.setVisibility(View.GONE);
                } else {
                    entryView.error_indicator.setVisibility(View.GONE);
                    entryView.loading_indicator.setVisibility(View.GONE);
                }
            } else {
                entryView.error_indicator.setVisibility(View.GONE);
                entryView.loading_indicator.setVisibility(View.GONE);
            }

            return rowView;
        }

        /**
         * ******************
         * ViewHolder Pattern
         * *******************
         */
        public class AccountView {
            public ImageView handle;
            public TextView alias;
            public TextView host;
            public View loading_indicator;
            public ImageView error_indicator;
            public CheckBox enabled;
        }

        public void replaceAll(List<Account> results) {
            Log.i(TAG, "AccountsAdapter replaceAll " + results.size());
            accounts.clear();
            accounts.addAll(results);
            notifyDataSetChanged();
        }

        private List<String> getAccountOrder() {
            ArrayList<String> order = new ArrayList<>(accounts.size());
            for (Account acc : accounts)
                order.add(acc.getAccountID());
            return order;
        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(LocalService.ACTION_ACCOUNT_UPDATE)) {
                refresh();
            }
        }
    };

    public void refresh() {
        LocalService service = mCallbacks.getService();
        View v = getView();
        if (service == null || v == null)
            return;
        mAccountsAdapter.replaceAll(service.getAccounts());
        if (mAccountsAdapter.isEmpty()) {
            mDnDListView.setEmptyView(v.findViewById(R.id.empty_account_list));
        }
        mAccountsAdapter.notifyDataSetChanged();
    }
}
