/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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
package org.sflphone.fragments;

import java.util.ArrayList;

import org.sflphone.R;
import org.sflphone.adapters.ContactsAdapter;
import org.sflphone.adapters.StarredContactsAdapter;
import org.sflphone.loaders.ContactsLoader;
import org.sflphone.loaders.LoaderConstants;
import org.sflphone.model.CallContact;
import org.sflphone.service.ISipService;
import org.sflphone.views.SwipeListViewTouchListener;
import org.sflphone.views.stickylistheaders.StickyListHeadersListView;

import android.annotation.SuppressLint;
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
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

public class ContactListFragment extends Fragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<Bundle> {
    private static final String TAG = "ContactListFragment";
    ContactsAdapter mListAdapter;
    StarredContactsAdapter mGridAdapter;
    SearchView mQuickReturnSearchView;
    String mCurFilter;
    StickyListHeadersListView mContactList;
    private GridView mStarredGrid;

    private int mCachedVerticalScrollRange;
    private int mQuickReturnHeight;

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    private int mState = STATE_ONSCREEN;
    private int mScrollY;
    private int mMinRawY = 0;

    @Override
    public void onCreate(Bundle savedInBundle) {
        super.onCreate(savedInBundle);
        mGridAdapter = new StarredContactsAdapter(getActivity());
        mListAdapter = new ContactsAdapter(this);
    }

    public Callbacks mCallbacks = sDummyCallbacks;
    private LinearLayout llMain;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallContact(CallContact c) {
        }

        @Override
        public void onTextContact(CallContact c) {
        }

        @Override
        public void onEditContact(CallContact c) {
        }

        @Override
        public ISipService getService() {
            Log.i(TAG, "Dummy");
            return null;
        }

        @Override
        public void onContactDragged() {
        }

        @Override
        public void openDrawer() {
        }
    };

    public interface Callbacks {
        void onCallContact(CallContact c);

        void onTextContact(CallContact c);

        public ISipService getService();

        void onContactDragged();

        void openDrawer();

        void onEditContact(CallContact item);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_contact_list, container, false);
        mHeader = (LinearLayout) inflater.inflate(R.layout.frag_contact_list_header, null);
        mContactList = (StickyListHeadersListView) inflatedView.findViewById(R.id.contacts_list);
        
        ((ImageButton) inflatedView.findViewById(R.id.contact_search_button)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mContactList.smoothScrollToPosition(0);
                mQuickReturnSearchView.setOnQueryTextListener(ContactListFragment.this);
                mQuickReturnSearchView.setIconified(false);
                mQuickReturnSearchView.setFocusable(true);
                mCallbacks.openDrawer();
            }
        });
        
        mQuickReturnSearchView = (SearchView) mHeader.findViewById(R.id.contact_search);
        mStarredGrid = (GridView) mHeader.findViewById(R.id.favorites_grid);
        llMain = (LinearLayout) mHeader.findViewById(R.id.llMain);
        return inflatedView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContactList.addHeaderView(mHeader, null, false);
        mContactList.setAdapter(mListAdapter);

        mStarredGrid.setAdapter(mGridAdapter);
        mQuickReturnSearchView.setIconifiedByDefault(false);

        mQuickReturnSearchView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mQuickReturnSearchView.setOnQueryTextListener(ContactListFragment.this);
                mQuickReturnSearchView.setIconified(false);
                mQuickReturnSearchView.setFocusable(true);
            }
        });

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
    private SwipeListViewTouchListener mSwipeLvTouchListener;
    private View mPlaceHolder;
    private LinearLayout mHeader;

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

                mSwipeLvTouchListener.openItem(view, pos, id);
            }
        });

        // mContactList.getWrappedList().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        // @Override
        // public void onGlobalLayout() {
        // if (!mContactList.scrollYIsComputed()) {
        // mQuickReturnHeight = mQuickReturnSearchView.getHeight();
        // mContactList.computeScrollY();
        // mCachedVerticalScrollRange = mContactList.getListHeight();
        // }
        // }
        // });
        // mContactList.setOnScrollListener(mScrollListener);
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

        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        // String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prefents restarting the loader when restoring state.
        // if (mCurFilter == null && newFilter == null) {
        // return true;
        // }
        // if (mCurFilter != null && mCurFilter.equals(newText)) {
        // return true;
        // }
        if (newText.isEmpty()) {
            getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, null, this);
            return true;
        }
        mCurFilter = newText;
        Bundle b = new Bundle();
        b.putString("filter", mCurFilter);
        getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, b, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Return false to let the SearchView perform the default action
        return false;
    }

    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle args) {
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
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {

        mGridAdapter.removeAll();
        mListAdapter.clear();
        ArrayList<CallContact> tmp = data.getParcelableArrayList("Contacts");
        ArrayList<CallContact> tmp2 = data.getParcelableArrayList("Starred");
        mListAdapter.addAll(tmp);
        mGridAdapter.addAll(tmp2);

        setListViewListeners();
        setGridViewListeners();

        mStarredGrid.post(new Runnable() {

            @Override
            public void run() {
                setGridViewHeight(mStarredGrid, llMain);
            }
        });

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

        params.height = (int) (totalHeight + (getResources().getDimension(R.dimen.contact_vertical_spacing) * (rows - 1)));
        ;
        llMain.setLayoutParams(params);
        mHeader.requestLayout();
    }

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @SuppressLint("NewApi")
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            mScrollY = 0;
            int translationY = 0;

            if (mContactList.scrollYIsComputed()) {
                mScrollY = mContactList.getComputedScrollY();
            }

            int rawY = mPlaceHolder.getTop() - Math.min(mCachedVerticalScrollRange - mContactList.getHeight(), mScrollY);

            switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
            }

            mQuickReturnSearchView.setTranslationY(translationY);

        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    };

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {
    }

    public void setHandleView(RelativeLayout handle) {

        ((ImageButton) handle.findViewById(R.id.contact_search_button)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mContactList.smoothScrollToPosition(0);
                mQuickReturnSearchView.setOnQueryTextListener(ContactListFragment.this);
                mQuickReturnSearchView.setIconified(false);
                mQuickReturnSearchView.setFocusable(true);
                mCallbacks.openDrawer();
            }
        });

    }

}
