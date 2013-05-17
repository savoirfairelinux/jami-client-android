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
package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.ContactsAdapter;
import com.savoirfairelinux.sflphone.adapters.StarredContactsAdapter;
import com.savoirfairelinux.sflphone.client.CallActivity;
import com.savoirfairelinux.sflphone.loaders.ContactsLoader;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.views.TACGridView;

public class ContactListFragment extends Fragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<Bundle> {
    final String TAG = "ContactListFragment";
    ContactsAdapter mListAdapter;
    StarredContactsAdapter mGridAdapter;

    String mCurFilter;
    private ISipService service;

    public static final int CONTACT_LOADER = 555;

    @Override
    public void onCreate(Bundle savedInBundle) {
        super.onCreate(savedInBundle);
        mListAdapter = new ContactsAdapter(getActivity());
        mGridAdapter = new StarredContactsAdapter(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In order to onCreateOptionsMenu be called
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(CONTACT_LOADER, null, this);

    }

    ListView list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_contact_list, container, false);
        list = (ListView) inflatedView.findViewById(R.id.contacts_list);

        list.setOnDragListener(dragListener);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {

                Log.i(TAG, "Launch Call Activity");
                Bundle bundle = new Bundle();
                bundle.putParcelable("CallContact", mListAdapter.getItem(pos));
                Intent intent = new Intent().setClass(getActivity(), CallActivity.class);
                intent.putExtras(bundle);
                getActivity().startActivity(intent);

            }
        });

        list.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View view, int pos, long id) {
                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view.findViewById(R.id.photo));
                view.startDrag(null, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                // Log.i(TAG, "On Long Click");
                // final CharSequence[] items = { "Make Call", "Send Message", "Add to Conference" };
                // final SipCall.CallInfo info = new SipCall.CallInfo();
                // info.mDisplayName = (String) ((TextView) v.findViewById(R.id.display_name)).getText();
                // info.mPhone = (String) ((TextView) v.findViewById(R.id.phones)).getText();
                // // TODO getCallInstnace should be implemented in SipCallList
                // // final SipCall call = SipCall.getCallInstance(info);
                // final SipCall call = new SipCall(info);
                //
                // AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // builder.setTitle("Action to perform with " + call.mCallInfo.mDisplayName).setCancelable(true)
                // .setItems(items, new DialogInterface.OnClickListener() {
                // public void onClick(DialogInterface dialog, int item) {
                // Log.i(TAG, "Selected " + items[item]);
                // switch (item) {
                // case 0:
                // // call.placeCallUpdateUi();
                // break;
                // case 1:
                // call.sendTextMessage();
                // // Need to hangup this call immediately since no way to do it after this action
                // call.notifyServiceHangup(service);
                // break;
                // case 2:
                // call.addToConference();
                // // Need to hangup this call immediately since no way to do it after this action
                // call.notifyServiceHangup(service);
                // break;
                // default:
                // break;
                // }
                // }
                // });
                // AlertDialog alert = builder.create();
                // alert.show();

                return true;
            }
        });

        View header = inflater.inflate(R.layout.frag_contact_list_header, null);
        list.addHeaderView(header, null, false);
        TACGridView grid = (TACGridView) header.findViewById(R.id.favorites_grid);

        list.setAdapter(mListAdapter);
        grid.setAdapter(mGridAdapter);
        grid.setExpanded(true);
        grid.setOnDragListener(dragListener);

        grid.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view.findViewById(R.id.photo));
                view.startDrag(null, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            }
        });

        return inflatedView;
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
                v.setBackgroundDrawable(null);
                break;
            case DragEvent.ACTION_DROP:
                View view = (View) event.getLocalState();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Place an action bar item for searching
        MenuItem item = menu.add("Search");
        item.setIcon(R.drawable.ic_menu_search);

        item.setShowAsAction(MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView sv = new SearchView(getActivity());
        sv.setOnQueryTextListener(this);
        item.setActionView(sv);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prefents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(CONTACT_LOADER, null, this);
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

        if (mCurFilter != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }
        ContactsLoader l = new ContactsLoader(getActivity(), baseUri);
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {

        mListAdapter.removeAll();
        mGridAdapter.removeAll();
        ArrayList<CallContact> tmp = data.getParcelableArrayList("Contacts");
        ArrayList<CallContact> tmp2 = data.getParcelableArrayList("Starred");

        Log.w(TAG, "Contact stareed " + tmp2.size());
        mListAdapter.addAll(tmp);
        mGridAdapter.addAll(tmp2);

    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {
        // Thi is called when the last Cursor provided to onLoadFinished
        // mListAdapter.swapCursor(null);
    }
}
