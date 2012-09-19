/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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
package com.savoirfairelinux.sflphone.client;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.text.TextUtils;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.util.Log;

import com.savoirfairelinux.sflphone.R;

public class ContactListFragment extends ListFragment implements OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor>
{
    ContactManager mContactManager;
    CallElementList.CallElementAdapter mAdapter;
    String mCurFilter;

    // These are the Contacts rows that we will retrieve.
    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID, Contacts.DISPLAY_NAME,
                                                                       Contacts.PHOTO_ID, Contacts.LOOKUP_KEY };
    static final String[] CONTACTS_PHONES_PROJECTION = new String[] { Phone.NUMBER, Phone.TYPE };
    static final String[] CONTACTS_SIP_PROJECTION = new String[] { SipAddress.SIP_ADDRESS, SipAddress.TYPE };

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // In order to onCreateOptionsMenu be called 
        setHasOptionsMenu(true);

        mContactManager = new ContactManager(getActivity());

        mAdapter = new CallElementList.CallElementAdapter(getActivity(), mContactManager);
        setListAdapter(mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.call_element_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Place an action bar item for searching
        MenuItem item = menu.add("Search");
        item.setIcon(R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView sv = new SearchView(getActivity());
        sv.setOnQueryTextListener(this);
        item.setActionView(sv);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        // Insert desired behavior here.
        CallContact contact = mContactManager.getContact(position);
        Log.i("ContactListFragment", "Contact clicked: " + contact.getDisplayName());

        SipCall call = SipCall.getCallInstance(contact);
        Log.i("ConatctListFragment", "OK");
        Log.i("ContactListFragment", "Number of calls " + SipCall.getNbCalls());
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prefents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) { return true; }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) { return true; }
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true; 
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        // Return false to let the SearchView perform the default action
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        Uri baseUri;

        if(mCurFilter != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        String select = "((" + Contacts.DISPLAY_NAME
                                  + " NOTNULL) AND ("
                                  + Contacts.HAS_PHONE_NUMBER
                                  + "=1) AND ("
                                  + Contacts.DISPLAY_NAME
                                  + " != '' ))";

        return new CursorLoader(getActivity(), baseUri, CONTACTS_SUMMARY_PROJECTION,
                                    select, null, Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        // Swap the new cursor in.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        // Thi is called when the last Cursor provided to onLoadFinished 
    }
}
