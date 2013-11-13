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
import org.sflphone.model.Codec;
import org.sflphone.service.ISipService;
import org.sflphone.views.dragsortlv.DragSortListView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AudioManagementFragment extends PreferenceFragment {
    static final String TAG = AudioManagementFragment.class.getSimpleName();

    protected Callbacks mCallbacks = sDummyCallbacks;
    ArrayList<Codec> codecs;
    private DragSortListView v;
    CodecAdapter listAdapter;
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }

        @Override
        public String getAccountID() {
            return null;
        }

    };

    public interface Callbacks {

        public ISipService getService();

        public String getAccountID();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        try {
            codecs = (ArrayList<Codec>) mCallbacks.getService().getAudioCodecList(mCallbacks.getAccountID());
            mCallbacks.getService().getRingtoneList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Codec item = listAdapter.getItem(from);
                listAdapter.remove(item);
                listAdapter.insert(item, to);
                try {
                    mCallbacks.getService().setActiveCodecList(getActiveCodecList(), mCallbacks.getAccountID());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public ArrayList<String> getActiveCodecList() {
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < listAdapter.getCount(); ++i) {
            if (listAdapter.getItem(i).isEnabled()) {
                results.add(listAdapter.getItem(i).getPayload().toString());
            }
        }
        return results;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.frag_audio_mgmt, null);
        v = (DragSortListView) rootView.findViewById(R.id.dndlistview);
        v.setAdapter(listAdapter);
        v.setDropListener(onDrop);
        v.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                listAdapter.getItem(pos).toggleState();
                listAdapter.notifyDataSetChanged();
                try {
                    mCallbacks.getService().setActiveCodecList(getActiveCodecList(), mCallbacks.getAccountID());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LinearLayout holder = (LinearLayout) getView().findViewById(R.id.lv_holder);
        holder.post(new Runnable() {

            @Override
            public void run() {
                setListViewHeight(v, holder);
            }
        });

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
        getView().requestLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.account_audio_prefs);
        listAdapter = new CodecAdapter(getActivity());
        listAdapter.setDataset(codecs);
    }

    Preference.OnPreferenceChangeListener changePreferenceListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((CharSequence) newValue);
            return true;
        }
    };

    public static class CodecAdapter extends BaseAdapter {

        ArrayList<Codec> items;
        private Context mContext;

        public CodecAdapter(Context context) {
            items = new ArrayList<Codec>();
            mContext = context;
        }

        public void insert(Codec item, int to) {
            items.add(to, item);
            notifyDataSetChanged();
        }

        public void remove(Codec item) {
            items.remove(item);
            notifyDataSetChanged();
        }

        public ArrayList<Codec> getDataset() {
            return items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Codec getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View rowView = convertView;
            CodecView entryView = null;

            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.item_codec, null);

                entryView = new CodecView();
                entryView.name = (TextView) rowView.findViewById(R.id.codec_name);
                entryView.bitrate = (TextView) rowView.findViewById(R.id.codec_bitrate);
                entryView.samplerate = (TextView) rowView.findViewById(R.id.codec_samplerate);
                entryView.channels = (TextView) rowView.findViewById(R.id.codec_channels);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.codec_checked);
                rowView.setTag(entryView);
            } else {
                entryView = (CodecView) rowView.getTag();
            }

            entryView.name.setText(items.get(pos).getName());
            entryView.samplerate.setText(items.get(pos).getSampleRate());
            entryView.bitrate.setText(items.get(pos).getBitRate());
            entryView.channels.setText(items.get(pos).getChannels());
            entryView.enabled.setChecked(items.get(pos).isEnabled());
            ;
            return rowView;

        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        public void setDataset(ArrayList<Codec> codecs) {
            items = new ArrayList<Codec>(codecs);
        }

        /*********************
         * ViewHolder Pattern
         *********************/
        public class CodecView {
            public TextView name;
            public TextView samplerate;
            public TextView bitrate;
            public TextView channels;
            public CheckBox enabled;
        }
    }
}
