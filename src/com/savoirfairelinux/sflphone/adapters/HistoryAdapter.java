package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.HistoryFragment;
import com.savoirfairelinux.sflphone.model.HistoryEntry;

public class HistoryAdapter extends BaseAdapter {

    HistoryFragment mContext;
    ArrayList<HistoryEntry> dataset;
    private static final String TAG = HistoryAdapter.class.getSimpleName();
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
            entryView.call_button = (ImageButton) rowView.findViewById(R.id.action_call);
            entryView.call_button.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                   mContext.makeNewCall(pos);
                    
                }
            } );
            rowView.setTag(entryView);
        } else {
            entryView = (HistoryView) rowView.getTag();
        }

        // Transfer the stock data from the data object
        // to the view objects
        
//        SipCall call = (SipCall) mCallList.values().toArray()[position];
        entryView.displayName.setText(dataset.get(pos).getContact().getmDisplayName());
        
        infos_fetcher.execute(new ContactPictureLoader(mContext.getActivity(), entryView.photo, dataset.get(pos).getContact().getId()));

        

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
//        dataset.addAll(history);

    }

}
