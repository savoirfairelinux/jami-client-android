/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
import java.util.HashMap;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.HistoryAdapter;
import com.savoirfairelinux.sflphone.loaders.HistoryLoader;
import com.savoirfairelinux.sflphone.loaders.LoaderConstants;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.HistoryEntry;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HistoryFragment extends ListFragment implements LoaderCallbacks<ArrayList<HistoryEntry>> {

    private static final String TAG = HistoryFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";

    HistoryAdapter mAdapter;
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallDialed(String account, String to) {
        }

        @Override
        public ISipService getService() {
            Log.i(TAG, "Dummy");
            return null;
        }

    };

    public interface Callbacks {
        public void onCallDialed(String account, String to);

        public ISipService getService();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new HistoryAdapter(getActivity(),new ArrayList<HashMap<String, String>>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_history, parent, false);

        ((ListView)inflatedView.findViewById(android.R.id.list)).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                mAdapter.getItem(pos);
            }
        });
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.w(TAG, "onStart");
        getLoaderManager().initLoader(LoaderConstants.HISTORY_LOADER, null, this);
    }

    public void makeNewCall(int position){
        mCallbacks.onCallDialed(String.valueOf(mAdapter.getItem(position).getAccountID()), mAdapter.getItem(position).getNumber());
    }

    @Override
    public Loader<ArrayList<HistoryEntry>> onCreateLoader(int id, Bundle args) {
        HistoryLoader loader = new HistoryLoader(getActivity(), mCallbacks.getService());
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<HistoryEntry>> arg0, ArrayList<HistoryEntry> history) {
        mAdapter = new HistoryAdapter(this, history);
        getListView().setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public void onLoaderReset(Loader<ArrayList<HistoryEntry>> arg0) {
        // TODO Auto-generated method stub

    }
}
