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

package org.sflphone.fragments;

import java.util.ArrayList;
import java.util.NavigableMap;

import org.sflphone.R;
import org.sflphone.adapters.ContactPictureTask;
import org.sflphone.model.HistoryEntry;
import org.sflphone.model.HistoryEntry.HistoryCall;
import org.sflphone.service.ISipService;
import org.sflphone.views.parallaxscrollview.AnotherView;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DetailsHistoryEntryFragment extends Fragment {

    View mheaderView;
    DetailHistoryAdapter mAdapter;
    HistoryEntry toDisplay;
    private static final String TAG = DetailsHistoryEntryFragment.class.getSimpleName();
    ContactPictureTask tasker;

    private ListView lvMain;
    private LinearLayout llMain, llMainHolder;
    private AnotherView anotherView;
    private RelativeLayout iv;

    private Callbacks mCallbacks = sDummyCallbacks;

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }

    };

    public interface Callbacks {

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

        toDisplay = (HistoryEntry) getArguments().get("entry");
        mAdapter = new DetailHistoryAdapter(toDisplay.getCalls(), getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_history_detail, parent, false);

        // mheaderView = LayoutInflater.from(getActivity()).inflate(R.layout.history_detail_header, null);

        llMain = (LinearLayout) inflatedView.findViewById(R.id.llMain);
        llMainHolder = (LinearLayout) inflatedView.findViewById(R.id.llMainHolder);
        lvMain = (ListView) inflatedView.findViewById(R.id.lvMain);
        lvMain.setAdapter(mAdapter);
        iv = (RelativeLayout) inflatedView.findViewById(R.id.iv);

        ((TextView) iv.findViewById(R.id.history_call_name)).setText(toDisplay.getContact().getmDisplayName());
        tasker = new ContactPictureTask(getActivity(), (ImageView) iv.findViewById(R.id.contact_photo), toDisplay.getContact());
        tasker.run();
        anotherView = (AnotherView) inflatedView.findViewById(R.id.anotherView);

        lvMain.post(new Runnable() {

            @Override
            public void run() {

                // Adjusts llMain's height to match ListView's height
                setListViewHeight(lvMain, llMain);

                // LayoutParams to set the top margin of LinearLayout holding
                // the content.
                // topMargin = iv.getHeight() - tvTitle.getHeight()
                LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) llMainHolder.getLayoutParams();
                // p.topMargin = iv.getHeight() - tvTitle.getHeight();
                llMainHolder.setLayoutParams(p);
            }
        });
        return inflatedView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    // Sets the ListView holder's height
    public void setListViewHeight(ListView listView, LinearLayout llMain) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {

            return;
        }

        int totalHeight = 0;
        int firstHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);

        for (int i = 0; i < listAdapter.getCount(); i++) {

            if (i == 0) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
                firstHeight = listItem.getMeasuredHeight();
            }
            totalHeight += firstHeight;
        }

        // totalHeight -= iv.getMeasuredHeight();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMain.getLayoutParams();

        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        llMain.setLayoutParams(params);
        anotherView.requestLayout();
    }

    private class DetailHistoryAdapter extends BaseAdapter implements ListAdapter {

        ArrayList<HistoryCall> dataset;
        Context mContext;

        public DetailHistoryAdapter(NavigableMap<Long, HistoryCall> calls, Context c) {
            dataset = new ArrayList<HistoryCall>(calls.descendingMap().values());
            mContext = c;
        }

        @Override
        public int getCount() {
            return dataset.size();
        }

        @Override
        public Object getItem(int position) {
            return dataset.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HistoryCallView entryView = null;

            if (convertView == null) {
                // Get a new instance of the row layout view
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.item_history_call, null);

                // Hold the view objects in an object
                // so they don't need to be re-fetched
                entryView = new HistoryCallView();
                entryView.historyCallState = (TextView) convertView.findViewById(R.id.history_call_state);
                entryView.formatted_date = (TextView) convertView.findViewById(R.id.history_call_date_formatted);
                entryView.record = (Button) convertView.findViewById(R.id.history_call_record);
                entryView.duration = (TextView) convertView.findViewById(R.id.history_call_duration);

                convertView.setTag(entryView);
            } else {
                entryView = (HistoryCallView) convertView.getTag();
            }

            final HistoryCall item = dataset.get(position);

            entryView.historyCallState.setText(item.getState());
            entryView.formatted_date.setText(item.getDate());
            // entryView.displayName.setText(item.getDisplayName());
            entryView.duration.setText(item.getDurationString());

            if (item.hasRecord()) {
                entryView.record.setVisibility(View.VISIBLE);
                entryView.record.setTag(R.id.history_call_record, true);
                entryView.record.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            if ((Boolean) v.getTag(R.id.history_call_record)) {
                                mCallbacks.getService().startRecordedFilePlayback(item.getRecordPath());
                                v.setTag(R.id.replay, false);
                                ((Button) v).setText(getString(R.string.hist_replay_button_stop));
                            } else {
                                mCallbacks.getService().stopRecordedFilePlayback(item.getRecordPath());
                                v.setTag(R.id.history_call_record, true);
                                ((Button) v).setText(getString(R.string.hist_replay_button));
                            }
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }

            return convertView;
        }

        /*********************
         * ViewHolder Pattern
         *********************/
        public class HistoryCallView {
            protected TextView historyCallState;
            protected TextView formatted_date;
            protected Button record;
            protected TextView duration;
        }

    }

}
