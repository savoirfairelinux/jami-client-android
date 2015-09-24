/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package cx.ring.fragments;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.adapters.ContactsAdapter;
import cx.ring.adapters.StarredContactsAdapter;
import cx.ring.loaders.ContactsLoader;
import cx.ring.loaders.LoaderConstants;
import cx.ring.model.CallContact;
import cx.ring.views.SwipeListViewTouchListener;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class ContactListFragment extends Fragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<ContactsLoader.Result> {
    public static final String TAG = "ContactListFragment";
    ContactsAdapter mListAdapter;
    StarredContactsAdapter mGridAdapter;
    //SearchView mQuickReturnSearchView;
    String mCurFilter;
    StickyListHeadersListView mContactList;

    // favorite contacts
    private LinearLayout llMain;
    private GridView mStarredGrid;
    private TextView favHeadLabel;
    private SwipeListViewTouchListener mSwipeLvTouchListener;
    private LinearLayout mHeader;
    private ViewGroup newcontact;

    @Override
    public void onCreate(Bundle savedInBundle) {
        super.onCreate(savedInBundle);
        mGridAdapter = new StarredContactsAdapter(getActivity());
        mListAdapter = new ContactsAdapter(this);
        setHasOptionsMenu(true);
    }

    public Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallContact(CallContact c) {
        }

        @Override
        public void onTextContact(CallContact c) {
        }

        @Override
        public void onEditContact(CallContact c) {
        }
/*
        @Override
        public ISipService getService() {
            Log.i(TAG, "Dummy");
            return null;
        }
*/
        @Override
        public void onContactDragged() {
        }

        @Override
        public void toggleDrawer() {
        }

        @Override
        public void setDragView(RelativeLayout relativeLayout) {
            
        }

        @Override
        public void toggleForSearchDrawer() {
        }
/*
        @Override
        public SearchView getSearchView() {
            return null;
        }*/
    };

    public interface Callbacks {
        void onCallContact(CallContact c);

        void onTextContact(CallContact c);

        //public ISipService getService();

        void onContactDragged();

        void toggleDrawer();

        void onEditContact(CallContact item);

        void setDragView(RelativeLayout relativeLayout);

        void toggleForSearchDrawer();

        //SearchView getSearchView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.newconv_option_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.contact_search).getActionView();
        searchView.setOnQueryTextListener(ContactListFragment.this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_contact_list, container, false);
        mHeader = (LinearLayout) inflater.inflate(R.layout.frag_contact_list_header, null);
        mContactList = (StickyListHeadersListView) inflatedView.findViewById(R.id.contacts_stickylv);
        //mContactList.setDividerHeight(0);
        mContactList.setDivider(null);

        inflatedView.findViewById(R.id.drag_view).setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        /*inflatedView.findViewById(R.id.contact_search_button).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mContactList.smoothScrollToPosition(0);
                mQuickReturnSearchView.setOnQueryTextListener(ContactListFragment.this);
                mQuickReturnSearchView.setIconified(false);
                mQuickReturnSearchView.setFocusable(true);
                mCallbacks.toggleForSearchDrawer();
            }
        });

        inflatedView.findViewById(R.id.slider_button).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCallbacks.toggleDrawer();
            }
        });
        
        mCallbacks.setDragView(((RelativeLayout) inflatedView.findViewById(R.id.slider_button)));
*/
        //mQuickReturnSearchView = (SearchView) mHeader.findViewById(R.id.contact_search);
        mStarredGrid = (GridView) mHeader.findViewById(R.id.favorites_grid);
        llMain = (LinearLayout) mHeader.findViewById(R.id.llMain);
        favHeadLabel = (TextView) mHeader.findViewById(R.id.fav_head_label);
        newcontact = (ViewGroup) mHeader.findViewById(R.id.newcontact_element);
        newcontact.setVisibility(View.GONE);
        return inflatedView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContactList.addHeaderView(mHeader, null, false);
        mContactList.setAdapter(mListAdapter);

        mStarredGrid.setAdapter(mGridAdapter);
        /*mQuickReturnSearchView.setIconifiedByDefault(false);

        mQuickReturnSearchView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mQuickReturnSearchView.setIconified(false);
                mQuickReturnSearchView.setFocusable(true);
            }
        });
        mQuickReturnSearchView.setOnQueryTextListener(ContactListFragment.this);*/

        getLoaderManager().initLoader(LoaderConstants.CONTACT_LOADER, null, this);

    }

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> av, View view, int pos, long id) {
            DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view.findViewById(R.id.photo));
            view.startDrag(null, shadowBuilder, view, 0);
            mCallbacks.onContactDragged();
            return true;
        }

    };

    private void setGridViewListeners() {
        mStarredGrid.setOnDragListener(dragListener);
        mStarredGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
                mCallbacks.onCallContact(mGridAdapter.getItem(pos));
            }
        });
        mStarredGrid.setOnItemLongClickListener(mItemLongClickListener);
    }

    private void setListViewListeners() {
        mSwipeLvTouchListener = new SwipeListViewTouchListener(mContactList.getWrappedList(), new SwipeListViewTouchListener.OnSwipeCallback() {
            @Override
            public void onSwipeLeft(ListView listView, int[] reverseSortedPositions) {
            }

            @Override
            public void onSwipeRight(ListView listView, View down) {
                down.findViewById(R.id.quick_edit).setClickable(true);
                down.findViewById(R.id.quick_discard).setClickable(true);
                down.findViewById(R.id.quick_starred).setClickable(true);

            }
        }, true, false);

        mContactList.getWrappedList().setOnDragListener(dragListener);
        mContactList.getWrappedList().setOnTouchListener(mSwipeLvTouchListener);
        mContactList.getWrappedList().setOnItemLongClickListener(mItemLongClickListener);
        mContactList.getWrappedList().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                Log.i(TAG, "Opening Item");
                mSwipeLvTouchListener.openItem(view, pos, id);
            }
        });
    }

    OnDragListener dragListener = new OnDragListener() {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Do nothing
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                // v.setBackgroundDrawable(null);
                break;
            case DragEvent.ACTION_DROP:
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                View view1 = (View) event.getLocalState();
                view1.setVisibility(View.VISIBLE);
            default:
                break;
            }
            return true;
        }

    };

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.isEmpty()) {
            getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, null, this);
            newcontact.setVisibility(View.GONE);
            return true;
        }
        mCurFilter = newText;
        Bundle b = new Bundle();
        b.putString("filter", mCurFilter);
        getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, b, this);
        newcontact.setVisibility(View.VISIBLE);
        ((TextView)newcontact.findViewById(R.id.display_name)).setText("Call \"" + newText + "\"");

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Return false to let the SearchView perform the default action
        return false;
    }

    @Override
    public Loader<ContactsLoader.Result> onCreateLoader(int id, Bundle args) {
        Uri baseUri;

        Log.i(TAG, "createLoader");

        if (args != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(args.getString("filter")));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }
        ContactsLoader l = new ContactsLoader(getActivity(), baseUri);
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {
        mListAdapter.setData(data.contacts);
        setListViewListeners();

        if (data.starred.isEmpty()) {
            llMain.setVisibility(View.GONE);
            favHeadLabel.setVisibility(View.GONE);
            mGridAdapter.removeAll();
        } else {
            llMain.setVisibility(View.VISIBLE);
            favHeadLabel.setVisibility(View.VISIBLE);
            mGridAdapter.setData(data.starred);
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
