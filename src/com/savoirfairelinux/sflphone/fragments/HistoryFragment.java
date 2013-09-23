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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.ContactPictureTask;
import com.savoirfairelinux.sflphone.loaders.HistoryLoader;
import com.savoirfairelinux.sflphone.loaders.LoaderConstants;
import com.savoirfairelinux.sflphone.model.HistoryEntry;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HistoryFragment extends ListFragment implements LoaderCallbacks<ArrayList<HistoryEntry>> {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    HistoryAdapter mAdapter;
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallDialed(String to) {
        }

        @Override
        public ISipService getService() {
            Log.i(TAG, "Dummy");
            return null;
        }

    };

    public interface Callbacks {
        public void onCallDialed(String to);

        public ISipService getService();

    }

    @Override
    public void onAttach(Activity activity) {
        Log.i(TAG, "Attaching HISTORY");
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        getLoaderManager().initLoader(LoaderConstants.HISTORY_LOADER, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_history, parent, false);

        ((ListView) inflatedView.findViewById(android.R.id.list)).setOnItemClickListener(new OnItemClickListener() {

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
        getLoaderManager().restartLoader(LoaderConstants.HISTORY_LOADER, null, this);
    }

    public void makeNewCall(int position) {
        mCallbacks.onCallDialed(mAdapter.getItem(position).getNumber());
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

    public class HistoryAdapter extends BaseAdapter {

        HistoryFragment mContext;
        ArrayList<HistoryEntry> dataset;
        private ExecutorService infos_fetcher = Executors.newCachedThreadPool();

        public HistoryAdapter(HistoryFragment activity, ArrayList<HistoryEntry> history) {
            mContext = activity;
            dataset = history;
        }

        @Override
        public View getView(final int pos, View convertView, ViewGroup arg2) {
            View rowView = convertView;
            HistoryView entryView = null;

            if (rowView == null) {
                // Get a new instance of the row layout view
                LayoutInflater inflater = LayoutInflater.from(mContext.getActivity());
                rowView = inflater.inflate(R.layout.item_history, null);

                // Hold the view objects in an object
                // so they don't need to be re-fetched
                entryView = new HistoryView();
                entryView.photo = (ImageView) rowView.findViewById(R.id.photo);
                entryView.displayName = (TextView) rowView.findViewById(R.id.display_name);
                entryView.duration = (TextView) rowView.findViewById(R.id.duration);
                entryView.date = (TextView) rowView.findViewById(R.id.date_start);
                entryView.missed = (TextView) rowView.findViewById(R.id.missed);
                entryView.incoming = (TextView) rowView.findViewById(R.id.incomings);
                entryView.outgoing = (TextView) rowView.findViewById(R.id.outgoings);
                entryView.replay = (Button) rowView.findViewById(R.id.replay);
                entryView.call_button = (ImageButton) rowView.findViewById(R.id.action_call);
                entryView.call_button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mContext.makeNewCall(pos);

                    }
                });
                rowView.setTag(entryView);
            } else {
                entryView = (HistoryView) rowView.getTag();
            }

            // Transfer the stock data from the data object
            // to the view objects

            // SipCall call = (SipCall) mCallList.values().toArray()[position];
            entryView.displayName.setText(dataset.get(pos).getContact().getmDisplayName());

            infos_fetcher.execute(new ContactPictureTask(mContext.getActivity(), entryView.photo, dataset.get(pos).getContact().getId()));

            entryView.missed.setText("Missed:" + dataset.get(pos).getMissed_sum());
            entryView.incoming.setText("In:" + dataset.get(pos).getIncoming_sum());
            entryView.outgoing.setText("Out:" + dataset.get(pos).getOutgoing_sum());

            if (dataset.get(pos).getCalls().lastEntry().getValue().getRecordPath().length() > 0) {
                entryView.replay.setVisibility(View.VISIBLE);
                entryView.replay.setTag(R.id.replay, true);
                entryView.replay.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            if ((Boolean) v.getTag(R.id.replay)) {
                                mCallbacks.getService().startRecordedFilePlayback(dataset.get(pos).getCalls().lastEntry().getValue().getRecordPath());
                                v.setTag(R.id.replay, false);
                                ((Button)v).setText("Stop");
                            } else {
                                mCallbacks.getService().stopRecordedFilePlayback(dataset.get(pos).getCalls().lastEntry().getValue().getRecordPath());
                                v.setTag(R.id.replay, true);
                                ((Button)v).setText("Replay");
                            }
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }

            entryView.date.setText(dataset.get(pos).getCalls().lastEntry().getValue().getDate("yyyy-MM-dd"));
            entryView.duration.setText(dataset.get(pos).getTotalDuration());

            return rowView;

        }

        /*********************
         * ViewHolder Pattern
         *********************/
        public class HistoryView {
            public ImageView photo;
            protected TextView displayName;
            protected TextView date;
            public TextView duration;
            private ImageButton call_button;
            private Button replay;
            private TextView missed;
            private TextView outgoing;
            private TextView incoming;
        }

        @Override
        public int getCount() {

            return dataset.size();
        }

        @Override
        public HistoryEntry getItem(int pos) {
            return dataset.get(pos);
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        public void clear() {
            dataset.clear();

        }

        public void addAll(ArrayList<HashMap<String, String>> history) {
            // dataset.addAll(history);

        }

    }

}
