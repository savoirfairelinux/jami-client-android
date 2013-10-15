package org.sflphone.views;

import java.util.ArrayList;

import org.sflphone.R;
import org.sflphone.model.Codec;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class AudioCodecListPreference extends DialogPreference {

    CodecAdapter listAdapter;

    public AudioCodecListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        listAdapter = new CodecAdapter(getContext());

    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {

        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    private static class CodecAdapter extends BaseAdapter {

        ArrayList<Codec> items;
        private Context mContext;

        public CodecAdapter(Context context) {
            
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
            
//            entryView.enabled.setChecked(items.get(pos).isEnabled());

//            entryView.enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    items.get(pos).setEnabled(isChecked);
//
//                    try {
//                        mCallbacks.getService().setAccountDetails(accounts.get(pos).getAccountID(), accounts.get(pos).getDetails());
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
            
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
        listAdapter.setDataset(codecs);
        listAdapter.notifyDataSetChanged();
    }

}
