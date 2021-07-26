/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
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

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.CallActivity;
import net.jami.model.Contact;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.tv.contact.TVContactActivity;
import cx.ring.utils.ConversationPath;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ContactSearchFragment extends BaseSearchFragment<ContactSearchPresenter>
        implements SearchSupportFragment.SearchResultProvider, ContactSearchView {
    private SearchEditText mTextEditor;
    private SearchBar mSearchBar;

    private final ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSearchResultProvider(this);
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> presenter.contactClicked(((ContactCard) item).getModel()));
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.mipmap.ic_launcher));
        setSearchQuery("", false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextEditor = view.findViewById(R.id.lb_search_text_editor);
        mSearchBar = view.findViewById(R.id.lb_search_bar);
        // view injection
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
    public void displayContact(String accountId, final Contact contact) {
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
        Intent intent = new Intent(CallActivity.ACTION_CALL, ConversationPath.toUri(accountID, number), getActivity(), TVCallActivity.class);
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void displayContactDetails(SmartListViewModel model) {
        Intent intent = new Intent(getActivity(), TVContactActivity.class);
        //intent.putExtra(TVContactActivity.CONTACT_REQUEST_URI, model.getContact().getPrimaryUri());
        intent.setDataAndType(ConversationPath.toUri(model.getAccountId(), model.getUri()), TVContactActivity.TYPE_CONTACT_REQUEST_OUTGOING);
        startActivity(intent);
        getActivity().finish();
    }
}
