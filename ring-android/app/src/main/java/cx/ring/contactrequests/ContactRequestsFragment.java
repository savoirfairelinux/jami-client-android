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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.fragments.ConversationFragment;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.DeviceUtils;
import cx.ring.viewholders.SmartListViewHolder;

public class ContactRequestsFragment extends BaseSupportFragment<ContactRequestsPresenter> implements ContactRequestsView,
        SmartListViewHolder.SmartListListeners {

    private  static final String TAG = ContactRequestsFragment.class.getSimpleName();
    public static final String ACCOUNT_ID = TAG + "accountID";

    @BindView(R.id.requests_list)
    protected RecyclerView mRequestsList;

    @BindView(R.id.pane_ringID)
    protected TextView mPaneTextView;

    @BindView(R.id.emptyTextView)
    protected TextView mEmptyTextView;

    private SmartListAdapter mAdapter;

    @Override
    public int getLayout() {
        return R.layout.frag_pending_contact_requests;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, parent, savedInstanceState);
    }

    public void presentForAccount(Bundle bundle) {
        if (bundle != null && bundle.containsKey(ACCOUNT_ID)) {
            String accountId = bundle.getString(ACCOUNT_ID);
            presenter.updateAccount(accountId);
            Bundle arguments = getArguments();
            if (arguments != null)
                arguments.putString(ACCOUNT_ID, accountId);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ACCOUNT_ID)) {
            presenter.updateAccount(getArguments().getString(ACCOUNT_ID));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_contact_request);
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
    public void updateView(final List<SmartListViewModel> list) {
        if (mPaneTextView == null || mEmptyTextView == null) {
            return;
        }

        if (!list.isEmpty()) {
            mPaneTextView.setVisibility(/*viewModel.hasPane() ? View.VISIBLE :*/ View.GONE);
        }

        mEmptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        if (mRequestsList.getAdapter() != null) {
            mAdapter.update(list);
        } else {
            mAdapter = new SmartListAdapter(list, ContactRequestsFragment.this);
            mRequestsList.setAdapter(mAdapter);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRequestsList.setLayoutManager(mLayoutManager);
        }
    }

    @Override
    public void updateItem(SmartListViewModel item) {
        if (mAdapter != null) {
            mAdapter.update(item);
        }
    }

    @Override
    public void goToConversation(String accountId, String contactId) {
        Context context = requireContext();
        if (DeviceUtils.isTablet(context)) {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
            bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).startConversationTablet(bundle);
        } else {
            Intent intent = new Intent()
                    .setClass(context, ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
            startActivity(intent);
        }
    }

    @Override
    public void onItemClick(SmartListViewModel viewModel) {
        presenter.contactRequestClicked(viewModel.getAccountId(), viewModel.getContact());
    }

    @Override
    public void onItemLongClick(SmartListViewModel smartListViewModel) {

    }
}
