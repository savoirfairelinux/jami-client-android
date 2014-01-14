/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
package org.sflphone.fragments;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.view.*;
import org.sflphone.R;
import org.sflphone.adapters.ContactPictureTask;
import org.sflphone.client.DetailHistoryActivity;
import org.sflphone.loaders.HistoryLoader;
import org.sflphone.loaders.LoaderConstants;
import org.sflphone.history.HistoryEntry;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

public class HistoryFragment extends ListFragment implements LoaderCallbacks<ArrayList<HistoryEntry>> {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    HistoryAdapter mAdapter;
    private Callbacks mCallbacks = sDummyCallbacks;

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

    public static String ARGS = "Bundle.args";

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

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_history:
                // TODO clean Database!
                    getLoaderManager().restartLoader(LoaderConstants.HISTORY_LOADER, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new HistoryAdapter(getActivity(), new ArrayList<HistoryEntry>());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_history, parent, false);

        return inflatedView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        getListView().setAdapter(mAdapter);

        getListView().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {

                Bundle b = new Bundle();
                b.putParcelable("entry", mAdapter.getItem(pos));
                Intent toStart = new Intent(getActivity(), DetailHistoryActivity.class).putExtra(HistoryFragment.ARGS, b);
                startActivity(toStart);

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.w(TAG, "onStart");
        //getLoaderManager().restartLoader(LoaderConstants.HISTORY_LOADER, null, this);
    }

    public void makeNewCall(int position) {
        mCallbacks.onCallDialed(mAdapter.getItem(position).getNumber());
    }

    public class HistoryAdapter extends BaseAdapter implements ListAdapter {

        Context mContext;
        ArrayList<HistoryEntry> dataset;
        private ExecutorService infos_fetcher = Executors.newCachedThreadPool();

        public HistoryAdapter(Context activity, ArrayList<HistoryEntry> history) {
            mContext = activity;
            dataset = history;
        }

        @Override
        public View getView(final int pos, View convertView, ViewGroup arg2) {

            HistoryView entryView;

            if (convertView == null) {
                // Get a new instance of the row layout view
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.item_history, null);

                // Hold the view objects in an object
                // so they don't need to be re-fetched
                entryView = new HistoryView();
                entryView.photo = (ImageButton) convertView.findViewById(R.id.photo);
                entryView.displayName = (TextView) convertView.findViewById(R.id.display_name);
                entryView.date = (TextView) convertView.findViewById(R.id.date_start);
                entryView.incoming = (TextView) convertView.findViewById(R.id.incomings);
                entryView.outgoing = (TextView) convertView.findViewById(R.id.outgoings);
                entryView.replay = (Button) convertView.findViewById(R.id.replay);
                convertView.setTag(entryView);
            } else {
                entryView = (HistoryView) convertView.getTag();
            }

            // Transfer the stock data from the data object
            // to the view objects

            // SipCall call = (SipCall) mCallList.values().toArray()[position];
            entryView.displayName.setText(dataset.get(pos).getContact().getmDisplayName());
            infos_fetcher.execute(new ContactPictureTask(mContext, entryView.photo, dataset.get(pos).getContact()));

            entryView.incoming.setText(getString(R.string.hist_in_calls, dataset.get(pos).getIncoming_sum()));
            entryView.outgoing.setText(getString(R.string.hist_out_calls, dataset.get(pos).getOutgoing_sum()));

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
                                ((Button) v).setText(getString(R.string.hist_replay_button_stop));
                            } else {
                                mCallbacks.getService().stopRecordedFilePlayback(dataset.get(pos).getCalls().lastEntry().getValue().getRecordPath());
                                v.setTag(R.id.replay, true);
                                ((Button) v).setText(getString(R.string.hist_replay_button));
                            }
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }

            entryView.date.setText(dataset.get(pos).getCalls().lastEntry().getValue().getDate());
            entryView.photo.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    makeNewCall(pos);

                }
            });

            return convertView;

        }

        /**
         * ******************
         * ViewHolder Pattern
         * *******************
         */
        public class HistoryView {
            public ImageButton photo;
            protected TextView displayName;
            protected TextView date;
            private Button replay;
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

        public void addAll(ArrayList<HistoryEntry> history) {
            dataset.addAll(history);
        }

    }

    @Override
    public AsyncTaskLoader<ArrayList<HistoryEntry>> onCreateLoader(int arg0, Bundle arg1) {
        HistoryLoader loader = new HistoryLoader(getActivity());
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<HistoryEntry>> loader, ArrayList<HistoryEntry> data) {
        mAdapter.clear();
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<HistoryEntry>> loader) {

    }


}
