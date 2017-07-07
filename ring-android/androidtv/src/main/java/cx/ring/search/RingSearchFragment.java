/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
package cx.ring.search;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.CardPresenter;
import cx.ring.model.CallContact;
import cx.ring.model.Uri;
import cx.ring.utils.Log;

public class RingSearchFragment extends BaseSearchFragment<RingSearchPresenter>
        implements SearchFragment.SearchResultProvider, RingSearchView {

    private static final String TAG = RingSearchFragment.class.getSimpleName();

    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        Log.d(TAG, mRowsAdapter.toString());
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRowsAdapter.clear();
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
                listRowAdapter.add(contact);
                HeaderItem header = new HeaderItem(getActivity().getResources().getString(R.string.search_results));
                mRowsAdapter.add(new ListRow(header, listRowAdapter));
            }
        });
    }

    @Override
    public void clearSearch() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRowsAdapter.clear();
            }
        });
    }

    @Override
    public void startCall(String accountID, Uri number) {
        Intent intent = new Intent(getActivity(), CallActivity.class);
        intent.putExtra("account", accountID);
        intent.putExtra("ringId", number.toString());
        startActivity(intent);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            presenter.contactClicked(((CallContact)item));
        }
    }

}
