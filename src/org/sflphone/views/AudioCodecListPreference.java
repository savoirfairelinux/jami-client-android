package org.sflphone.views;

import java.util.ArrayList;

import org.sflphone.R;
import org.sflphone.model.Codec;
import org.sflphone.views.dragsortlv.DragSortListView;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class AudioCodecListPreference extends DialogPreference {

    CodecAdapter listAdapter;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Codec item = listAdapter.getItem(from);
                listAdapter.remove(item);
                listAdapter.insert(item, to);

            }
        }

    };

    private ArrayList<Codec> originalStates;

    public AudioCodecListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        listAdapter = new CodecAdapter(getContext());

    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                callChangeListener(listAdapter.getDataset());
                setList(listAdapter.getDataset());
            }
        });

        builder.setNegativeButton(android.R.string.no, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setList(originalStates);
            }
        });
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected View onCreateDialogView() {
        // LinearLayout layout = new LinearLayout(getContext());
        // layout.setOrientation(LinearLayout.VERTICAL);
        // layout.setPadding(6, 6, 6, 6);

        DragSortListView v = (DragSortListView) LayoutInflater.from(getContext()).inflate(R.layout.dialog_codecs_list, null);

        v.setDropListener(onDrop);
        v.setAdapter(listAdapter);
        v.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                listAdapter.getItem(pos).toggleState();
                listAdapter.notifyDataSetChanged();

            }
        });
        // layout.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return v;
    }

    private static class CodecAdapter extends BaseAdapter {

        ArrayList<Codec> items;
        private Context mContext;

        public CodecAdapter(Context context) {

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

    public void setList(ArrayList<Codec> codecs) {
        originalStates = new ArrayList<Codec>(codecs.size());
        for (Codec c : codecs) {
            originalStates.add(new Codec(c));
        }
        listAdapter.setDataset(codecs);
        listAdapter.notifyDataSetChanged();
    }

    public ArrayList<String> getActiveCodecList() {
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < listAdapter.getCount(); ++i) {
            if (listAdapter.getItem(i).isEnabled()) {
                results.add(listAdapter.getItem(i).getPayload().toString());
            }
        }
        return results;
    }

}
