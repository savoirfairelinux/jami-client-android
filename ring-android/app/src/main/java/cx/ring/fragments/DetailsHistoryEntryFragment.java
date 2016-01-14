/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 */

package cx.ring.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.model.account.Account;
import cx.ring.model.SipCall;
import cx.ring.service.IDRingService;

import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;

public class DetailsHistoryEntryFragment extends Fragment {

    DetailHistoryAdapter mAdapter;
    HistoryEntry toDisplay;
    @SuppressWarnings("unused")
    private static final String TAG = DetailsHistoryEntryFragment.class.getSimpleName();
    ContactPictureTask tasker;

    private ListView lvMain;
    private LinearLayout llMain;
    private RelativeLayout iv;

    private Callbacks mCallbacks = sDummyCallbacks;

    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public IDRingService getService() {
            return null;
        }

        @Override
        public void onCall(String account, String number) {
        }

    };

    public interface Callbacks {

        IDRingService getService();

        void onCall(String account, String number);

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

        llMain = (LinearLayout) inflatedView.findViewById(R.id.llMain);
        /*llMainHolder = (LinearLayout) inflatedView.findViewById(R.id.llMainHolder);*/
        lvMain = (ListView) inflatedView.findViewById(R.id.lvMain);
        lvMain.setAdapter(mAdapter);
        iv = (RelativeLayout) inflatedView.findViewById(R.id.iv);

        ((TextView) iv.findViewById(R.id.history_call_name)).setText(toDisplay.getContact().getDisplayName());

        tasker = new ContactPictureTask(getActivity(), (ImageView) inflatedView.findViewById(R.id.contact_photo), toDisplay.getContact());
        tasker.run();
//        ((TextView) iv.findViewById(R.id.history_entry_number)).setText(getString(R.string.detail_hist_call_number, toDisplay.getNumber()));
        iv.findViewById(R.id.history_call_name).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onCall(toDisplay.getAccountID(), toDisplay.getNumber());
            }
        });
        return inflatedView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
                entryView.formatted_hour = (TextView) convertView.findViewById(R.id.history_call_hour);
                entryView.record = (Button) convertView.findViewById(R.id.history_call_record);
                entryView.duration = (TextView) convertView.findViewById(R.id.history_call_duration);

                convertView.setTag(entryView);
            } else {
                entryView = (HistoryCallView) convertView.getTag();
            }

            final HistoryCall item = dataset.get(position);

            entryView.historyCallState.setText(item.getDirection());
            entryView.formatted_date.setText(item.getDate());
            entryView.duration.setText(item.getDurationString());
            entryView.formatted_hour.setText(item.getStartString("h:mm a"));
            if (item.isIncoming() && item.isMissed())
                convertView.setBackgroundColor(getResources().getColor(R.color.holo_red_light));

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

        /**
         * ******************
         * ViewHolder Pattern
         * *******************
         */
        public class HistoryCallView {
            protected TextView historyCallState;
            protected TextView formatted_date;
            protected TextView formatted_hour;
            protected Button record;
            protected TextView duration;
        }

    }

}
