/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import cx.ring.R;
import cx.ring.adapters.ContactsAdapter;
import cx.ring.adapters.StarredContactsAdapter;
import cx.ring.loaders.ContactsLoader;
import cx.ring.loaders.LoaderConstants;
import cx.ring.model.CallContact;
import cx.ring.service.LocalService;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class ContactListFragment extends Fragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<ContactsLoader.Result> {
    public static final String TAG = "ContactListFragment";
    ContactsAdapter mListAdapter;
    StarredContactsAdapter mGridAdapter;
    String mCurFilter;
    StickyListHeadersListView mContactList;

    // favorite contacts
    private LinearLayout llMain;
    private GridView mStarredGrid;
    private TextView favHeadLabel;
    private LinearLayout mHeader;
    private ViewGroup newcontact;

    @Override
    public void onCreate(Bundle savedInBundle) {
        super.onCreate(savedInBundle);
        setHasOptionsMenu(true);
    }

    public Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static class DummyCallbacks extends LocalService.DummyCallbacks implements Callbacks {
        @Override
        public void onCallContact(CallContact c) {
        }
        @Override
        public void onTextContact(CallContact c) {
        }
    };
    private static final Callbacks sDummyCallbacks = new DummyCallbacks();

    public interface Callbacks extends LocalService.Callbacks {
        void onCallContact(CallContact c);
        void onTextContact(CallContact c);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
        mGridAdapter = new StarredContactsAdapter(getActivity());
        mListAdapter = new ContactsAdapter(getActivity(), mCallbacks, mCallbacks.getService().get40dpContactCache(), mCallbacks.getService().getThreadPool());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.newconv_option_menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_contact_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(ContactListFragment.this);
        searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        searchMenuItem.expandActionView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_contact_list, container, false);
        mHeader = (LinearLayout) inflater.inflate(R.layout.frag_contact_list_header, null);
        mContactList = (StickyListHeadersListView) inflatedView.findViewById(R.id.contacts_stickylv);
        mContactList.setDivider(null);

        mStarredGrid = (GridView) mHeader.findViewById(R.id.favorites_grid);
        llMain = (LinearLayout) mHeader.findViewById(R.id.llMain);
        favHeadLabel = (TextView) mHeader.findViewById(R.id.fav_head_label);
        newcontact = (ViewGroup) mHeader.findViewById(R.id.newcontact_element);
        newcontact.setVisibility(View.GONE);
        newcontact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) v.getTag();
                if (c != null)
                    mCallbacks.onTextContact(c);
            }
        });
        newcontact.findViewById(R.id.quick_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) newcontact.getTag();
                if (c != null)
                   mCallbacks.onCallContact(c);
            }
        });
        return inflatedView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContactList.addHeaderView(mHeader, null, false);
        mContactList.setAdapter(mListAdapter);

        mStarredGrid.setAdapter(mGridAdapter);

        onLoadFinished(null, mCallbacks.getService().getSortedContacts());
    }

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> av, View view, int pos, long id) {
            return false;
        }

    };

    private void setGridViewListeners() {
        //mStarredGrid.setOnDragListener(dragListener);
        mStarredGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
                mCallbacks.onCallContact(mGridAdapter.getItem(pos));
            }
        });
        mStarredGrid.setOnItemLongClickListener(mItemLongClickListener);
    }

    private void setListViewListeners() {
        mContactList.getWrappedList().setOnItemLongClickListener(mItemLongClickListener);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCurFilter = newText;
        if (newText.isEmpty()) {
            getLoaderManager().destroyLoader(LoaderConstants.CONTACT_LOADER);
            onLoadFinished(null, mCallbacks.getService().getSortedContacts());
            newcontact.setVisibility(View.GONE);
            return true;
        }
        Bundle b = new Bundle();
        b.putString("filter", mCurFilter);
        getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, b, this);
        newcontact.setVisibility(View.VISIBLE);
        ((TextView)newcontact.findViewById(R.id.display_name)).setText(newText);
        CallContact contact = CallContact.buildUnknown(newText);
        newcontact.setTag(contact);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        newcontact.callOnClick();
        return true;
    }

    @Override
    public Loader<ContactsLoader.Result> onCreateLoader(int id, Bundle args) {
        Uri baseUri = null;
        if (args != null)
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(args.getString("filter")));
        ContactsLoader l = new ContactsLoader(getActivity(), baseUri, mCallbacks.getService().getContactCache());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {
        Log.i(TAG, "onLoadFinished with " + data.contacts.size() + " contacts, " + data.starred.size() + " starred.");

        mListAdapter.setData(data.contacts);
        setListViewListeners();

        mGridAdapter.setData(data.starred);
        if (data.starred.isEmpty()) {
            llMain.setVisibility(View.GONE);
            favHeadLabel.setVisibility(View.GONE);
        } else {
            llMain.setVisibility(View.VISIBLE);
            favHeadLabel.setVisibility(View.VISIBLE);
            setGridViewListeners();
            mStarredGrid.post(new Runnable() {

                @Override
                public void run() {
                    setGridViewHeight(mStarredGrid, llMain);
                }
            });
        }
    }

    // Sets the GridView holder's height to fully expand it
    public void setGridViewHeight(GridView gridView, LinearLayout llMain) {
        ListAdapter listAdapter = gridView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int firstHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(gridView.getWidth(), MeasureSpec.AT_MOST);

        int rows = (listAdapter.getCount() + gridView.getNumColumns() - 1) / gridView.getNumColumns();

        for (int i = 0; i < rows; i++) {
            if (i == 0) {
                View listItem = listAdapter.getView(i, null, gridView);
                listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
                firstHeight = listItem.getMeasuredHeight();
            }
            totalHeight += firstHeight;
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMain.getLayoutParams();

        params.height = (int) (totalHeight + (getResources().getDimension(R.dimen.contact_vertical_spacing) * (rows - 1) + llMain.getPaddingBottom() + llMain.getPaddingTop()));
        llMain.setLayoutParams(params);
        mHeader.requestLayout();
    }

    @Override
    public void onLoaderReset(Loader<ContactsLoader.Result> loader) {
    }
}
