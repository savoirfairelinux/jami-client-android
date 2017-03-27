/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cx.ring.R;
import cx.ring.application.RingApplication;

public class BlackListFragment extends Fragment implements BlackListView,
        BlackListViewHolder.BlackListListeners {

    public static final String TAG = BlackListFragment.class.getSimpleName();

    @Inject
    protected BlackListPresenter mBlackListPresenter;

    @BindView(R.id.blacklist)
    protected RecyclerView mBlacklist;

    private BlackListAdapter mAdapter;
    private Unbinder mUnbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View inflatedView = inflater.inflate(R.layout.frag_blacklist, container, false);

        mUnbinder = ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // view binding
        mBlackListPresenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Butterknife unbinding
        mUnbinder.unbind();

        // view unbinding
        mBlackListPresenter.unbindView();
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
    public void onUnblockClick(BlackListViewModel viewModel) {
        //mBlackListPresenter.unblockClicked(viewModel);
    }

    @Override
    public void updateView(final ArrayList<BlackListViewModel> list) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBlacklist.getAdapter() != null) {
                    mAdapter.replaceAll(list);
                } else {
                    mAdapter = new BlackListAdapter(list, BlackListFragment.this);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
                    mBlacklist.setLayoutManager(layoutManager);
                    mBlacklist.setAdapter(mAdapter);
                }
            }
        });
    }
}
