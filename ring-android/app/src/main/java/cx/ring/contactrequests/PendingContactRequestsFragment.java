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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import cx.ring.utils.Log;

public class PendingContactRequestsFragment extends Fragment implements GenericView<PendingContactRequestsViewModel> {

    static final String TAG = PendingContactRequestsFragment.class.getSimpleName();
    public static final String ACCOUNT_ID = TAG + "accountID";

    @Inject
    protected PendingContactRequestsPresenter mPendingContactRequestsPresenter;

    @BindView(R.id.requests_list)
    protected RecyclerView mRequestsList;

    @BindView(R.id.pane_ringID)
    protected TextView mPaneTextView;

    @BindView(R.id.emptyTextView)
    protected TextView mEmptyTextView;

    private Unbinder mUnbinder;
    private ContactRequestsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        setHasOptionsMenu(true);

        final View inflatedView = inflater.inflate(R.layout.frag_pending_contact_requests, parent, false);

        // views injection
        mUnbinder = ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mAdapter = new ContactRequestsAdapter(getActivity(), new ArrayList<TrustRequest>(), mPendingContactRequestsPresenter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRequestsList.setLayoutManager(mLayoutManager);
        mRequestsList.setAdapter(mAdapter);

        return inflatedView;
    }

    public void presentForAccount(Bundle bundle) {
        if (bundle != null && bundle.containsKey(ACCOUNT_ID)) {
            boolean shouldUpdateList = (bundle.getString(ACCOUNT_ID) == null);
            mPendingContactRequestsPresenter.updateAccount(bundle.getString(ACCOUNT_ID), shouldUpdateList);
            getArguments().putString(ACCOUNT_ID, bundle.getString(ACCOUNT_ID));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_contact_request);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ACCOUNT_ID)) {
            mPendingContactRequestsPresenter.updateAccount(getArguments().getString(ACCOUNT_ID), false);
        }
        // view binding
        mPendingContactRequestsPresenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Butterknife unbinding
        mUnbinder.unbind();

        // view unbinding
        mPendingContactRequestsPresenter.unbindView();
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
    public void showViewModel(final PendingContactRequestsViewModel viewModel) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (viewModel.hasPane()) {
                    mPaneTextView.setText(getString(R.string.contact_request_account, viewModel.getAccountUsername()));
                }
                mPaneTextView.setVisibility(viewModel.hasPane() ? View.VISIBLE : View.GONE);
                mAdapter.replaceAll(viewModel.getTrustRequests());

                mEmptyTextView.setVisibility(viewModel.getTrustRequests().isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}
