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

package cx.ring.trustrequests;

import android.app.Fragment;
import android.os.Bundle;
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
import cx.ring.client.HomeActivity;
import cx.ring.model.TrustRequest;
import cx.ring.mvp.GenericView;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.utils.Log;

public class PendingTrustRequestsFragment extends Fragment implements GenericView<PendingTrustRequestsViewModel> {

    static final String TAG = PendingTrustRequestsFragment.class.getSimpleName();

    @Inject
    protected PendingTrustRequestsPresenter mPendingTrustRequestsPresenter;

    @BindView(R.id.requests_list)
    protected RecyclerView mRequestsList;

    private Unbinder mUnbinder;
    private TrustRequestsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        setHasOptionsMenu(true);

        final View inflatedView = inflater.inflate(R.layout.frag_pending_trust_requests, parent, false);

        // views injection
        mUnbinder = ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mAdapter = new TrustRequestsAdapter(getActivity(), new ArrayList<TrustRequest>());
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRequestsList.setLayoutManager(mLayoutManager);
        mRequestsList.setAdapter(mAdapter);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_trust_request);

        // view binding
        mPendingTrustRequestsPresenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Butterknife unbinding
        mUnbinder.unbind();

        // view unbinding
        mPendingTrustRequestsPresenter.unbindView();
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
    public void showViewModel(final PendingTrustRequestsViewModel viewModel) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.replaceAll(viewModel.getTrustRequests());
            }
        });
    }
}
