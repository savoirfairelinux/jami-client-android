/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
package cx.ring.tv.search;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.SearchFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SearchEditText;
import androidx.core.content.ContextCompat;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.CallContact;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.utils.Log;

public class RingSearchFragment extends BaseSearchFragment<RingSearchPresenter>
        implements SearchFragment.SearchResultProvider, RingSearchView {

    private static final String TAG = RingSearchFragment.class.getSimpleName();
    @BindView(R.id.lb_search_text_editor)
    SearchEditText mTextEditor;
    private ArrayObjectAdapter mRowsAdapter;
    private Unbinder mUnbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());

        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.mipmap.ic_launcher));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // view injection
        mUnbinder = ButterKnife.bind(this, view);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Disable voice recognition, it is not working properly for blockchain usernames
        setSearchQuery("", false);
        if (mTextEditor != null) {
            mTextEditor.requestFocus();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Butterknife unbinding
        mUnbinder.unbind();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        Log.d(TAG, "getResultsAdapter: " + mRowsAdapter.toString());
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        presenter.queryTextChanged(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search Query Text Submit %s", query));
        return true;
    }

    @Override
    public void displayContact(final CallContact contact) {
            mRowsAdapter.clear();
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));
            listRowAdapter.add(new ContactCard(contact, Card.Type.SEARCH_RESULT));
            HeaderItem header = new HeaderItem(getActivity().getResources().getString(R.string.search_results));
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    @Override
    public void clearSearch() {
        getActivity().runOnUiThread(() -> mRowsAdapter.clear());
    }

    @Override
    public void startCall(String accountID, String number) {
        Intent intent = new Intent(getActivity(), TVCallActivity.class);
        intent.putExtra("account", accountID);
        intent.putExtra("ringId", number);
        getActivity().startActivity(intent, null);
        getActivity().finish();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            presenter.contactClicked(((ContactCard) item).getModel().getContact());
        }
    }

}
