/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.contactrequests;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

import cx.ring.account.AccountEditionFragment;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragBlacklistBinding;
import cx.ring.model.CallContact;
import cx.ring.mvp.BaseSupportFragment;

public class BlackListFragment extends BaseSupportFragment<BlackListPresenter> implements BlackListView,
        BlackListViewHolder.BlackListListeners {

    public static final String TAG = BlackListFragment.class.getSimpleName();

    private BlackListAdapter mAdapter;
    private FragBlacklistBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragBlacklistBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() == null || getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY) == null) {
            return;
        }
        presenter.setAccountId(getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
    public void onUnblockClicked(CallContact viewModel) {
        presenter.unblockClicked(viewModel);
    }

    @Override
    public void updateView(final Collection<CallContact> list) {
        binding.blacklist.setVisibility(View.VISIBLE);
        if (binding.blacklist.getAdapter() != null) {
            mAdapter.replaceAll(list);
        } else {
            mAdapter = new BlackListAdapter(list, BlackListFragment.this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            binding.blacklist.setLayoutManager(layoutManager);
            binding.blacklist.setAdapter(mAdapter);
        }
    }

    @Override
    public void hideListView() {
        binding.blacklist.setVisibility(View.GONE);
    }

    @Override
    public void displayEmptyListMessage(final boolean display) {
        binding.emptyTextView.setVisibility(display ? View.VISIBLE : View.GONE);
    }
}