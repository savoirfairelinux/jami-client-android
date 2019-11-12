/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import java.util.List;

import butterknife.BindView;
import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.account.AccountWizardActivity;
import cx.ring.account.AccountsAdapter;
import cx.ring.account.AccountsManagementPresenter;
import cx.ring.account.AccountsManagementView;
import cx.ring.adapters.AccountView;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.utils.ContentUriHandler;

public class AccountsManagementFragment extends BaseSupportFragment<AccountsManagementPresenter> implements AccountsManagementView,
        AccountView.OnAccountActionListener {
    public static final int ACCOUNT_CREATE_REQUEST = 1;
    public static final int ACCOUNT_EDIT_REQUEST = 2;
    static final String TAG = AccountsManagementFragment.class.getSimpleName();

    @BindView(R.id.accounts_list)
    protected RecyclerView mDnDListView;

    @BindView(R.id.empty_account_list)
    protected View mEmptyView;

    private AccountsAdapter mAccountsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Create Account Management Fragment");

        // dependency injection
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mAccountsAdapter = new AccountsAdapter(this);
    }

    @Override
    public int getLayout() {
        return R.layout.frag_accounts_list;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mDnDListView.setFocusable(false);
        mDnDListView.setHasFixedSize(true);
        mDnDListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDnDListView.setAdapter(mAccountsAdapter);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.w(TAG, "onResume()");
        FloatingActionButton button = ((HomeActivity) getActivity()).getActionButton();
        button.setOnClickListener(v -> presenter.addClicked());
//        ((HomeActivity) getActivity()).setToolbarState(true, R.string.menu_item_accounts);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.w(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.w(TAG, "onStop()");
        FloatingActionButton button = ((HomeActivity) getActivity()).getActionButton();
        button.setOnClickListener(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
    public void launchAccountEditActivity(Account account) {
        Log.d(TAG, "launchAccountEditActivity: Launch account edit activity");

        Intent intent = new Intent(getActivity(), AccountEditionActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, account.getAccountID()));
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void launchAccountMigrationActivity(Account account) {
        Log.d(TAG, "launchAccountMigrationActivity: Launch account migration activity");

        Intent intent = new Intent()
                .setClass(getActivity(), AccountWizardActivity.class)
                .setData(Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, account.getAccountID()));
        startActivityForResult(intent, ACCOUNT_EDIT_REQUEST);
    }

    @Override
    public void launchWizardActivity() {
        Intent intent = new Intent(getActivity(), AccountWizardActivity.class);
        startActivityForResult(intent, ACCOUNT_CREATE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void refresh(final List<Account> accounts) {
        mAccountsAdapter.replaceAll(accounts);
        if (accounts.isEmpty()) {
            mDnDListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mDnDListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void refreshAccount(final Account account) {
        mAccountsAdapter.replaceAccount(account);
    }

    @Override
    public void onAccountSelected(Account account) {
        presenter.clickAccount(account);
    }

    @Override
    public void onAccountEnabled(Account account) {
        presenter.accountEnabled(account);
    }
}
