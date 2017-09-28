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

import butterknife.BindView;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseFragment;

public class ContactRequestsFragment extends BaseFragment<ContactRequestsPresenter> implements ContactRequestsView,
        ContactRequestViewHolder.ContactRequestListeners {

    static final String TAG = ContactRequestsFragment.class.getSimpleName();
    public static final String ACCOUNT_ID = TAG + "accountID";

    @BindView(R.id.requests_list)
    protected RecyclerView mRequestsList;

    @BindView(R.id.pane_ringID)
    protected TextView mPaneTextView;

    @BindView(R.id.emptyTextView)
    protected TextView mEmptyTextView;

    private ContactRequestsAdapter mAdapter;

    @Override
    public int getLayout() {
        return R.layout.frag_pending_contact_requests;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, parent, savedInstanceState);
    }

    public void presentForAccount(Bundle bundle) {
        if (bundle != null && bundle.containsKey(ACCOUNT_ID)) {
            boolean shouldUpdateList = (bundle.getString(ACCOUNT_ID) == null);
            presenter.updateAccount(bundle.getString(ACCOUNT_ID), shouldUpdateList);
            getArguments().putString(ACCOUNT_ID, bundle.getString(ACCOUNT_ID));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_contact_request);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ACCOUNT_ID)) {
            presenter.updateAccount(getArguments().getString(ACCOUNT_ID), false);
        }
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
    public void onAcceptClick(PendingContactRequestsViewModel viewModel) {
        presenter.acceptTrustRequest(viewModel);
    }

    @Override
    public void onRefuseClick(PendingContactRequestsViewModel viewModel) {
        presenter.refuseTrustRequest(viewModel);
    }

    @Override
    public void onBlockClick(PendingContactRequestsViewModel viewModel) {
        presenter.blockTrustRequest(viewModel);
    }

    @Override
    public void updateView(final ArrayList<PendingContactRequestsViewModel> list) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPaneTextView == null || mEmptyTextView == null) {
                    return;
                }

                if (!list.isEmpty()) {
                    PendingContactRequestsViewModel viewModel = list.get(0);
                    if (viewModel.hasPane()) {
                        mPaneTextView.setText(getString(R.string.contact_request_account, viewModel.getAccountUsername()));
                    }
                    mPaneTextView.setVisibility(viewModel.hasPane() ? View.VISIBLE : View.GONE);
                }

                mEmptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                if (mRequestsList.getAdapter() != null) {
                    mAdapter.replaceAll(list);
                } else {
                    mAdapter = new ContactRequestsAdapter(list, ContactRequestsFragment.this);
                    mRequestsList.setAdapter(mAdapter);
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
                    mRequestsList.setLayoutManager(mLayoutManager);
                }
            }
        });
    }


}
