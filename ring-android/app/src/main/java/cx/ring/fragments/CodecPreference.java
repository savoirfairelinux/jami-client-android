/*
 *  Copyright (C) 2015-2017 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.fragments;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.Codec;

class CodecPreference extends Preference {
    private static final String TAG = CodecPreference.class.getSimpleName();

    private CodecAdapter listAdapter;

    public CodecPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodecPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.frag_audio_mgmt);
        listAdapter = new CodecAdapter(context);
        Log.w(TAG, "CodecPreference create");
    }

    private void setListViewHeight(ListView listView, LinearLayout llMain) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int firstHeight;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            firstHeight = listItem.getMeasuredHeight();
            totalHeight += firstHeight;
        }

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) llMain.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount()));
        llMain.setLayoutParams(params);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ListView mCodecList = (ListView) holder.findViewById(R.id.dndlistview);
        if (mCodecList.getAdapter() != listAdapter)
            mCodecList.setAdapter(listAdapter);
        mCodecList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                listAdapter.getItem(pos).toggleState();
                listAdapter.notifyDataSetChanged();
                callChangeListener(getActiveCodecList());
            }
        });

        setListViewHeight(mCodecList, (LinearLayout) mCodecList.getParent());
    }

    ArrayList<Long> getActiveCodecList() {
        ArrayList<Long> results = new ArrayList<>();
        for (int i = 0; i < listAdapter.getCount(); ++i) {
            if (listAdapter.getItem(i).isEnabled()) {
                results.add(listAdapter.getItem(i).getPayload());
            }
        }
        return results;
    }

    void setCodecs(ArrayList<Codec> codecs) {
        listAdapter.setDataset(codecs);
    }

    void refresh() {
        if (null != this.listAdapter) {
            this.listAdapter.notifyDataSetChanged();
        }
    }

    private static class CodecAdapter extends BaseAdapter {

        ArrayList<Codec> items;
        private Context mContext;

        CodecAdapter(Context context) {
            items = new ArrayList<>();
            mContext = context;
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
            CodecView entryView;

            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.item_codec, parent, false);

                entryView = new CodecView();
                entryView.name = (TextView) rowView.findViewById(R.id.codec_name);
                entryView.samplerate = (TextView) rowView.findViewById(R.id.codec_samplerate);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.codec_checked);
                rowView.setTag(entryView);
            } else {
                entryView = (CodecView) rowView.getTag();
            }

            Codec codec = items.get(pos);

            if (codec.isSpeex())
                entryView.samplerate.setVisibility(View.VISIBLE);
            else
                entryView.samplerate.setVisibility(View.GONE);

            entryView.name.setText(codec.getName());
            entryView.samplerate.setText(codec.getSampleRate());
            entryView.enabled.setChecked(codec.isEnabled());

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

        void setDataset(ArrayList<Codec> codecs) {
            items = new ArrayList<>(codecs.size());
            for (Codec c : codecs)
                items.add(c);
        }

        class CodecView {
            public TextView name;
            public TextView samplerate;
            public CheckBox enabled;
        }
    }
}
