package com.savoirfairelinux.sflphone.adapters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.ServiceConstants;

public class HistoryAdapter extends BaseAdapter {

    Context mContext;
    ArrayList<HashMap<String, String>> dataset;
    private static final String TAG = HistoryAdapter.class.getSimpleName();

    public HistoryAdapter(Activity activity, ArrayList<HashMap<String, String>> entries) {
        mContext = activity;
        dataset = entries;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup arg2) {
        View rowView = convertView;
        HistoryView entryView = null;

        if (rowView == null) {
            // Get a new instance of the row layout view
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_history, null);

            // Hold the view objects in an object
            // so they don't need to be re-fetched
            entryView = new HistoryView();
            entryView.displayName = (TextView) rowView.findViewById(R.id.display_name);
            entryView.duration = (TextView) rowView.findViewById(R.id.duration);
            entryView.date = (TextView) rowView.findViewById(R.id.date_start);

            rowView.setTag(entryView);
        } else {
            entryView = (HistoryView) rowView.getTag();
        }

        // Transfer the stock data from the data object
        // to the view objects
        
//        SipCall call = (SipCall) mCallList.values().toArray()[position];
        entryView.displayName.setText(dataset.get(pos).get(ServiceConstants.HISTORY_ACCOUNT_ID_KEY));

        
        long timestampEnd = Long.parseLong(dataset.get(pos).get(ServiceConstants.HISTORY_TIMESTAMP_STOP_KEY));
        long timestampStart = Long.parseLong(dataset.get(pos).get(ServiceConstants.HISTORY_TIMESTAMP_START_KEY));
        entryView.date.setText(getDate(timestampStart,"yyyy-MM-dd"));
        
        long duration = timestampEnd - timestampStart;
        entryView.duration.setText("Duration: "+duration);

        return rowView;

    }

    private String getDate(long timeStamp, String format) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat objFormatter = new SimpleDateFormat(format, Locale.CANADA);
        objFormatter.setTimeZone(tz);

        Calendar objCalendar = Calendar.getInstance(tz);
        objCalendar.setTimeInMillis(timeStamp*1000);
        String result = objFormatter.format(objCalendar.getTime());
        objCalendar.clear();
        return result;
    }

    /*********************
     * ViewHolder Pattern
     *********************/
    public class HistoryView {
        protected TextView displayName;
        protected TextView date;
        public TextView duration;
    }

    @Override
    public int getCount() {

        return dataset.size();
    }

    @Override
    public HashMap<String, String> getItem(int pos) {
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
        dataset.addAll(history);

    }

}
