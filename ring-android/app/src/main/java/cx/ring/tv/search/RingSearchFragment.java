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
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SearchBar;
import androidx.leanback.widget.SearchEditText;
import androidx.core.content.ContextCompat;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.utils.Log;

public class RingSearchFragment extends BaseSearchFragment<RingSearchPresenter>
        implements SearchSupportFragment.SearchResultProvider, RingSearchView {

    private static final String TAG = RingSearchFragment.class.getSimpleName();
    @BindView(R.id.lb_search_text_editor)
    SearchEditText mTextEditor;

    @BindView(R.id.lb_search_bar)
    SearchBar mSearchBar;

    private final ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
    private Unbinder mUnbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSearchResultProvider(this);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> presenter.contactClicked(((ContactCard) item).getModel().getContact()));
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.mipmap.ic_launcher));
        setSearchQuery("", false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // view injection
        mUnbinder = ButterKnife.bind(this, view);
        mSearchBar.setSearchBarListener(new SearchBar.SearchBarListener() {
            @Override
            public void onSearchQueryChange(String query) {
                onQueryTextChange(query);
            }

            @Override
            public void onSearchQuerySubmit(String query) {
                onQueryTextSubmit(query);
            }

            @Override
            public void onKeyboardDismiss(String query) {
                mSearchBar.postDelayed(()-> {
                    getRowsSupportFragment().getVerticalGridView().requestFocus();
                }, 200);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTextEditor != null) {
            mTextEditor.requestFocus();
        }
    }

    @Override
    public void onDestroyView() {
        mUnbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        presenter.queryTextChanged(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        presenter.queryTextChanged(query);
        return true;
    }

    @Override
    public void displayContact(String accountId, final CallContact contact) {
        mRowsAdapter.clear();
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));
        listRowAdapter.add(new ContactCard(accountId, contact, Card.Type.SEARCH_RESULT));
        HeaderItem header = new HeaderItem(getActivity().getResources().getString(R.string.search_results));
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    @Override
    public void clearSearch() {
        mRowsAdapter.clear();
    }

    @Override
    public void startCall(String accountID, String number) {
        Intent intent = new Intent(getActivity(), TVCallActivity.class);
        intent.putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountID);
        intent.putExtra(ConversationFragment.KEY_CONTACT_RING_ID, number);
        getActivity().startActivity(intent, null);
        getActivity().finish();
    }
}
