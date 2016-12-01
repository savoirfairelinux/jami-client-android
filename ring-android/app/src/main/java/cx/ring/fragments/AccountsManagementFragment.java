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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountEditionActivity;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.model.DaemonEvent;
import cx.ring.services.AccountService;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.views.dragsortlv.DragSortListView;

public class AccountsManagementFragment extends Fragment implements HomeActivity.Refreshable, Observer<DaemonEvent> {
    static final String TAG = AccountsManagementFragment.class.getSimpleName();

    public static final int ACCOUNT_CREATE_REQUEST = 1;
    public static final int ACCOUNT_EDIT_REQUEST = 2;
    private AccountsAdapter mAccountsAdapter;

    @Inject
    AccountService mAccountService;

    @BindView(R.id.accounts_list)
    DragSortListView mDnDListView;

    @BindView(R.id.empty_account_list)
    View mEmptyView;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Account item = mAccountsAdapter.getItem(from);
                mAccountsAdapter.remove(item);
                mAccountsAdapter.insert(item, to);
                mAccountService.setAccountOrder(mAccountsAdapter.getAccountOrder());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Create Account Management Fragment");

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        mAccountService.addObserver(this);

        mAccountsAdapter = new AccountsAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_accounts_list, parent, false);
        ButterKnife.bind(this, inflatedView);
        mDnDListView.setAdapter(mAccountsAdapter);
        return inflatedView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDnDListView.setDropListener(onDrop);
    }

    @OnItemClick(R.id.accounts_list)
    @SuppressWarnings("unused")
    void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
        Account selectedAccount = mAccountsAdapter.getItem(pos);
        if (selectedAccount.needsMigration()) {
            launchAccountMigrationActivity(mAccountsAdapter.getItem(pos));
        } else {
            launchAccountEditActivity(mAccountsAdapter.getItem(pos));
        }
    }

    public void onResume() {
        super.onResume();
        refresh();
        ((HomeActivity) getActivity()).setToolbarState(true, R.string.menu_item_accounts);
        FloatingActionButton btn = ((HomeActivity) getActivity()).getActionButton();
        btn.setImageResource(R.drawable.ic_add_white);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountWizard.class);
                startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
            }
        });
    }

    private void launchAccountEditActivity(Account acc) {
        Log.d(TAG, "Launch account edit activity");

        Intent intent = new Intent(getActivity(), AccountEditionActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, acc.getAccountID()));
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    private void launchAccountMigrationActivity(Account acc) {
        Log.d(TAG, "Launch account migration activity");

        Intent intent = new Intent()
                .setClass(getActivity(), AccountWizard.class)
                .setData(Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, acc.getAccountID()));
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refresh();
    }

    @Override
    public void update(Observable observable, DaemonEvent argument) {
        if (argument!= null && argument.getEventType()== DaemonEvent.EventType.ACCOUNTS_CHANGED) {
            refresh();
        }
    }

    /**
     * Adapter for accounts List
     *
     * @author lisional
     */
    public class AccountsAdapter extends BaseAdapter {

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
                entryView.loadingIndicator = rowView.findViewById(R.id.loading_indicator);
                entryView.errorIndicator = (ImageView) rowView.findViewById(R.id.error_indicator);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.account_checked);
                entryView.errorIndicator.setColorFilter(mContext.getResources().getColor(R.color.error_red));
                entryView.errorIndicator.setVisibility(View.GONE);
                entryView.loadingIndicator.setVisibility(View.GONE);
                rowView.setTag(entryView);
            } else {
                entryView = (AccountView) rowView.getTag();
            }

            final Account item = accounts.get(pos);
            entryView.alias.setText(item.getAlias());
            entryView.host.setTextColor(getResources().getColor(R.color.text_color_secondary));

            if (item.isIP2IP()) {
                entryView.host.setText(item.getRegistrationState());
            } else if (item.isSip()) {
                entryView.host.setText(item.getHost() + " - " + item.getRegistrationState());
            } else {
                entryView.host.setText(item.getDetail(ConfigKey.ACCOUNT_USERNAME));
            }

            entryView.enabled.setChecked(item.isEnabled());
            entryView.enabled.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    item.setEnabled(!item.isEnabled());
                    mAccountService.setAccountDetails(item.getAccountID(), item.getDetails());
                }
            });
            entryView.handle.setVisibility(View.VISIBLE);

            if (item.isEnabled()) {
                if (item.isTrying()) {
                    entryView.errorIndicator.setVisibility(View.GONE);
                    entryView.loadingIndicator.setVisibility(View.VISIBLE);
                } else if (item.needsMigration()) {
                    entryView.host.setText(R.string.account_update_needed);
                    entryView.host.setTextColor(Color.RED);
                    entryView.errorIndicator.setImageResource(R.drawable.ic_warning);
                    entryView.errorIndicator.setColorFilter(Color.RED);
                    entryView.errorIndicator.setVisibility(View.VISIBLE);
                } else if (item.isInError()) {
                    entryView.errorIndicator.setImageResource(R.drawable.ic_error_white);
                    entryView.errorIndicator.setColorFilter(Color.RED);
                    entryView.errorIndicator.setVisibility(View.VISIBLE);
                    entryView.loadingIndicator.setVisibility(View.GONE);
                } else if (!item.isRegistered()) {
                    entryView.errorIndicator.setImageResource(R.drawable.ic_network_disconnect_black_24dp);
                    entryView.errorIndicator.setColorFilter(Color.BLACK);
                    entryView.errorIndicator.setVisibility(View.VISIBLE);
                    entryView.loadingIndicator.setVisibility(View.GONE);
                } else {
                    entryView.errorIndicator.setVisibility(View.GONE);
                    entryView.loadingIndicator.setVisibility(View.GONE);
                }
            } else {
                entryView.errorIndicator.setVisibility(View.GONE);
                entryView.loadingIndicator.setVisibility(View.GONE);
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
            public View loadingIndicator;
            public ImageView errorIndicator;
            public CheckBox enabled;
        }

        public void replaceAll(List<Account> results) {
            Log.d(TAG, "AccountsAdapter replaceAll " + results.size());
            accounts.clear();
            accounts.addAll(results);
            notifyDataSetChanged();
        }

        private List<String> getAccountOrder() {
            ArrayList<String> order = new ArrayList<>(accounts.size());
            for (Account acc : accounts) {
                order.add(acc.getAccountID());
            }
            return order;
        }
    }

    @Override
    public void refresh() {
        mAccountsAdapter.replaceAll(mAccountService.getAccounts());
        if (mAccountsAdapter.isEmpty()) {
            mDnDListView.setEmptyView(mEmptyView);
        }
        mAccountsAdapter.notifyDataSetChanged();
    }
}
