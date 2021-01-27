/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.content.Context;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

import cx.ring.account.AccountEditionFragment;
import cx.ring.account.JamiAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.FragBlocklistBinding;

import net.jami.contactrequests.BlockListPresenter;
import net.jami.contactrequests.BlockListView;
import net.jami.model.Contact;
import cx.ring.mvp.BaseSupportFragment;

public class BlockListFragment extends BaseSupportFragment<BlockListPresenter> implements BlockListView,
        BlockListViewHolder.BlockListListeners {

    public static final String TAG = BlockListFragment.class.getSimpleName();

    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mOnBackPressedCallback.setEnabled(false);
            JamiAccountSummaryFragment fragment = (JamiAccountSummaryFragment) getParentFragment();
            if (fragment != null) {
                fragment.popBackStack();
            }
        }
    };

    private BlockListAdapter mAdapter;
    private FragBlocklistBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragBlocklistBinding.inflate(inflater, container, false);
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
        String mAccountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
        mOnBackPressedCallback.setEnabled(true);
        presenter.setAccountId(mAccountId);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
    }

    @Override
    public void onUnblockClicked(Contact viewModel) {
        presenter.unblockClicked(viewModel);
    }

    @Override
    public void updateView(final Collection<Contact> list) {
        binding.blocklist.setVisibility(View.VISIBLE);
        if (binding.blocklist.getAdapter() != null) {
            mAdapter.replaceAll(list);
        } else {
            mAdapter = new BlockListAdapter(list, BlockListFragment.this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            binding.blocklist.setLayoutManager(layoutManager);
            binding.blocklist.setAdapter(mAdapter);
        }
    }

    @Override
    public void hideListView() {
        binding.blocklist.setVisibility(View.GONE);
    }

    @Override
    public void displayEmptyListMessage(final boolean display) {
        binding.placeholder.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    public void setAccount(String accountId) {
        presenter.setAccountId(accountId);
    }
}